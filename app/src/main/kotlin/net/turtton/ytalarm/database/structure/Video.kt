@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.database.structure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
        object Information : State

        @Serializable
        data class Downloading(val state: WorkerState) : State

        @Serializable
        data class Downloaded(
            @ColumnInfo(name = "internal_link")
            val internalLink: String = "",
            // byte size
            val fileSize: Int
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