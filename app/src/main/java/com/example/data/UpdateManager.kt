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
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateStatus { IDLE, CHECKING, AVAILABLE, UP_TO_DATE, DOWNLOADING, READY_TO_INSTALL, ERROR }

data class LauncherUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String = "",
    val mandatory: Boolean = false
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
        _state.value = LauncherUpdateState(UpdateStatus.CHECKING, message = "جارٍ فحص Google Drive...")
        try {
            val raw = readUrl(BuildConfig.UPDATE_MANIFEST_URL)
            val json = JSONObject(raw)
            val info = LauncherUpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.optString("versionName", json.getInt("versionCode").toString()),
                apkUrl = json.getString("apkUrl"),
                notes = json.optString("notes", ""),
                mandatory = json.optBoolean("mandatory", false)
            )
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _state.value = LauncherUpdateState(UpdateStatus.AVAILABLE, info = info, message = "يتوفر إصدار ${info.versionName}")
                info
            } else {
                _state.value = LauncherUpdateState(UpdateStatus.UP_TO_DATE, info = info, message = "لديك أحدث إصدار")
                null
            }
        } catch (e: Exception) {
            _state.value = LauncherUpdateState(UpdateStatus.ERROR, message = "تعذر فحص التحديث: ${e.localizedMessage ?: "خطأ اتصال"}")
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
        try {
            val conn = openConnection(target.apkUrl)
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            val temp = File(updateDir, "Launcher-2026-update.tmp")
            conn.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastProgress = -1
                    while (input.read(buffer).also { read = it } >= 0) {
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        done += read
                        val progress = if (total > 0) ((done * 100L) / total).toInt().coerceIn(0, 100) else 0
                        if (progress != lastProgress) {
                            lastProgress = progress
                            _state.value = LauncherUpdateState(UpdateStatus.DOWNLOADING, target, progress, "تنزيل التحديث $progress%")
                        }
                    }
                }
            }
            conn.disconnect()
            if (temp.length() < 1_000_000L) throw IllegalStateException("الملف المحمل غير صالح")
            if (apkFile.exists()) apkFile.delete()
            if (!temp.renameTo(apkFile)) {
                temp.copyTo(apkFile, overwrite = true)
                temp.delete()
            }
            _state.value = LauncherUpdateState(UpdateStatus.READY_TO_INSTALL, target, 100, "التحديث جاهز للتثبيت")
            true
        } catch (e: Exception) {
            _state.value = LauncherUpdateState(UpdateStatus.ERROR, target, message = "تعذر تنزيل التحديث: ${e.localizedMessage ?: "خطأ"}")
            false
        }
    }

    fun installDownloadedUpdate(): Boolean {
        if (!apkFile.exists() || apkFile.length() < 1_000_000L) {
            _state.value = _state.value.copy(status = UpdateStatus.ERROR, message = "لا يوجد APK تحديث جاهز")
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                _state.value = _state.value.copy(message = "فعّل السماح بالتثبيت ثم اضغط تثبيت التحديث")
                false
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(status = UpdateStatus.ERROR, message = "تعذر فتح مثبت أندرويد: ${e.localizedMessage ?: "خطأ"}")
            false
        }
    }

    fun resetMessage() {
        if (_state.value.status == UpdateStatus.ERROR) _state.value = LauncherUpdateState()
    }

    private fun readUrl(url: String): String {
        val conn = openConnection(url)
        return try { conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } } finally { conn.disconnect() }
    }

    private fun openConnection(rawUrl: String): HttpURLConnection {
        var current = rawUrl
        repeat(6) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Launcher-2026/${BuildConfig.VERSION_NAME}")
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
}
