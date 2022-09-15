package net.turtton.ytalarm.structure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class Video(
    @PrimaryKey
    val id: String,
    val title: String = "No title",
    @ColumnInfo(name = "thumbnail_link")
    val thumbnailUrl: String = "",
    @ColumnInfo(name = "video_url")
    val videoUrl: String = "",
    @ColumnInfo(name = "internal_link")
    val internalLink: String = "",
    val domain: String = "",
    // byte size
    @ColumnInfo(name = "file_size")
    val fileSize: Int = -1,
)