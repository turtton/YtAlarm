package net.turtton.ytalarm.datasource.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.datasource.entity.PlaylistEntity

@OptIn(ExperimentalSerializationApi::class)
class PlaylistTypeConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): PlaylistEntity.Type? =
        value?.let { cbor.decodeFromByteArray(it) }

    @TypeConverter
    fun toByteArray(type: PlaylistEntity.Type?): ByteArray? =
        type?.let { cbor.encodeToByteArray(it) }
}