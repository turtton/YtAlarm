package net.turtton.ytalarm.util.extensions

import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.util.DayOfWeekCompat.Companion.A_WEEK
import java.util.Calendar

fun Alarm.toCalendar(now: Calendar): Calendar {
    val flagHour = Calendar.HOUR_OF_DAY
    val flagMinute = Calendar.MINUTE
    val flagDayOfWeek = Calendar.DAY_OF_WEEK

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now.timeInMillis

    val nowDayOfWeek = now[flagDayOfWeek]
    when (repeatType) {
        is Alarm.RepeatType.Once, is Alarm.RepeatType.Everyday, is Alarm.RepeatType.Snooze -> {
            if (now.isPrevOrSameTime(hour, minute)) {
                calendar.add(Calendar.DATE, 1)
            }
        }

        is Alarm.RepeatType.Days -> {
            val targetDays = repeatType.days
            var week = targetDays.getNearestWeek(nowDayOfWeek).convertCalenderCode()
            if (nowDayOfWeek == week && now.isPrevOrSameTime(hour, minute)) {
                val nextDay = nowDayOfWeek.let { if (it == Calendar.THURSDAY) 0 else it + 1 }
                val nextTarget = targetDays.getNearestWeek(nextDay).convertCalenderCode()
                if (week == nextTarget) {
                    calendar.add(Calendar.DATE, A_WEEK)
                }
                week = nextTarget
            }
            if (nowDayOfWeek > week) {
                calendar.add(Calendar.DATE, A_WEEK)
            }
            calendar.set(flagDayOfWeek, week)
        }

        is Alarm.RepeatType.Date -> {
            calendar.timeInMillis = repeatType.targetDate.time
        }
    }

    calendar.set(flagHour, hour)
    calendar.set(flagMinute, minute)
    calendar.set(Calendar.SECOND, 0)

    return calendar
}

fun Alarm.getDisplayTime(): String = "%02d:%02d".format(hour, minute)

fun Alarm.updateDate(): Alarm = copy(lastUpdated = Calendar.getInstance())

fun List<Alarm>.pickNearestTime(nowTime: Calendar): Pair<Alarm, Calendar>? = associateWith {
    it.toCalendar(nowTime)
}
    .minByOrNull { (_, calendar) -> calendar.timeInMillis }
    ?.toPair()