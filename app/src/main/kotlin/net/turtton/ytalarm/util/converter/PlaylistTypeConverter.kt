package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.structure.Playlist

@OptIn(ExperimentalSerializationApi::class)
class PlaylistTypeConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Playlist.Type? {
        return value?.let { cbor.decodeFromByteArray(it) }
    }

    @TypeConverter
    fun toByteArray(type: Playlist.Type?): ByteArray? {
        return type?.let { cbor.encodeToByteArray(it) }
    }
}