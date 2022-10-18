@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.structure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.turtton.ytalarm.util.serializer.UUIDSerializer
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
    val stateData: State
) {
    @Serializable
    sealed interface State {
        fun isUpdating(): Boolean = when (this) {
            is Importing, is Downloading -> true
            else -> false
        }

        val workerId: UUID?
            get() = null

        @Serializable
        data class Importing(override val workerId: UUID) : State

        @Serializable
        object Information : State

        @Serializable
        data class Downloading(override val workerId: UUID) : State

        @Serializable
        data class Downloaded(
            @ColumnInfo(name = "internal_link")
            val internalLink: String = "",
            // byte size
            val fileSize: Int
        ) : State
    }
}