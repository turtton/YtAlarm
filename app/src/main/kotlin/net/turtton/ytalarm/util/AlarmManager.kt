package net.turtton.ytalarm.util

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import net.turtton.ytalarm.AlarmActivity
import net.turtton.ytalarm.structure.Alarm
import java.util.Calendar

fun updateAlarm(context: Activity, data: Alarm, isAdder: Boolean) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    if (alarmManager != null) {
        val intent = Intent(context, AlarmActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(AlarmActivity.EXTRA_ALARM_ID, data.id!!)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance()
        val nowDay = calendar[Calendar.DAY_OF_WEEK]

        val (hour, minute) = data.time.split(':').map { it.toInt() }

        val nowHour = calendar[Calendar.HOUR_OF_DAY]
        val nowMinute = calendar[Calendar.MINUTE]

        val isPrevious = nowHour > hour || (nowHour == hour && nowMinute > minute)

        when (val repeatType = data.repeatType) {
            is RepeatType.Once, is RepeatType.Everyday -> {
                if (isPrevious) {
                    calendar.add(Calendar.DATE, 1)
                }
            }
            is RepeatType.Days -> {
                val days = repeatType.days.map { it.convertCalenderCode() }.toMutableList()
                days.filter { if (isPrevious) nowDay < it else nowDay <= it }
                    .minByOrNull { it - nowDay }
                    ?.also {
                        calendar.set(Calendar.DAY_OF_WEEK, it)
                    } ?: run { days.filter { if (isPrevious) it <= nowDay else it < nowDay } }
                    .minByOrNull { it - nowDay }
                    ?.also {
                        calendar.set(Calendar.DAY_OF_WEEK, it)
                        calendar.add(Calendar.DATE, 7)
                    }
            }
            is RepeatType.Date -> {
                calendar.time = repeatType.targetDate
            }
        }

        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.SECOND, 0)

        val clockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
        if (isAdder) {
            @Suppress("kitlint:max_line_length")
            Log.d(
                "RegisterAlarm",
                "Year: ${calendar[Calendar.YEAR]},Month:${calendar[Calendar.MONTH]},Day:${calendar[Calendar.DAY_OF_MONTH]},Hour:${calendar[Calendar.HOUR_OF_DAY]},Minute:${calendar[Calendar.MINUTE]}"
            )
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }
}