package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@OptIn(ExperimentalSerializationApi::class)
class StringListConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): List<String> =
        value?.let { cbor.decodeFromByteArray(it) } ?: emptyList()

    @TypeConverter
    fun toByteArray(list: List<String>?): ByteArray? = list?.let { cbor.encodeToByteArray(it) }
}