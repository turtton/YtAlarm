@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.database.structure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.turtton.ytalarm.util.serializer.UUIDSerializer
import java.util.Calendar
import java.util.UUID

@Entity(tableName = "videos")
data class Video(
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
        fun isUpdating(): Boolean = when (this) {
            is Importing, is Downloading -> true
            else -> false
        }

        @Serializable
        data class Importing(val state: WorkerState) : State

        @Serializable
        data class Information(val downloadOnly: Boolean = false) : State

        @Serializable
        data class Downloading(val state: WorkerState) : State

        @Serializable
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            // byte size
            @SerialName("file_size")
            val fileSize: Int,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : State
    }

    @Serializable
    sealed interface WorkerState {
        @Serializable
        data class Failed(val url: String) : WorkerState

        @Serializable
        data class Working(val workerId: UUID) : WorkerState
    }
}