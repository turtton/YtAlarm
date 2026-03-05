package net.turtton.ytalarm.datasource.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "video_id")
    val videoId: String,
    val title: String = "No title",
    @ColumnInfo(name = "thumbnail_link")
    val thumbnailUrl: String = "",
    @ColumnInfo(name = "video_url")
    val videoUrl: String = "",
    val domain: String = "",
    @ColumnInfo(name = "state_data", typeAffinity = ColumnInfo.BLOB)
    val stateData: State,
    @ColumnInfo(name = "creation_date")
    val creationDate: Calendar = Calendar.getInstance()
) {
    @Serializable
    sealed interface State {
        @Serializable
        data object Importing : State

        @Serializable
        data class Information(
            @SerialName("is_streamable")
            val isStreamable: Boolean = true
        ) : State

        @Serializable
        data object Downloading : State

        @Serializable
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            @SerialName("file_size")
            val fileSize: Long,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : State

        @Serializable
        data class Failed(
            @SerialName("source_url")
            val sourceUrl: String
        ) : State
    }
}