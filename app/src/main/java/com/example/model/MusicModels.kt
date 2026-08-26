package com.example.model

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String,
    val albumId: Long = 0
)

data class MusicPlaybackState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volumeLevel: Float = 0.8f,
    val playlist: List<MusicTrack> = emptyList(),
    val errorMessage: String? = null
)
