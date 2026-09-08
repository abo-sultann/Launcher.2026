package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One downsampled cover per active track; no artwork decoding on the UI thread. */
@Composable
internal fun DarbakMusicArtwork(path: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(null, path) {
        value = null
        if (!path.isNullOrBlank()) value = withContext(Dispatchers.IO) { decodeCover(context, path) }
    }
    Box(modifier.clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(CarbonSurface, CarbonDark))), contentAlignment = Alignment.Center) {
        val cover = bitmap
        if (cover == null) Icon(Icons.Default.MusicNote, null, tint = CyanNeon, modifier = Modifier.size(64.dp))
        else Image(cover.asImageBitmap(), "غلاف المقطع", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun decodeCover(context: Context, path: String): Bitmap? {
    val reader = MediaMetadataRetriever()
    return try {
        if (path.startsWith("content://")) reader.setDataSource(context, Uri.parse(path)) else reader.setDataSource(path)
        val bytes = reader.embeddedPicture ?: return null
        if (bytes.size > 8 * 1024 * 1024) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 320 || bounds.outHeight / sample > 320) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) { null }
    finally { try { reader.release() } catch (_: Exception) { } }
}
