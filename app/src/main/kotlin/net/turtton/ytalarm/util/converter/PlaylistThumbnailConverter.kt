package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.database.structure.Playlist

@OptIn(ExperimentalSerializationApi::class)
class PlaylistThumbnailConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Playlist.Thumbnail? =
        value?.let { cbor.decodeFromByteArray(it) }

    @TypeConverter
    fun toByteArray(type: Playlist.Thumbnail?): ByteArray? =
        type?.let { cbor.encodeToByteArray(it) }
}