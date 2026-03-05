package net.turtton.ytalarm.datasource.converter

import androidx.room.TypeConverter
import java.util.Calendar

class CalendarConverter {
    @TypeConverter
    fun fromLong(value: Long?): Calendar? = value?.let {
        Calendar.getInstance().also { cal ->
            cal.timeInMillis = value
        }
    }

    @TypeConverter
    fun toLong(value: Calendar?): Long? = value?.timeInMillis
}