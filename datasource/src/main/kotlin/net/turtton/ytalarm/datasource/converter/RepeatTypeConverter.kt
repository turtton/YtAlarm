package net.turtton.ytalarm.datasource.converter

import androidx.room.TypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.datasource.entity.AlarmEntity

@OptIn(ExperimentalSerializationApi::class)
class RepeatTypeConverter {
    private val cbor = Cbor

    @TypeConverter
    fun fromByteArray(value: ByteArray?): AlarmEntity.RepeatType? =
        value?.let { cbor.decodeFromByteArray(it) }

    @TypeConverter
    fun toByteArray(repeatType: AlarmEntity.RepeatType?): ByteArray? =
        repeatType?.let { cbor.encodeToByteArray(it) }
}