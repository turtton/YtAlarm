package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.util.RepeatType

@OptIn(ExperimentalSerializationApi::class)
class RepeatTypeConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): RepeatType? {
        return value?.let { cbor.decodeFromByteArray(it) }
    }

    @TypeConverter
    fun toByteArray(repeatType: RepeatType?): ByteArray? {
        return repeatType?.let { cbor.encodeToByteArray(it) }
    }
}