@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.database.structure

import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.serializer.UUIDSerializer
import java.util.*

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    var title: String = "Playlist",
    val thumbnail: Thumbnail = Thumbnail.Drawable(R.drawable.ic_no_image),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var videos: List<Long> = emptyList(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val type: Type = Type.Original,
    @ColumnInfo(name = "creation_date")
    val creationDate: Calendar = Calendar.getInstance(),
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Calendar = Calendar.getInstance()
) {
    @Serializable
    sealed interface Thumbnail {
        @Serializable
        data class Video(val id: Long) : Thumbnail

        @Serializable
        data class Drawable(@DrawableRes val id: Int) : Thumbnail
    }

    @Serializable
    sealed interface Type {
        @Serializable
        object Importing : Type

        @Serializable
        object Original : Type

        @Serializable
        data class CloudPlaylist(
            val url: String,
            @SerialName("worker_id")
            val workerId: UUID,
            @SerialName("sync_rule")
            val syncRule: SyncRule = SyncRule.ALWAYS_ADD
        ) : Type
    }

    enum class SyncRule {
        ALWAYS_ADD, DELETE_IF_NOT_EXIST
    }
}