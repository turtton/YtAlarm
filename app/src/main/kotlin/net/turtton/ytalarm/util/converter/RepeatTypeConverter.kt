package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.util.RepeatType

class RepeatTypeConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String?): RepeatType? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun toString(repeatType: RepeatType?): String? {
        return repeatType?.let { json.encodeToString(it) }
    }
}