package net.turtton.ytalarm.datasource.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.datasource.entity.PlaylistEntity

@OptIn(ExperimentalSerializationApi::class)
class PlaylistThumbnailConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): PlaylistEntity.Thumbnail? =
        value?.let { cbor.decodeFromByteArray(it) }

    @TypeConverter
    fun toByteArray(thumbnail: PlaylistEntity.Thumbnail?): ByteArray? =
        thumbnail?.let { cbor.encodeToByteArray(it) }
}