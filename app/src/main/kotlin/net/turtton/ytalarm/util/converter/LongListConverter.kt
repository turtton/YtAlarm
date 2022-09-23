package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LongListConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String?): List<Long> {
        return value?.let { json.decodeFromString(it) } ?: emptyList()
    }

    @TypeConverter
    fun toString(list: List<Long>?): String? {
        return list?.let { json.encodeToString(it) }
    }
}