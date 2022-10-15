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
    @PrimaryKey
    val id: String,
    val title: String = "No title",
    @ColumnInfo(name = "thumbnail_link")
    val thumbnailUrl: String = "",
    @ColumnInfo(name = "video_url")
    val videoUrl: String = "",
    val domain: String = "",
    val stateData: State
) {
    @Serializable
    sealed interface State {
        @Serializable
        @JvmInline
        value class Importing(val workerId: UUID) : State

        @Serializable
        object Information : State

        @Serializable
        @JvmInline
        value class Downloading(val workerId: UUID) : State

        @Serializable
        data class Downloaded(
            @ColumnInfo(name = "internal_link")
            val internalLink: String = "",
            // byte size
            val fileSize: Int
        ) : State
    }
}