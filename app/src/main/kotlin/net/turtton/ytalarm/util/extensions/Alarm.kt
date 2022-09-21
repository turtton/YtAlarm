package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.util.RepeatType
import java.util.Calendar

fun Alarm.toCalendar(now: Calendar): Calendar {
    val (hour, minute) = time.split(':').map { s -> s.toInt() }

    val flagHour = Calendar.HOUR_OF_DAY
    val flagMinute = Calendar.MINUTE
    val flagDayOfWeek = Calendar.DAY_OF_WEEK

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now.timeInMillis

    val nowDayOfWeek = now[flagDayOfWeek]
    when (repeatType) {
        is RepeatType.Once, is RepeatType.Everyday, is RepeatType.Snooze -> {
            if (now.isPrevOrSameTime(hour, minute)) {
                calendar.add(Calendar.DATE, 1)
            }
        }
        is RepeatType.Days -> {
            val targetDays = repeatType.days
            var week = targetDays.getNearestWeek(nowDayOfWeek).convertCalenderCode()
            if (nowDayOfWeek == week && now.isPrevOrSameTime(hour, minute)) {
                val nextDay = nowDayOfWeek.let { if (it == 7) 0 else it + 1 }
                val nextTarget = targetDays.getNearestWeek(nextDay).convertCalenderCode()
                if (week == nextTarget) {
                    calendar.add(Calendar.DATE, 7)
                }
                week = nextTarget
            }
            if (nowDayOfWeek > week) {
                calendar.add(Calendar.DATE, 7)
            }
            calendar.set(flagDayOfWeek, week)
        }
        is RepeatType.Date -> {
            calendar.timeInMillis = repeatType.targetDate.time
        }
    }

    calendar.set(flagHour, hour)
    calendar.set(flagMinute, minute)
    calendar.set(Calendar.SECOND, 0)

    return calendar
}

fun List<Alarm>.pickNearestTime(nowTime: Calendar): Pair<Alarm, Calendar>? {
    return associateWith { it.toCalendar(nowTime) }
        .minByOrNull { (_, calendar) -> calendar.timeInMillis }
        ?.toPair()
}