package com.example.data

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.MusicPlaybackState
import com.example.model.MusicTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MusicPlayerService(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null

    fun initialize() {
        serviceScope.launch(Dispatchers.IO) {
            val tracks = scanLocalAudioFiles()
            withContext(Dispatchers.Main) {
                _playbackState.value = _playbackState.value.copy(playlist = tracks)

                // Restore last played track and position if enabled
                val settings = preferencesManager.getSettings()
                if (settings.resumeMusicPlayback) {
                    val lastPath = preferencesManager.getLastMusicPath()
                    val lastPos = preferencesManager.getLastMusicPosition()
                    if (lastPath != null) {
                        val matched = tracks.find { it.dataPath == lastPath } ?: tracks.firstOrNull()
                        if (matched != null) {
                            _playbackState.value = _playbackState.value.copy(
                                currentTrack = matched,
                                currentPositionMs = lastPos,
                                durationMs = matched.durationMs
                            )
                        }
                    } else if (tracks.isNotEmpty()) {
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = tracks.first(),
                            durationMs = tracks.first().durationMs
                        )
                    }
                }
            }
        }
    }

    fun scanLocalAudioFiles(): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "مقطع صوتي غير معروف"
                    val artist = it.getString(artistCol) ?: "فنان غير معروف"
                    val album = it.getString(albumCol) ?: "ألبوم غير معروف"
                    val duration = it.getLong(durCol)
                    val path = it.getString(dataCol) ?: ""
                    val albumId = it.getLong(albumIdCol)

                    if (File(path).exists() || path.isNotBlank()) {
                        tracks.add(
                            MusicTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                dataPath = path,
                                albumId = albumId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore for audio", e)
        }

        // If MediaStore is empty (e.g. fresh emulator/device), provide high quality offline demo tracks so the user can test the UI instantly
        if (tracks.isEmpty()) {
            tracks.addAll(getBuiltInFallbackTracks())
        }

        return tracks
    }

    private fun getBuiltInFallbackTracks(): List<MusicTrack> {
        return listOf(
            MusicTrack(
                id = 1L,
                title = "محطة الرحلة — هدوء الطريق السريع",
                artist = "راديو السيارة 2026",
                album = "موسيقى القيادة الهادئة",
                durationMs = 240000L,
                dataPath = "demo://track1"
            ),
            MusicTrack(
                id = 2L,
                title = "ألحان الصحراء والليل",
                artist = "نغمات خليجية",
                album = "طريق السفر",
                durationMs = 310000L,
                dataPath = "demo://track2"
            ),
            MusicTrack(
                id = 3L,
                title = "إيقاع رياضي فاخر — Turbo Drive",
                artist = "Automotive Sound",
                album = "Sports Cockpit",
                durationMs = 195000L,
                dataPath = "demo://track3"
            )
        )
    }

    fun playTrack(track: MusicTrack, startPositionMs: Long = 0L) {
        try {
            stopCurrentPlayer()
            if (track.dataPath.startsWith("demo://")) {
                // Simulated offline demo playback with real progress ticks
                _playbackState.value = _playbackState.value.copy(
                    currentTrack = track,
                    isPlaying = true,
                    currentPositionMs = startPositionMs,
                    durationMs = track.durationMs,
                    errorMessage = null
                )
                startProgressTracker()
                saveResumeState(track.dataPath, startPositionMs)
                return
            }

            val file = File(track.dataPath)
            if (!file.exists()) {
                _playbackState.value = _playbackState.value.copy(
                    errorMessage = "الملف غير موجود في الذاكرة"
                )
                return
            }

            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                setDataSource(context, Uri.fromFile(file))
                prepare()
                if (startPositionMs > 0 && startPositionMs < duration) {
                    seekTo(startPositionMs.toInt())
                }
                start()
                setOnCompletionListener {
                    playNext()
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        errorMessage = "تعذر تشغيل الملف الصوتي"
                    )
                    true
                }
            }

            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = true,
                currentPositionMs = startPositionMs,
                durationMs = mediaPlayer?.duration?.toLong() ?: track.durationMs,
                errorMessage = null
            )
            startProgressTracker()
            saveResumeState(track.dataPath, startPositionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                errorMessage = "تعذر تشغيل المقطع الصوتي"
            )
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            val first = current.playlist.firstOrNull()
            if (first != null) {
                playTrack(first, current.currentPositionMs)
            }
            return
        }

        if (current.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun resume() {
        val track = _playbackState.value.currentTrack ?: return
        if (track.dataPath.startsWith("demo://")) {
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker()
            return
        }

        try {
            if (mediaPlayer == null) {
                playTrack(track, _playbackState.value.currentPositionMs)
            } else {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback", e)
            playTrack(track, _playbackState.value.currentPositionMs)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            val pos = mediaPlayer?.currentPosition?.toLong() ?: _playbackState.value.currentPositionMs
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                currentPositionMs = pos
            )
            progressJob?.cancel()
            saveResumeState(_playbackState.value.currentTrack?.dataPath, pos)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun playNext() {
        val playlist = _playbackState.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.dataPath == _playbackState.value.currentTrack?.dataPath }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < playlist.size) currentIndex + 1 else 0
        playTrack(playlist[nextIndex], 0L)
    }

    fun playPrevious() {
        val playlist = _playbackState.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.dataPath == _playbackState.value.currentTrack?.dataPath }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        playTrack(playlist[prevIndex], 0L)
    }

    fun seekTo(positionMs: Long) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
            saveResumeState(_playbackState.value.currentTrack?.dataPath, positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun skipForward10Sec() {
        val newPos = (_playbackState.value.currentPositionMs + 10000L).coerceAtMost(_playbackState.value.durationMs)
        seekTo(newPos)
    }

    fun skipBackward10Sec() {
        val newPos = (_playbackState.value.currentPositionMs - 10000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun setVolume(delta: Float) {
        try {
            audioManager?.let { am ->
                val direction = if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting system volume", e)
        }
    }

    fun toggleMute() {
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                } else {
                    @Suppress("DEPRECATION")
                    am.setStreamMute(AudioManager.STREAM_MUSIC, !am.isMusicActive)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                delay(1000L)
                val currentTrack = _playbackState.value.currentTrack
                if (currentTrack != null) {
                    val pos = if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.currentPosition?.toLong() ?: _playbackState.value.currentPositionMs
                    } else {
                        val simulated = _playbackState.value.currentPositionMs + 1000L
                        if (simulated >= _playbackState.value.durationMs) {
                            playNext()
                            0L
                        } else {
                            simulated
                        }
                    }
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = pos)
                    saveResumeState(currentTrack.dataPath, pos)
                }
            }
        }
    }

    private fun saveResumeState(path: String?, pos: Long) {
        if (path != null) {
            preferencesManager.saveMusicResumeState(path, pos)
        }
    }

    private fun stopCurrentPlayer() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        stopCurrentPlayer()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "MusicPlayerService"
    }
}
