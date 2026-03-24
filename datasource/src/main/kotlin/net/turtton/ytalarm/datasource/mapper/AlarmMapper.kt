@file:Suppress("NewApi")

package net.turtton.ytalarm.datasource.mapper

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.turtton.ytalarm.datasource.entity.AlarmEntity
import net.turtton.ytalarm.kernel.entity.Alarm
import java.util.Calendar
import java.util.Date
import kotlin.time.Instant

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    repeatType = repeatType.toDomain(),
    playlistIds = playListId,
    shouldLoop = shouldLoop,
    shouldShuffle = shouldShuffle,
    volume = Alarm.Volume(volume),
    snoozeMinute = snoozeMinute,
    shouldVibrate = shouldVibrate,
    isEnabled = isEnabled,
    creationDate = creationDate.toKotlinInstant(),
    lastUpdated = lastUpdated.toKotlinInstant()
)

internal fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    repeatType = repeatType.toEntity(),
    playListId = playlistIds,
    shouldLoop = shouldLoop,
    shouldShuffle = shouldShuffle,
    volume = volume.volume,
    snoozeMinute = snoozeMinute,
    shouldVibrate = shouldVibrate,
    isEnabled = isEnabled,
    creationDate = creationDate.toCalendar(),
    lastUpdated = lastUpdated.toCalendar()
)

private fun AlarmEntity.RepeatType.toDomain(): Alarm.RepeatType = when (this) {
    is AlarmEntity.RepeatType.Once -> Alarm.RepeatType.Once

    is AlarmEntity.RepeatType.Everyday -> Alarm.RepeatType.Everyday

    is AlarmEntity.RepeatType.Snooze -> Alarm.RepeatType.Snooze

    is AlarmEntity.RepeatType.Days -> Alarm.RepeatType.Days(
        days.map {
            it.calendarDayToDayOfWeek()
        }
    )

    is AlarmEntity.RepeatType.Date -> Alarm.RepeatType.Date(targetDate.toLocalDate())
}

internal fun Alarm.RepeatType.toEntity(): AlarmEntity.RepeatType = when (this) {
    is Alarm.RepeatType.Once -> AlarmEntity.RepeatType.Once
    is Alarm.RepeatType.Everyday -> AlarmEntity.RepeatType.Everyday
    is Alarm.RepeatType.Snooze -> AlarmEntity.RepeatType.Snooze
    is Alarm.RepeatType.Days -> AlarmEntity.RepeatType.Days(days.map { it.toCalendarDay() })
    is Alarm.RepeatType.Date -> AlarmEntity.RepeatType.Date(targetDate.toJavaDate())
}

// Calendar.SUNDAY=1, Calendar.MONDAY=2, ..., Calendar.SATURDAY=7
// DayOfWeek.MONDAY=1 (isoDayNumber), ..., DayOfWeek.SUNDAY=7 (isoDayNumber)
private fun Int.calendarDayToDayOfWeek(): DayOfWeek = when (this) {
    Calendar.MONDAY -> DayOfWeek.MONDAY
    Calendar.TUESDAY -> DayOfWeek.TUESDAY
    Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
    Calendar.THURSDAY -> DayOfWeek.THURSDAY
    Calendar.FRIDAY -> DayOfWeek.FRIDAY
    Calendar.SATURDAY -> DayOfWeek.SATURDAY
    Calendar.SUNDAY -> DayOfWeek.SUNDAY
    else -> throw IllegalArgumentException("Unknown Calendar day-of-week value: $this")
}

private fun DayOfWeek.toCalendarDay(): Int = when (this) {
    DayOfWeek.MONDAY -> Calendar.MONDAY
    DayOfWeek.TUESDAY -> Calendar.TUESDAY
    DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
    DayOfWeek.THURSDAY -> Calendar.THURSDAY
    DayOfWeek.FRIDAY -> Calendar.FRIDAY
    DayOfWeek.SATURDAY -> Calendar.SATURDAY
    DayOfWeek.SUNDAY -> Calendar.SUNDAY
}

private fun Date.toLocalDate(): LocalDate {
    val instant = Instant.fromEpochMilliseconds(time)
    return instant.toLocalDateTime(TimeZone.UTC).date
}

private fun LocalDate.toJavaDate(): Date {
    val instant = atStartOfDayIn(TimeZone.UTC)
    return Date(instant.toEpochMilliseconds())
}

internal fun Calendar.toKotlinInstant(): Instant = Instant.fromEpochMilliseconds(timeInMillis)

internal fun Instant.toCalendar(): Calendar = Calendar.getInstance().also {
    it.timeInMillis = toEpochMilliseconds()
}