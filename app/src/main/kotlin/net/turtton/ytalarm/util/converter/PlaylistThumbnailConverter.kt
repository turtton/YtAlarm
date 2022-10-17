package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.structure.Playlist

@OptIn(ExperimentalSerializationApi::class)
class PlaylistThumbnailConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Playlist.Thumbnail? {
        return value?.let { cbor.decodeFromByteArray(it) }
    }

    @TypeConverter
    fun toByteArray(type: Playlist.Thumbnail?): ByteArray? {
        return type?.let { cbor.encodeToByteArray(it) }
    }
}