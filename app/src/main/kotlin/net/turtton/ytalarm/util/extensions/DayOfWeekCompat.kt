package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.util.DayOfWeekCompat

fun List<DayOfWeekCompat>.getNearestWeekOrNull(calendarDayOfWeek: Int): DayOfWeekCompat? {
    var nearestWeek: DayOfWeekCompat? = null
    var nearestWeekDiff: Int? = null

    var nextWeek: DayOfWeekCompat? = null
    var nextWeekDiff: Int? = null

    associateWith(DayOfWeekCompat::convertCalenderCode)
        .forEach { (dayOfWeek, calendarCode) ->
            val diff = calendarCode - calendarDayOfWeek
            if (diff < 0) {
                if (nextWeek == null || nextWeekDiff!! > diff) {
                    nextWeek = dayOfWeek
                    nextWeekDiff = diff
                }
            } else {
                if (nearestWeek == null || nearestWeekDiff!! > diff) {
                    nearestWeek = dayOfWeek
                    nearestWeekDiff = diff
                }
            }
        }

    return nearestWeek ?: nextWeek
}

fun List<DayOfWeekCompat>.getNearestWeek(calendarDayOfWeek: Int): DayOfWeekCompat =
    getNearestWeekOrNull(calendarDayOfWeek) ?: error("Empty list")