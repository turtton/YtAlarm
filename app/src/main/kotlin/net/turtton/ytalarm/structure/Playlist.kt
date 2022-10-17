@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.structure

import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.serializer.UUIDSerializer
import java.util.UUID

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    var title: String = "Playlist",
    val thumbnail: Thumbnail = Thumbnail.Drawable(R.drawable.ic_no_image),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var videos: List<Long> = emptyList(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val type: Type = Type.Original
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
        object Downloading : Type

        @Serializable
        object Original : Type

        @Serializable
        data class CloudPlaylist(val url: String, val syncState: SyncState = SyncState.Done) : Type
    }

    @Serializable
    sealed interface SyncState {
        @Serializable
        object Done : SyncState

        @Serializable
        data class Syncing(val workerId: UUID) : SyncState
    }
}