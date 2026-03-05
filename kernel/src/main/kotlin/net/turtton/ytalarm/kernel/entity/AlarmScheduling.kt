package net.turtton.ytalarm.kernel.entity

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private const val DAYS_IN_A_WEEK = 7
private const val ISO_MONDAY = 1
private const val ISO_SUNDAY = 7

/**
 * Returns the next fire time for this alarm after [now] in the given [timeZone].
 *
 * For [Alarm.RepeatType.Once], [Alarm.RepeatType.Everyday], and [Alarm.RepeatType.Snooze]:
 * - If the alarm time (hour:minute) is strictly after [now]'s time-of-day, the result is today's date.
 * - Otherwise (same time or past), the result is tomorrow's date.
 *
 * For [Alarm.RepeatType.Days]:
 * - Finds the nearest upcoming [DayOfWeek] from the list.
 * - If the nearest day is today but alarm time is at or before [now]'s time-of-day,
 *   picks the next occurrence (next week if only one day, or the next day in the list).
 *
 * For [Alarm.RepeatType.Date]:
 * - Uses the specified [LocalDate] directly.
 * - **Note**: If [Alarm.RepeatType.Date.targetDate] is in the past relative to [now],
 *   this function returns a past [Instant]. Callers such as [pickNearestTime] are responsible
 *   for filtering out past fire times if needed.
 */
fun Alarm.toNextFireTime(now: Instant, timeZone: TimeZone): Instant {
    val localNow = now.toLocalDateTime(timeZone)
    val targetTime = LocalTime(hour, minute, 0)

    val targetDate: LocalDate = when (val type = repeatType) {
        is Alarm.RepeatType.Once,
        is Alarm.RepeatType.Everyday,
        is Alarm.RepeatType.Snooze -> {
            if (now.isPrevOrSameTime(hour, minute, timeZone)) {
                localNow.date.plus(1, DateTimeUnit.DAY)
            } else {
                localNow.date
            }
        }

        is Alarm.RepeatType.Days -> {
            val nowDayOfWeek = localNow.dayOfWeek
            var nearestDay = type.days.getNearestDayOfWeekOrNull(nowDayOfWeek)
                ?: error("Days list is empty")
            var extraWeeks = 0

            if (nearestDay == nowDayOfWeek && now.isPrevOrSameTime(hour, minute, timeZone)) {
                // Today is the target day but the alarm time has passed or is now — skip to next occurrence
                val nextDayOfWeek = nowDayOfWeek.nextDayOfWeek()
                val nextNearest = type.days.getNearestDayOfWeekOrNull(nextDayOfWeek)
                    ?: error("Days list is empty")
                if (nearestDay == nextNearest) {
                    // Only one day in the week — same day next week
                    extraWeeks = DAYS_IN_A_WEEK
                }
                nearestDay = nextNearest
            }

            val daysUntil = daysUntilDayOfWeek(nowDayOfWeek, nearestDay) + extraWeeks
            localNow.date.plus(daysUntil, DateTimeUnit.DAY)
        }

        is Alarm.RepeatType.Date -> type.targetDate
    }

    return LocalDateTime(targetDate, targetTime).toInstant(timeZone)
}

/**
 * Returns the nearest alarm and its fire time from this list, or null if the list is empty.
 *
 * Alarms with [Alarm.RepeatType.Date] may produce a past fire time if the specified date has
 * already passed; such alarms are included as-is without filtering, since the caller
 * is responsible for deciding how to handle them.
 */
fun List<Alarm>.pickNearestTime(now: Instant, timeZone: TimeZone): Pair<Alarm, Instant>? =
    associateWith { it.toNextFireTime(now, timeZone) }
        .minByOrNull { (_, instant) -> instant }
        ?.toPair()

/**
 * Returns the nearest [DayOfWeek] from this list at or after [from], wrapping around the week.
 * Returns null if the list is empty.
 *
 * [DayOfWeek] uses ISO order: MONDAY=1, ..., SUNDAY=7
 */
fun List<DayOfWeek>.getNearestDayOfWeekOrNull(from: DayOfWeek): DayOfWeek? {
    if (isEmpty()) return null

    // Look for a day >= from (same week)
    val sameWeek: DayOfWeek? = filter { it.isoDayNumber >= from.isoDayNumber }
        .minByOrNull { it.isoDayNumber }

    if (sameWeek != null) return sameWeek

    // Wrap around: pick the earliest day next week
    return minByOrNull { it.isoDayNumber }
}

/**
 * Returns the number of days from [from] to [to], wrapping around the week if needed.
 * Returns 0 if [from] == [to].
 */
private fun daysUntilDayOfWeek(from: DayOfWeek, to: DayOfWeek): Int {
    val diff = to.isoDayNumber - from.isoDayNumber
    return if (diff < 0) diff + DAYS_IN_A_WEEK else diff
}

/**
 * Returns the next [DayOfWeek] after this one (wraps SUNDAY -> MONDAY).
 */
private fun DayOfWeek.nextDayOfWeek(): DayOfWeek =
    DayOfWeek(if (isoDayNumber == ISO_SUNDAY) ISO_MONDAY else isoDayNumber + 1)

/**
 * Returns true if the alarm time (targetHour:targetMinute) is at or before this [Instant]'s
 * time-of-day in the given [timeZone]. In other words, the alarm time is in the past or now.
 */
fun Instant.isPrevOrSameTime(targetHour: Int, targetMinute: Int, timeZone: TimeZone): Boolean {
    val local = toLocalDateTime(timeZone)
    val nowHour = local.hour
    val nowMinute = local.minute
    return targetHour < nowHour || (targetHour == nowHour && targetMinute <= nowMinute)
}

/**
 * Returns true if this [Instant] and [other] represent the same calendar date in [timeZone].
 */
fun Instant.isSameDate(other: Instant, timeZone: TimeZone): Boolean {
    val date1 = toLocalDateTime(timeZone).date
    val date2 = other.toLocalDateTime(timeZone).date
    return date1 == date2
}