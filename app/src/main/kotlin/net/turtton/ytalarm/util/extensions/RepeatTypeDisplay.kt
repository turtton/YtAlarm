package net.turtton.ytalarm.util.extensions

import android.content.Context
import android.os.Build
import kotlinx.datetime.DayOfWeek
import net.turtton.ytalarm.R
import net.turtton.ytalarm.kernel.entity.Alarm
import java.text.DateFormat
import java.util.Calendar

/**
 * Returns a human-readable display string for this [Alarm.RepeatType].
 *
 * This is an App-layer Extension function that converts the pure domain [Alarm.RepeatType]
 * to a locale-aware string using Android [Context] resources.
 */
fun Alarm.RepeatType.getDisplay(context: Context): String = when (this) {
    is Alarm.RepeatType.Once -> context.getString(R.string.repeat_type_once)

    is Alarm.RepeatType.Everyday -> context.getString(R.string.repeat_type_everyday)

    is Alarm.RepeatType.Snooze -> ""

    is Alarm.RepeatType.Days -> days.mapNotNull { it.getDisplay(context) }.joinToString(", ")

    is Alarm.RepeatType.Date -> DateFormat.getDateInstance().format(
        java.util.Date(
            java.util.Calendar.getInstance().apply {
                set(targetDate.year, targetDate.monthNumber - 1, targetDate.dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    )
}

/**
 * Returns a human-readable short display name for this [DayOfWeek], or null if unavailable.
 *
 * Uses the device locale and Android Calendar API to get the localized day name.
 * This is an App-layer Extension function.
 */
fun DayOfWeek.getDisplay(context: Context): String? {
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Calendar.SHORT_FORMAT
    } else {
        Calendar.SHORT
    }
    val primaryLocale = context.resources.configuration.locales[0]
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, toCalendarDayOfWeek())
    return calendar.getDisplayName(Calendar.DAY_OF_WEEK, format, primaryLocale)
}

/**
 * Converts a kotlinx.datetime [DayOfWeek] to the [java.util.Calendar] day-of-week constant.
 *
 * Note: kotlinx.datetime.DayOfWeek on JVM corresponds to java.time.DayOfWeek (API 26+).
 * The @SuppressLint annotation is required because this code runs on devices with API 24+,
 * but kotlinx-datetime handles the compatibility internally.
 */
@Suppress("NewApi")
private fun DayOfWeek.toCalendarDayOfWeek(): Int = when (this) {
    DayOfWeek.MONDAY -> Calendar.MONDAY
    DayOfWeek.TUESDAY -> Calendar.TUESDAY
    DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
    DayOfWeek.THURSDAY -> Calendar.THURSDAY
    DayOfWeek.FRIDAY -> Calendar.FRIDAY
    DayOfWeek.SATURDAY -> Calendar.SATURDAY
    DayOfWeek.SUNDAY -> Calendar.SUNDAY
}