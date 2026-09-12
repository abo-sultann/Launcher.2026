package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
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
import java.net.URLEncoder

/**
 * Manual Telegram transport for Darbak Audio.
 *
 * This class has no Google Play Services, Retrofit, Room, background service or boot receiver. It
 * only runs when the user presses Sync. Downloaded bytes are handed to [DarbakAudioSyncManager],
 * which owns validation, duplicate protection and final library storage.
 */
class DarbakAudioTelegramTransport(
    context: Context,
    private val importer: DarbakAudioSyncManager,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(DarbakAudioRemoteState())
    val state: StateFlow<DarbakAudioRemoteState> = _state.asStateFlow()

    fun configuration(): DarbakAudioTelegramConfig = DarbakAudioTelegramConfig(
        botToken = prefs.getString(KEY_BOT_TOKEN, "").orEmpty(),
        chatId = prefs.getString(KEY_CHAT_ID, "").orEmpty(),
    )

    fun saveConfiguration(botToken: String, chatId: String) {
        val token = botToken.trim()
        val chat = chatId.trim()
        val changed = token != prefs.getString(KEY_BOT_TOKEN, "").orEmpty() ||
            chat != prefs.getString(KEY_CHAT_ID, "").orEmpty()
        prefs.edit()
            .putString(KEY_BOT_TOKEN, token)
            .putString(KEY_CHAT_ID, chat)
            .apply()
        if (changed) prefs.edit().remove(KEY_LAST_OFFSET).apply()
    }

    fun isConfigured(): Boolean {
        val config = configuration()
        return config.botToken.isNotBlank() && config.chatId.isNotBlank()
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val config = configuration()
            require(config.botToken.isNotBlank()) { "أدخل توكن البوت" }
            require(isOnline()) { "لا يوجد اتصال بالإنترنت" }
            val root = requestJson(apiUrl(config.botToken, "getMe"))
            require(root.optBoolean("ok")) { root.optString("description", "فشل الاتصال بالبوت") }
            val user = root.optJSONObject("result")
            val name = user?.optString("first_name")?.takeIf { it.isNotBlank() }
                ?: user?.optString("username")?.takeIf { it.isNotBlank() }
                ?: "بوت درب الصوت"
            name
        }
    }

    suspend fun syncNow(): DarbakAudioRemoteState = withContext(Dispatchers.IO) {
        val config = configuration()
        if (config.botToken.isBlank() || config.chatId.isBlank()) {
            return@withContext finishError("أدخل توكن البوت و Chat ID أولاً")
        }
        if (!isOnline()) return@withContext finishError("لا يوجد اتصال بالإنترنت")

        _state.value = DarbakAudioRemoteState(
            status = DarbakAudioRemoteStatus.CHECKING,
            message = "جارٍ البحث عن صوتيات جديدة...",
        )

        var imported = 0
        var duplicates = 0
        var checked = 0
        return@withContext try {
            var offset = prefs.getLong(KEY_LAST_OFFSET, 0L)
            var page = 0
            var keepPaging = true

            while (keepPaging && page < MAX_PAGES) {
                val url = apiUrl(
                    config.botToken,
                    "getUpdates?offset=${offset.coerceAtLeast(0L)}&limit=$PAGE_SIZE&timeout=0",
                )
                val root = requestJson(url)
                require(root.optBoolean("ok")) { root.optString("description", "تعذر قراءة رسائل البوت") }
                val updates = root.optJSONArray("result") ?: break
                if (updates.length() == 0) break

                for (i in 0 until updates.length()) {
                    val update = updates.getJSONObject(i)
                    val updateId = update.optLong("update_id", -1L)
                    if (updateId < 0L) continue
                    val message = update.optJSONObject("message")
                        ?: update.optJSONObject("channel_post")
                        ?: update.optJSONObject("edited_message")

                    if (message == null) {
                        offset = updateId + 1L
                        saveOffset(offset)
                        continue
                    }

                    val chat = message.optJSONObject("chat")?.optLong("id")?.toString().orEmpty()
                    if (chat != config.chatId) {
                        offset = updateId + 1L
                        saveOffset(offset)
                        continue
                    }

                    checked++
                    val candidate = extractMp3Candidate(message)
                    if (candidate == null) {
                        offset = updateId + 1L
                        saveOffset(offset)
                        continue
                    }

                    _state.value = DarbakAudioRemoteState(
                        status = DarbakAudioRemoteStatus.DOWNLOADING,
                        message = "جارٍ تنزيل ${candidate.fileName}",
                        importedCount = imported,
                        duplicateCount = duplicates,
                        checkedCount = checked,
                    )

                    val fileMeta = requestJson(
                        apiUrl(
                            config.botToken,
                            "getFile?file_id=${encode(candidate.fileId)}",
                        )
                    )
                    require(fileMeta.optBoolean("ok")) {
                        fileMeta.optString("description", "تعذر الحصول على ملف تيليجرام")
                    }
                    val filePath = fileMeta.optJSONObject("result")?.optString("file_path").orEmpty()
                    require(filePath.isNotBlank()) { "تيليجرام لم يعط مسار الملف" }

                    val temporary = File(
                        importer.inboxDirectory,
                        "telegram_${updateId}_${System.currentTimeMillis()}.part",
                    )
                    temporary.delete()
                    try {
                        download(
                            url = "https://api.telegram.org/file/bot${config.botToken}/$filePath",
                            target = temporary,
                        )
                        val outcome = importer.commitDownloadedFile(
                            sourceFile = temporary,
                            originalName = candidate.fileName,
                            expectedSizeBytes = candidate.fileSize,
                        ).getOrThrow()
                        when (outcome) {
                            DarbakAudioSyncManager.ImportOutcome.IMPORTED -> imported++
                            DarbakAudioSyncManager.ImportOutcome.DUPLICATE -> duplicates++
                        }
                    } finally {
                        temporary.delete()
                    }

                    // Commit the Telegram offset only after the matching audio was safely handled.
                    offset = updateId + 1L
                    saveOffset(offset)
                }

                keepPaging = updates.length() >= PAGE_SIZE
                page++
            }

            DarbakAudioRemoteState(
                status = DarbakAudioRemoteStatus.DONE,
                message = when {
                    imported > 0 -> "تمت إضافة $imported صوتيات جديدة"
                    duplicates > 0 -> "لا جديد • تم تجاوز $duplicates مكرر"
                    else -> "لا توجد صوتيات جديدة"
                },
                importedCount = imported,
                duplicateCount = duplicates,
                checkedCount = checked,
            ).also { _state.value = it }
        } catch (t: Throwable) {
            Log.w(TAG, "Manual Darbak Audio sync failed", t)
            finishError(t.message ?: "تعذر مزامنة درب الصوت", imported, duplicates, checked)
        }
    }

    fun clearResult() {
        _state.value = DarbakAudioRemoteState()
    }

    private fun extractMp3Candidate(message: JSONObject): TelegramAudioCandidate? {
        val audio = message.optJSONObject("audio")
        if (audio != null) {
            val fileName = audio.optString("file_name")
                .takeIf { it.lowercase().endsWith(".mp3") }
                ?: audio.optString("title").takeIf { it.isNotBlank() }?.let { "$it.mp3" }
                ?: "darbak_audio_${message.optLong("message_id", System.currentTimeMillis())}.mp3"
            return TelegramAudioCandidate(
                fileId = audio.optString("file_id"),
                fileName = fileName,
                fileSize = audio.optLong("file_size", 0L).coerceAtLeast(0L),
            ).takeIf { it.fileId.isNotBlank() }
        }

        val document = message.optJSONObject("document") ?: return null
        val fileName = document.optString("file_name")
        val mime = document.optString("mime_type")
        if (!fileName.lowercase().endsWith(".mp3") && !mime.equals("audio/mpeg", ignoreCase = true)) {
            return null
        }
        return TelegramAudioCandidate(
            fileId = document.optString("file_id"),
            fileName = fileName.ifBlank { "darbak_audio_${message.optLong("message_id", System.currentTimeMillis())}.mp3" },
            fileSize = document.optLong("file_size", 0L).coerceAtLeast(0L),
        ).takeIf { it.fileId.isNotBlank() }
    }

    private fun requestJson(url: String): JSONObject {
        val conn = open(url)
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (text.isBlank()) throw IllegalStateException("استجابة فارغة من تيليجرام ($code)")
            val json = JSONObject(text)
            if (code !in 200..299) {
                throw IllegalStateException(json.optString("description", "خطأ تيليجرام $code"))
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, target: File) {
        val conn = open(url)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("فشل تنزيل الصوت ($code)")
            val length = conn.contentLengthLong
            if (length > MAX_AUDIO_BYTES) throw IllegalStateException("الملف أكبر من الحد المسموح")
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        total += read
                        if (total > MAX_AUDIO_BYTES) throw IllegalStateException("الملف أكبر من الحد المسموح")
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            if (target.length() <= 0L) throw IllegalStateException("تم تنزيل ملف صوت فارغ")
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        instanceFollowRedirects = true
        useCaches = false
        setRequestProperty("Accept", "application/json, audio/mpeg, application/octet-stream")
        setRequestProperty("User-Agent", "DarbakLauncher/2")
    }

    private fun apiUrl(token: String, methodAndQuery: String): String =
        "https://api.telegram.org/bot$token/$methodAndQuery"

    private fun saveOffset(offset: Long) {
        prefs.edit().putLong(KEY_LAST_OFFSET, offset.coerceAtLeast(0L)).apply()
    }

    private fun finishError(
        message: String,
        imported: Int = 0,
        duplicates: Int = 0,
        checked: Int = 0,
    ): DarbakAudioRemoteState = DarbakAudioRemoteState(
        status = DarbakAudioRemoteStatus.ERROR,
        message = message,
        importedCount = imported,
        duplicateCount = duplicates,
        checkedCount = checked,
    ).also { _state.value = it }

    private fun isOnline(): Boolean = try {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            manager.activeNetworkInfo?.isConnected == true
        }
    } catch (_: Throwable) {
        false
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class TelegramAudioCandidate(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
    )

    companion object {
        private const val TAG = "DarbakAudioTelegram"
        private const val PREFS = "darbak_audio_telegram"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_LAST_OFFSET = "last_update_offset"
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 20
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 90_000
        private const val MAX_AUDIO_BYTES = 200L * 1024L * 1024L
    }
}

data class DarbakAudioTelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
)

enum class DarbakAudioRemoteStatus { IDLE, CHECKING, DOWNLOADING, DONE, ERROR }

data class DarbakAudioRemoteState(
    val status: DarbakAudioRemoteStatus = DarbakAudioRemoteStatus.IDLE,
    val message: String = "",
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val checkedCount: Int = 0,
)
