package net.turtton.ytalarm.util.extensions

import java.util.Calendar

fun Calendar.isSameDate(calendar: Calendar): Boolean {
    val year = get(Calendar.YEAR)
    val date = get(Calendar.DAY_OF_YEAR)

    val targetYear = calendar.get(Calendar.YEAR)
    val targetDate = calendar.get(Calendar.DAY_OF_YEAR)

    return year == targetYear && date == targetDate
}

fun Calendar.isPrevOrSameTime(targetHour: Int, targetMinute: Int): Boolean {
    val hour = get(Calendar.HOUR_OF_DAY)
    val minute = get(Calendar.MINUTE)

    return targetHour < hour || (targetHour == hour && targetMinute <= minute)
}