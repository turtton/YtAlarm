package net.turtton.ytalarm.util.converter

import androidx.room.TypeConverter
import net.turtton.ytalarm.database.structure.Alarm

class AlarmVolumeConverter {
    @TypeConverter
    fun fromInt(value: Int?): Alarm.Volume? = value?.let { Alarm.Volume(it) }

    @TypeConverter
    fun toInt(value: Alarm.Volume?): Int? = value?.volume
}