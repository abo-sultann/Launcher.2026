package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

enum class UpdateStatus { IDLE, CHECKING, AVAILABLE, UP_TO_DATE, DOWNLOADING, READY_TO_INSTALL, ERROR }

data class LauncherUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String = "",
    val mandatory: Boolean = false,
    val sha256: String = ""
)

data class LauncherUpdateState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val info: LauncherUpdateInfo? = null,
    val progressPercent: Int = 0,
    val message: String = ""
)

class UpdateManager(private val context: Context) {
    private val _state = MutableStateFlow(LauncherUpdateState())
    val state: StateFlow<LauncherUpdateState> = _state.asStateFlow()

    private val updateDir = File(context.filesDir, "updates").apply { mkdirs() }
    private val apkFile = File(updateDir, "Launcher-2026-update.apk")

    suspend fun checkForUpdate(): LauncherUpdateInfo? = withContext(Dispatchers.IO) {
        _state.value = LauncherUpdateState(UpdateStatus.CHECKING, message = "جارٍ فحص التحديث...")
        try {
            val raw = readTextUrl(BuildConfig.UPDATE_MANIFEST_URL)
            val json = JSONObject(raw)
            val info = LauncherUpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.optString("versionName", json.getInt("versionCode").toString()),
                apkUrl = json.getString("apkUrl"),
                notes = json.optString("notes", ""),
                mandatory = json.optBoolean("mandatory", false),
                sha256 = json.optString("sha256", "").trim().lowercase(Locale.US)
            )
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _state.value = LauncherUpdateState(UpdateStatus.AVAILABLE, info = info, message = "يتوفر إصدار ${info.versionName}")
                info
            } else {
                _state.value = LauncherUpdateState(UpdateStatus.UP_TO_DATE, info = info, message = "لديك أحدث إصدار")
                null
            }
        } catch (e: Exception) {
            _state.value = LauncherUpdateState(UpdateStatus.ERROR, message = friendlyError("تعذر فحص التحديث", e))
            null
        }
    }

    suspend fun checkAndAutoDownload() {
        val info = checkForUpdate() ?: return
        download(info)
    }

    suspend fun download(info: LauncherUpdateInfo? = _state.value.info): Boolean = withContext(Dispatchers.IO) {
        val target = info ?: return@withContext false
        _state.value = LauncherUpdateState(UpdateStatus.DOWNLOADING, info = target, progressPercent = 0, message = "جارٍ تنزيل التحديث...")
        val temp = File(updateDir, "Launcher-2026-update.tmp")
        temp.delete()
        try {
            var lastError: Exception? = null
            var success = false
            for (attempt in 1..2) {
                try {
                    downloadApk(target.apkUrl, temp, target, attempt)
                    validateApk(temp, target)
                    success = true
                    break
                } catch (e: Exception) {
                    lastError = e
                    temp.delete()
                    if (attempt < 2) {
                        _state.value = LauncherUpdateState(UpdateStatus.DOWNLOADING, target, 0, "إعادة محاولة التنزيل...")
                    }
                }
            }
            if (!success) throw lastError ?: IllegalStateException("تعذر تنزيل ملف التحديث")

            if (apkFile.exists()) apkFile.delete()
            if (!temp.renameTo(apkFile)) {
                temp.copyTo(apkFile, overwrite = true)
                temp.delete()
            }
            _state.value = LauncherUpdateState(UpdateStatus.READY_TO_INSTALL, target, 100, "التحديث جاهز للتثبيت")
            true
        } catch (e: Exception) {
            temp.delete()
            _state.value = LauncherUpdateState(UpdateStatus.ERROR, target, message = friendlyError("تعذر تنزيل التحديث", e))
            false
        }
    }

    fun installDownloadedUpdate(): Boolean {
        if (!apkFile.exists() || apkFile.length() < MIN_APK_BYTES) {
            _state.value = _state.value.copy(status = UpdateStatus.ERROR, message = "لا يوجد تحديث جاهز للتثبيت")
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                _state.value = _state.value.copy(message = "فعّل السماح بالتثبيت ثم أعد الضغط على تثبيت")
                false
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, APK_MIME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(status = UpdateStatus.ERROR, message = friendlyError("تعذر فتح مثبت أندرويد", e))
            false
        }
    }

    fun resetMessage() {
        if (_state.value.status == UpdateStatus.ERROR) _state.value = LauncherUpdateState()
    }

    private fun downloadApk(rawUrl: String, targetFile: File, info: LauncherUpdateInfo, attempt: Int) {
        val candidates = buildDownloadCandidates(rawUrl, attempt)
        var last: Exception? = null
        for (candidate in candidates) {
            try {
                val conn = openConnection(candidate)
                val contentType = (conn.contentType ?: "").lowercase(Locale.US)
                val total = conn.contentLengthLong.coerceAtLeast(0L)
                if (contentType.contains("text/html")) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(350_000) }
                    conn.disconnect()
                    val confirmed = extractDriveConfirmedUrl(body, candidate)
                    if (confirmed != null) {
                        downloadRaw(confirmed, targetFile, info)
                        return
                    }
                    throw IllegalStateException("Google Drive أعاد صفحة بدل ملف APK")
                }
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        copyWithProgress(input, output, total, info)
                    }
                }
                conn.disconnect()
                return
            } catch (e: Exception) {
                last = e
                targetFile.delete()
            }
        }
        throw last ?: IllegalStateException("فشل رابط التنزيل")
    }

    private fun downloadRaw(url: String, targetFile: File, info: LauncherUpdateInfo) {
        val conn = openConnection(url)
        val type = (conn.contentType ?: "").lowercase(Locale.US)
        if (type.contains("text/html")) {
            conn.disconnect()
            throw IllegalStateException("رابط Drive لم يعط ملف APK")
        }
        val total = conn.contentLengthLong.coerceAtLeast(0L)
        conn.inputStream.use { input -> FileOutputStream(targetFile).use { output -> copyWithProgress(input, output, total, info) } }
        conn.disconnect()
    }

    private fun copyWithProgress(input: java.io.InputStream, output: FileOutputStream, total: Long, info: LauncherUpdateInfo) {
        val buffer = ByteArray(64 * 1024)
        var done = 0L
        var lastProgress = -1
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            done += read
            val progress = if (total > 0) ((done * 100L) / total).toInt().coerceIn(0, 100) else 0
            if (progress != lastProgress) {
                lastProgress = progress
                _state.value = LauncherUpdateState(UpdateStatus.DOWNLOADING, info, progress, if (total > 0) "تنزيل التحديث $progress%" else "جارٍ تنزيل التحديث...")
            }
        }
        output.fd.sync()
    }

    private fun validateApk(file: File, info: LauncherUpdateInfo) {
        if (!file.exists() || file.length() < MIN_APK_BYTES) throw IllegalStateException("الملف الذي وصل من Drive غير مكتمل")
        FileInputStream(file).use { input ->
            val first = ByteArray(4)
            if (input.read(first) != 4 || first[0] != 0x50.toByte() || first[1] != 0x4B.toByte()) {
                throw IllegalStateException("الملف المحمل ليس APK صالحًا")
            }
        }
        if (info.sha256.isNotBlank()) {
            val actual = sha256(file)
            if (!actual.equals(info.sha256, ignoreCase = true)) throw IllegalStateException("فشل التحقق من سلامة ملف التحديث")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readTextUrl(url: String): String {
        val conn = openConnection(url)
        val type = (conn.contentType ?: "").lowercase(Locale.US)
        return try {
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (type.contains("text/html") && !raw.trimStart().startsWith("{")) {
                val confirmed = extractDriveConfirmedUrl(raw, url)
                if (confirmed != null) return readTextUrl(confirmed)
            }
            raw
        } finally { conn.disconnect() }
    }

    private fun buildDownloadCandidates(rawUrl: String, attempt: Int): List<String> {
        val id = extractDriveFileId(rawUrl) ?: return listOf(rawUrl)
        val primary = "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
        val classic = "https://drive.google.com/uc?export=download&id=$id&confirm=t"
        return if (attempt == 1) listOf(primary, classic, rawUrl) else listOf(classic, primary, rawUrl)
    }

    private fun extractDriveFileId(url: String): String? {
        Regex("[?&]id=([A-Za-z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("/d/([A-Za-z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    private fun extractDriveConfirmedUrl(html: String, sourceUrl: String): String? {
        val id = extractDriveFileId(sourceUrl)
        val token = Regex("confirm=([0-9A-Za-z_-]+)").find(html)?.groupValues?.getOrNull(1)
            ?: Regex("name=\"confirm\" value=\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)
        if (id != null && token != null) return "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=$token"
        return null
    }

    private fun openConnection(rawUrl: String): HttpURLConnection {
        var current = rawUrl
        repeat(8) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 45_000
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("User-Agent", "Mozilla/5.0 Launcher-2026/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.android.package-archive,application/octet-stream,*/*")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location") ?: throw IllegalStateException("تحويل رابط غير صالح")
                conn.disconnect()
                current = URL(URL(current), next).toString()
            } else {
                if (code !in 200..299) {
                    conn.disconnect()
                    throw IllegalStateException("HTTP $code")
                }
                return conn
            }
        }
        throw IllegalStateException("تحويلات كثيرة في رابط التحديث")
    }

    private fun friendlyError(prefix: String, e: Exception): String {
        val detail = e.message?.takeIf { it.isNotBlank() } ?: "خطأ اتصال"
        return "$prefix: $detail"
    }

    companion object {
        private const val MIN_APK_BYTES = 1_000_000L
        private const val APK_MIME = "application/vnd.android.package-archive"
    }
}
