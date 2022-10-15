package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.structure.Video

class VideoStateConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String?): Video.State? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromString(state: Video.State?): String? {
        return state?.let { json.encodeToString(it) }
    }
}