package net.turtton.ytalarm.util

import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
enum class DayOfWeekCompat {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    fun getDisplay(context: Context): CharSequence? {
        val format = if (Build.VERSION.SDK_INT >= 26) {
            Calendar.SHORT_FORMAT
        } else {
            Calendar.SHORT
        }
        val primaryLocale = context.resources.configuration.locales[0]
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, convertCalenderCode())
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, format, primaryLocale)
    }

    fun convertCalenderCode(): Int = when (this) {
        MONDAY -> Calendar.MONDAY
        TUESDAY -> Calendar.TUESDAY
        WEDNESDAY -> Calendar.WEDNESDAY
        THURSDAY -> Calendar.THURSDAY
        FRIDAY -> Calendar.FRIDAY
        SATURDAY -> Calendar.SATURDAY
        SUNDAY -> Calendar.SUNDAY
    }
}