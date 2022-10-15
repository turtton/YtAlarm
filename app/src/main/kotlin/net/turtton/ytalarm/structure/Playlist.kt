@file:UseSerializers(UUIDSerializer::class)

package net.turtton.ytalarm.structure

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.turtton.ytalarm.util.serializer.UUIDSerializer
import java.util.UUID

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    var title: String = "Playlist",
    val thumbnailUrl: String? = null,
    var videos: List<Long> = emptyList(),
    val type: Type = Type.Original
) {
    @Serializable
    sealed interface Type {
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