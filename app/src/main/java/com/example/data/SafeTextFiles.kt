package com.example.data

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

const val SAFE_TEXT_IMPORT_LIMIT_BYTES = 8L * 1024L * 1024L

fun ContentResolver.readUtf8TextLimited(
    uri: Uri,
    maxBytes: Long = SAFE_TEXT_IMPORT_LIMIT_BYTES
): String {
    val declaredLength = runCatching {
        openAssetFileDescriptor(uri, "r")?.use { it.length }
    }.getOrNull()
    if (declaredLength != null && declaredLength >= 0L && declaredLength > maxBytes) {
        throw IOException("الملف أكبر من الحد الآمن للاستيراد")
    }
    val maxChars = maxBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
        val result = StringBuilder(minOf(maxChars, 64 * 1024))
        val buffer = CharArray(8 * 1024)
        var total = 0
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxChars) throw IOException("الملف أكبر من الحد الآمن للاستيراد")
            result.append(buffer, 0, read)
        }
        result.toString()
    } ?: throw IOException("تعذر فتح الملف")
}
