package com.example.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

enum class DarbakAudioSyncStatus { IDLE, SCANNING, IMPORTING, DONE, ERROR }

data class DarbakAudioSyncState(
    val status: DarbakAudioSyncStatus = DarbakAudioSyncStatus.IDLE,
    val message: String = "",
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
)

/**
 * Internal Darb Al-Sout storage/import engine owned by Darbak Launcher.
 *
 * Network/account transport is deliberately outside this class. A Drive, Telegram or future
 * transport only downloads into [inboxDirectory]; this manager validates and commits audio into the
 * local music library. Transport failures therefore cannot affect Launcher startup or playback.
 */
class DarbakAudioSyncManager(context: Context) {
    private val appContext = context.applicationContext
    private val privateRoot = File(appContext.filesDir, "darbak_audio_sync").apply { mkdirs() }
    val inboxDirectory: File = File(privateRoot, "inbox").apply { mkdirs() }

    private val _state = MutableStateFlow(DarbakAudioSyncState())
    val state: StateFlow<DarbakAudioSyncState> = _state.asStateFlow()

    suspend fun importInbox(): DarbakAudioSyncState = withContext(Dispatchers.IO) {
        _state.value = DarbakAudioSyncState(DarbakAudioSyncStatus.SCANNING, "جارٍ فحص صوتيات درب الصوت...")
        var imported = 0
        var duplicates = 0
        return@withContext try {
            val candidates = inboxDirectory.listFiles()
                ?.filter { it.isFile && isSupportedAudioName(it.name) && !it.name.endsWith(TEMP_SUFFIX) }
                ?.sortedBy { it.name.lowercase() }
                .orEmpty()

            for ((index, file) in candidates.withIndex()) {
                _state.value = DarbakAudioSyncState(
                    status = DarbakAudioSyncStatus.IMPORTING,
                    message = "جارٍ إضافة ${index + 1}/${candidates.size}",
                    importedCount = imported,
                    duplicateCount = duplicates,
                )
                when (commitDownloadedFile(file, file.name).getOrThrow()) {
                    ImportOutcome.IMPORTED -> imported++
                    ImportOutcome.DUPLICATE -> duplicates++
                }
            }

            DarbakAudioSyncState(
                status = DarbakAudioSyncStatus.DONE,
                message = if (candidates.isEmpty()) "لا توجد صوتيات جديدة" else "تمت المزامنة",
                importedCount = imported,
                duplicateCount = duplicates,
            ).also { _state.value = it }
        } catch (t: Throwable) {
            Log.e(TAG, "Audio inbox import failed", t)
            DarbakAudioSyncState(
                status = DarbakAudioSyncStatus.ERROR,
                message = t.message ?: "تعذر مزامنة الصوتيات",
                importedCount = imported,
                duplicateCount = duplicates,
            ).also { _state.value = it }
        }
    }

    /**
     * Commits one fully downloaded audio file. Optional size/hash checks let a future transport
     * verify cloud metadata before the original remote object is deleted.
     */
    suspend fun commitDownloadedFile(
        sourceFile: File,
        originalName: String,
        expectedSizeBytes: Long = 0L,
        expectedMd5: String? = null,
    ): Result<ImportOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            require(sourceFile.exists() && sourceFile.isFile) { "ملف الصوت غير موجود" }
            require(sourceFile.length() > 0L) { "ملف الصوت فارغ" }
            require(isSupportedAudioName(originalName)) { "الملف ليس MP3 صالحًا لدرب الصوت" }
            if (expectedSizeBytes > 0L) {
                require(sourceFile.length() == expectedSizeBytes) { "حجم الملف لا يطابق المصدر" }
            }

            val sourceHash = md5(sourceFile)
            if (!expectedMd5.isNullOrBlank()) {
                require(sourceHash.equals(expectedMd5.trim(), ignoreCase = true)) { "بصمة الملف لا تطابق المصدر" }
            }

            val targetDir = targetMusicDirectory()
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw IllegalStateException("تعذر إنشاء مجلد درب الصوت")
            }

            val safeName = sanitizeFileName(originalName)
            val sameName = File(targetDir, safeName)
            if (sameName.exists() && sameName.isFile && md5(sameName) == sourceHash) {
                if (sourceFile.parentFile == inboxDirectory) sourceFile.delete()
                return@runCatching ImportOutcome.DUPLICATE
            }

            // Also stop renamed duplicates from accumulating when the cloud source sends the same
            // audio more than once under a slightly different file name.
            val duplicate = targetDir.listFiles()?.firstOrNull { existing ->
                existing.isFile && isSupportedAudioName(existing.name) &&
                    existing.length() == sourceFile.length() && runCatching { md5(existing) == sourceHash }.getOrDefault(false)
            }
            if (duplicate != null) {
                if (sourceFile.parentFile == inboxDirectory) sourceFile.delete()
                return@runCatching ImportOutcome.DUPLICATE
            }

            val finalTarget = uniqueTarget(targetDir, safeName)
            val temporaryTarget = File(targetDir, finalTarget.name + TEMP_SUFFIX)
            temporaryTarget.delete()

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(temporaryTarget).use { output ->
                    input.copyTo(output, COPY_BUFFER_BYTES)
                    output.fd.sync()
                }
            }
            require(temporaryTarget.length() == sourceFile.length()) { "لم يكتمل نسخ الملف الصوتي" }
            require(md5(temporaryTarget) == sourceHash) { "فشل التحقق من الملف بعد الحفظ" }

            if (finalTarget.exists() && !finalTarget.delete()) {
                throw IllegalStateException("تعذر استبدال ملف الصوت")
            }
            if (!temporaryTarget.renameTo(finalTarget)) {
                FileInputStream(temporaryTarget).use { input ->
                    FileOutputStream(finalTarget).use { output ->
                        input.copyTo(output, COPY_BUFFER_BYTES)
                        output.fd.sync()
                    }
                }
                temporaryTarget.delete()
            }
            require(finalTarget.exists() && finalTarget.length() == sourceFile.length()) { "تعذر تثبيت ملف الصوت" }

            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(finalTarget.absolutePath),
                arrayOf("audio/mpeg"),
                null,
            )

            if (sourceFile.parentFile == inboxDirectory) sourceFile.delete()
            ImportOutcome.IMPORTED
        }
    }

    fun resetMessage() {
        if (_state.value.status == DarbakAudioSyncStatus.DONE || _state.value.status == DarbakAudioSyncStatus.ERROR) {
            _state.value = DarbakAudioSyncState()
        }
    }

    @Suppress("DEPRECATION")
    private fun targetMusicDirectory(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), TARGET_FOLDER_NAME)

    private fun uniqueTarget(directory: File, requestedName: String): File {
        val direct = File(directory, requestedName)
        if (!direct.exists()) return direct
        val dot = requestedName.lastIndexOf('.')
        val stem = if (dot > 0) requestedName.substring(0, dot) else requestedName
        val ext = if (dot > 0) requestedName.substring(dot) else ".mp3"
        for (i in 2..999) {
            val candidate = File(directory, "$stem ($i)$ext")
            if (!candidate.exists()) return candidate
        }
        return File(directory, "${stem}_${System.currentTimeMillis()}$ext")
    }

    private fun sanitizeFileName(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
            .replace(Regex("[\\u0000-\\u001f<>:\"/\\\\|?*]"), "_")
            .take(120)
            .ifBlank { "darbak_audio_${System.currentTimeMillis()}.mp3" }
        return if (base.lowercase().endsWith(".mp3")) base else "$base.mp3"
    }

    private fun isSupportedAudioName(name: String): Boolean = name.trim().lowercase().endsWith(".mp3")

    private fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
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

    enum class ImportOutcome { IMPORTED, DUPLICATE }

    companion object {
        private const val TAG = "DarbakAudioSync"
        private const val TARGET_FOLDER_NAME = "درب الصوت"
        private const val TEMP_SUFFIX = ".part"
        private const val COPY_BUFFER_BYTES = 128 * 1024
    }
}
