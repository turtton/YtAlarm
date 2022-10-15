package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.structure.Playlist

class PlaylistTypeConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String?): Playlist.Type? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun toString(type: Playlist.Type?): String? {
        return type?.let { json.encodeToString(it) }
    }
}