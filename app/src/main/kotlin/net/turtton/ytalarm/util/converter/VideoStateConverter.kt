package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.structure.Video

@OptIn(ExperimentalSerializationApi::class)
class VideoStateConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Video.State? {
        return value?.let { cbor.decodeFromByteArray(it) }
    }

    @TypeConverter
    fun fromByteArray(state: Video.State?): ByteArray? {
        return state?.let { cbor.encodeToByteArray(it) }
    }
}