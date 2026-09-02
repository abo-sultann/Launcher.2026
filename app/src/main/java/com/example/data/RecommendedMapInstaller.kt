package com.example.data

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

enum class RecommendedMapStatus { IDLE, DOWNLOADING, INSTALLED, ERROR }

data class RecommendedMapDownloadState(
    val status: RecommendedMapStatus = RecommendedMapStatus.IDLE,
    val progressPercent: Int = 0,
    val message: String = ""
)

/**
 * Downloads the official GCC Mapsforge map as verified Drive parts, then installs it atomically.
 *
 * Parts stay below Drive's large-file transport limit. The driver sees one progress value and one
 * installed map; part files never appear in the map manager and are deleted after each check.
 */
class RecommendedMapInstaller(
    private val context: Context,
    private val mapEngine: OfflineMapEngine
) {
    private val _state = MutableStateFlow(RecommendedMapDownloadState())
    val state: StateFlow<RecommendedMapDownloadState> = _state.asStateFlow()

    private val downloadMutex = Mutex()

    suspend fun downloadAndInstall(): Boolean = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val mapsDir = File(context.filesDir, "maps").apply { mkdirs() }
            val requiredBytes = TOTAL_BYTES + LARGEST_PART_BYTES + FREE_SPACE_MARGIN_BYTES
            if (StatFs(mapsDir.absolutePath).availableBytes < requiredBytes) {
                _state.value = RecommendedMapDownloadState(
                    RecommendedMapStatus.ERROR,
                    message = "المساحة غير كافية؛ يلزم نحو 460 ميجابايت مؤقتًا"
                )
                return@withLock false
            }

            val assembled = File(mapsDir, ".Launcher-2026-GCC.map.download")
            val partFile = File(mapsDir, ".Launcher-2026-GCC.part.download")
            assembled.delete()
            partFile.delete()

            try {
                _state.value = RecommendedMapDownloadState(
                    RecommendedMapStatus.DOWNLOADING,
                    message = "بدء تنزيل خريطة الخليج..."
                )

                FileOutputStream(assembled).use { assembledOutput ->
                    var completedBytes = 0L
                    PARTS.forEachIndexed { index, part ->
                        downloadVerifiedPart(part, partFile, completedBytes, index + 1)
                        FileInputStream(partFile).use { input ->
                            input.copyTo(assembledOutput, DOWNLOAD_BUFFER_BYTES)
                        }
                        completedBytes += part.sizeBytes
                        partFile.delete()
                    }
                    assembledOutput.fd.sync()
                }

                if (assembled.length() != TOTAL_BYTES || !sha256(assembled).equals(FULL_SHA256, ignoreCase = true)) {
                    throw IllegalStateException("فشل التحقق النهائي من ملف الخريطة")
                }

                val installed = uniqueDestination(mapsDir, FINAL_FILE_NAME)
                if (!assembled.renameTo(installed)) {
                    FileInputStream(assembled).use { input ->
                        FileOutputStream(installed).use { output ->
                            input.copyTo(output, DOWNLOAD_BUFFER_BYTES)
                            output.fd.sync()
                        }
                    }
                    assembled.delete()
                }

                if (!mapEngine.importMapFile(installed, DISPLAY_NAME)) {
                    installed.delete()
                    throw IllegalStateException(mapEngine.mapError.value ?: "ملف الخريطة غير صالح")
                }

                _state.value = RecommendedMapDownloadState(
                    RecommendedMapStatus.INSTALLED,
                    100,
                    "تم تنزيل خريطة الخليج وتفعيلها"
                )
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Recommended map download failed", t)
                assembled.delete()
                partFile.delete()
                _state.value = RecommendedMapDownloadState(
                    RecommendedMapStatus.ERROR,
                    message = friendlyError(t)
                )
                false
            }
        }
    }

    fun clearMessage() {
        if (_state.value.status != RecommendedMapStatus.DOWNLOADING) {
            _state.value = RecommendedMapDownloadState()
        }
    }

    private fun downloadVerifiedPart(part: MapPart, target: File, completedBytes: Long, partNumber: Int) {
        var lastError: Throwable? = null
        val candidates = listOf(
            "https://drive.usercontent.google.com/download?id=${part.driveId}&export=download&confirm=t",
            "https://drive.google.com/uc?export=download&id=${part.driveId}&confirm=t"
        )
        candidates.forEach { candidate ->
            try {
                target.delete()
                downloadCandidate(candidate, target, completedBytes, partNumber)
                if (target.length() != part.sizeBytes) throw IllegalStateException("الجزء $partNumber غير مكتمل")
                if (!sha256(target).equals(part.sha256, ignoreCase = true)) {
                    throw IllegalStateException("فشل التحقق من الجزء $partNumber")
                }
                return
            } catch (t: Throwable) {
                lastError = t
                target.delete()
            }
        }
        throw lastError ?: IllegalStateException("تعذر تنزيل الجزء $partNumber")
    }

    private fun downloadCandidate(rawUrl: String, target: File, completedBytes: Long, partNumber: Int, confirmationDepth: Int = 0) {
        if (confirmationDepth > MAX_CONFIRMATION_DEPTH) throw IllegalStateException("تكررت صفحة تأكيد Drive")
        val connection = openConnection(rawUrl)
        try {
            val contentType = (connection.contentType ?: "").lowercase(Locale.US)
            if (contentType.contains("text/html")) {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    it.readText().take(MAX_CONFIRMATION_HTML_CHARS)
                }
                val confirmed = extractDriveConfirmedUrl(body, rawUrl)
                    ?: throw IllegalStateException("Drive أعاد صفحة بدل الجزء $partNumber")
                connection.disconnect()
                downloadCandidate(confirmed, target, completedBytes, partNumber, confirmationDepth + 1)
                return
            }

            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = (((completedBytes + downloaded) * 100L) / TOTAL_BYTES)
                            .toInt().coerceIn(0, 99)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            _state.value = RecommendedMapDownloadState(
                                RecommendedMapStatus.DOWNLOADING,
                                progress,
                                "تنزيل خريطة الخليج $progress% • الجزء $partNumber من ${PARTS.size}"
                            )
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(rawUrl: String): HttpURLConnection {
        var current = rawUrl
        repeat(MAX_REDIRECTS) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("User-Agent", "Mozilla/5.0 Launcher-2026/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/octet-stream,*/*")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("تحويل رابط الخريطة غير صالح")
                connection.disconnect()
                current = URL(URL(current), location).toString()
            } else {
                if (code !in 200..299) {
                    connection.disconnect()
                    throw IllegalStateException("تعذر تنزيل الخريطة (HTTP $code)")
                }
                return connection
            }
        }
        throw IllegalStateException("تحويلات كثيرة في رابط الخريطة")
    }

    private fun extractDriveConfirmedUrl(html: String, sourceUrl: String): String? {
        val id = Regex("[?&]id=([A-Za-z0-9_-]+)").find(sourceUrl)?.groupValues?.getOrNull(1)
            ?: return null
        val token = Regex("confirm=([0-9A-Za-z_-]+)").find(html)?.groupValues?.getOrNull(1)
            ?: Regex("name=\"confirm\" value=\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)
            ?: return null
        return "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=$token"
    }

    private fun uniqueDestination(directory: File, fileName: String): File {
        val direct = File(directory, fileName)
        if (!direct.exists()) return direct
        val base = fileName.removeSuffix(".map")
        for (index in 2..999) {
            val candidate = File(directory, "$base-$index.map")
            if (!candidate.exists()) return candidate
        }
        return File(directory, "$base-${System.currentTimeMillis()}.map")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun friendlyError(t: Throwable): String {
        val detail = t.message?.takeIf { it.isNotBlank() } ?: "تعذر تنزيل الخريطة"
        return "تعذر تنزيل خريطة الخليج: $detail"
    }

    private data class MapPart(
        val driveId: String,
        val sizeBytes: Long,
        val sha256: String
    )

    companion object {
        private const val TAG = "RecommendedMap"
        private const val DISPLAY_NAME = "خريطة الخليج 2026"
        private const val FINAL_FILE_NAME = "Launcher-2026-GCC.map"
        private const val TOTAL_BYTES = 322_177_740L
        private const val LARGEST_PART_BYTES = 90_000_000L
        private const val FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L
        private const val FULL_SHA256 = "635f330be605b8a77a7df6d9d4ddddf7935c8f800f98d046e323eccc96ea2ff4"
        private const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val MAX_REDIRECTS = 8
        private const val MAX_CONFIRMATION_HTML_CHARS = 350_000
        private const val MAX_CONFIRMATION_DEPTH = 2

        private val PARTS = listOf(
            MapPart("1CmnGz6CVgV4L8GURhKktz82e72mvWNNv", 90_000_000L, "990f43d2c3c33b71d6fb1917c3229a11ccb7b21a7d4e673635a2c803ccdcebca"),
            MapPart("1TCXNsrpM41dCa75iBMdefJXg5sQpGm2W", 90_000_000L, "b0feecacdd1666c9508dd26ef7ed868bbac8e1f1db18f2a6980323de9824ec56"),
            MapPart("1O0qNxtagcpxKp4nTVx2dG4KTGcy1lWWW", 90_000_000L, "6a3e9201369941b207d8149b8ceb08f6f090ba781ab3573fc5516bd58d507bb0"),
            MapPart("16FmaCqEI59NgbCardsfpkcJVuRyYO5Zq", 52_177_740L, "f4669ac0df4e0c4a8e7f6bb8981c75ec9580b8a428515f29cd16313f100288b2")
        )
    }
}
