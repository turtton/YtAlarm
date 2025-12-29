package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@OptIn(ExperimentalSerializationApi::class)
class LongListConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): List<Long> =
        value?.let { cbor.decodeFromByteArray(it) } ?: emptyList()

    @TypeConverter
    fun toByteArray(list: List<Long>?): ByteArray? = list?.let { cbor.encodeToByteArray(it) }
}