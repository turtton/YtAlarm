package net.turtton.ytalarm.datasource.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.datasource.entity.VideoEntity

@OptIn(ExperimentalSerializationApi::class)
class VideoStateConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): VideoEntity.State? =
        value?.let { cbor.decodeFromByteArray(it) }

    @TypeConverter
    fun toByteArray(state: VideoEntity.State?): ByteArray? =
        state?.let { cbor.encodeToByteArray(it) }
}