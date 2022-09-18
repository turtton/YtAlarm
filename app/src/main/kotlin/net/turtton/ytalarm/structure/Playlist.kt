package net.turtton.ytalarm.structure

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    var title: String = "Playlist",
    val thumbnailUrl: String? = null,
    var videos: List<String> = emptyList()
)