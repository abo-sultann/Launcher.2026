package com.example.model

data class MapItem(
    val id: String,
    val name: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val isActive: Boolean = false,
    val dateAdded: String = ""
)
