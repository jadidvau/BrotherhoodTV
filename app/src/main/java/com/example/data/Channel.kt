package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long = 0L, // 0 means never watched, otherwise timestamp
    val pingMs: Long? = null, // null means unchecked, -1 means offline
    val isSeed: Boolean = false,
    val playlistId: Int? = null // links channels to an added playlist
)
