package com.example.data

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
    private var scanJob: Job? = null
    private var lastResumePersistAtElapsed = 0L

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_playbackState.value.isPlaying) pause()
            }
        }
    }

    private val mediaSession: MediaSession = MediaSession(context, "Launcher2026Music").apply {
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = resume()
            override fun onPause() = pause()
            override fun onSkipToNext() = playNext()
            override fun onSkipToPrevious() = playPrevious()
            override fun onSeekTo(pos: Long) = seekTo(pos)
            override fun onStop() = pause()
        }, Handler(Looper.getMainLooper()))
        isActive = true
    }

    init {
        updateMediaSessionState()
    }

    fun initialize() {
        scanJob?.cancel()
        scanJob = serviceScope.launch(Dispatchers.IO) {
            val tracks = scanLocalAudioFiles()
            withContext(Dispatchers.Main) {
                _playbackState.value = _playbackState.value.copy(playlist = tracks)

                val settings = preferencesManager.getSettings()
                if (settings.resumeMusicPlayback) {
                    val lastPath = preferencesManager.getLastMusicPath()
                    val lastPos = preferencesManager.getLastMusicPosition()
                    if (lastPath != null) {
                        val matched = tracks.find { it.dataPath == lastPath } ?: tracks.firstOrNull()
                        if (matched != null) {
                            val safePosition = if (matched.dataPath == lastPath) {
                                lastPos.coerceIn(0L, matched.durationMs.coerceAtLeast(0L))
                            } else 0L
                            _playbackState.value = _playbackState.value.copy(
                                currentTrack = matched,
                                currentPositionMs = safePosition,
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
                updateMediaSessionState()
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

        return tracks
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        try {
            audioManager?.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to request audio focus", e)
        }
    }

    fun playTrack(track: MusicTrack, startPositionMs: Long = 0L) {
        try {
            requestAudioFocus()
            stopCurrentPlayer()
            val file = File(track.dataPath)
            if (!file.exists()) {
                _playbackState.value = _playbackState.value.copy(errorMessage = "الملف غير موجود في الذاكرة")
                updateMediaSessionState()
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
                if (startPositionMs > 0 && startPositionMs < duration) seekTo(startPositionMs.toInt())
                start()
                setOnCompletionListener { playNext() }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        errorMessage = "تعذر تشغيل الملف الصوتي"
                    )
                    updateMediaSessionState()
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
            updateMediaSessionState()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                errorMessage = "تعذر تشغيل المقطع الصوتي"
            )
            updateMediaSessionState()
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            val first = current.playlist.firstOrNull()
            if (first != null) playTrack(first, current.currentPositionMs)
            return
        }
        if (current.isPlaying) pause() else resume()
    }

    fun resume() {
        val track = _playbackState.value.currentTrack ?: run {
            _playbackState.value.playlist.firstOrNull()?.let { playTrack(it) }
            return
        }
        requestAudioFocus()
        try {
            if (mediaPlayer == null) {
                playTrack(track, _playbackState.value.currentPositionMs)
            } else {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracker()
                updateMediaSessionState()
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
            _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = pos)
            progressJob?.cancel()
            saveResumeState(_playbackState.value.currentTrack?.dataPath, pos)
            updateMediaSessionState()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            updateMediaSessionState()
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
            if (mediaPlayer != null) mediaPlayer?.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
            saveResumeState(_playbackState.value.currentTrack?.dataPath, positionMs)
            updateMediaSessionState()
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
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: _playbackState.value.currentPositionMs
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = pos)
                    if (SystemClock.elapsedRealtime() - lastResumePersistAtElapsed >= RESUME_SAVE_INTERVAL_MS) {
                        saveResumeState(currentTrack.dataPath, pos)
                    }
                    updateMediaSessionState()
                }
            }
        }
    }

    private fun updateMediaSessionState() {
        try {
            val current = _playbackState.value
            val state = if (current.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

            mediaSession.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, current.currentPositionMs, if (current.isPlaying) 1f else 0f)
                    .build()
            )

            current.currentTrack?.let { track ->
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, current.durationMs.takeIf { it > 0 } ?: track.durationMs)
                        .build()
                )
            }
            mediaSession.isActive = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to update media session", e)
        }
    }

    private fun saveResumeState(path: String?, pos: Long) {
        if (path != null) {
            preferencesManager.saveMusicResumeState(path, pos)
            lastResumePersistAtElapsed = SystemClock.elapsedRealtime()
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

    @Suppress("DEPRECATION")
    fun release() {
        val current = _playbackState.value
        saveResumeState(current.currentTrack?.dataPath, mediaPlayer?.currentPosition?.toLong() ?: current.currentPositionMs)
        scanJob?.cancel()
        stopCurrentPlayer()
        try { audioManager?.abandonAudioFocus(audioFocusListener) } catch (_: Exception) { }
        try {
            mediaSession.isActive = false
            mediaSession.release()
        } catch (_: Exception) { }
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "MusicPlayerService"
        private const val RESUME_SAVE_INTERVAL_MS = 15_000L
    }
}
