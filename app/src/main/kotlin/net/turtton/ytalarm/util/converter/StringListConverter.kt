package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StringListConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.let { json.decodeFromString(it) } ?: emptyList()
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.let { json.encodeToString(it) }
    }
}