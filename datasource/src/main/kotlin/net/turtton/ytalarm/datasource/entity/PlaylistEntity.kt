package net.turtton.ytalarm.datasource.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "Playlist",
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val thumbnail: Thumbnail = Thumbnail.None,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val videos: List<Long> = emptyList(),
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
        @SerialName("Video")
        data class Video(val id: Long) : Thumbnail

        @Serializable
        @SerialName("None")
        data object None : Thumbnail
    }

    @Serializable
    sealed interface Type {
        @Serializable
        @SerialName("Importing")
        data object Importing : Type

        @Serializable
        @SerialName("Original")
        data object Original : Type

        @Serializable
        @SerialName("CloudPlaylist")
        data class CloudPlaylist(
            val url: String,
            @SerialName("sync_rule")
            val syncRule: SyncRule = SyncRule.ALWAYS_ADD
        ) : Type
    }

    enum class SyncRule {
        ALWAYS_ADD,
        DELETE_IF_NOT_EXIST
    }
}