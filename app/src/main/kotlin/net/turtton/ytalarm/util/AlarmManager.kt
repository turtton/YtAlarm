package net.turtton.ytalarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import net.turtton.ytalarm.AlarmActivity
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.util.extensions.toCalendar
import java.util.*

fun LiveData<List<Alarm>>.observeAlarm(lifecycleOwner: LifecycleOwner, context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    observe(lifecycleOwner) { list ->
        if (alarmManager == null) return@observe
        val alarmList = list.filter { it.enable }.toMutableList()

        val intent = Intent(context, AlarmActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val alarm = alarmList.pickNearestTime(calendar) ?: kotlin.run {
            alarmManager.cancel(pendingIntent)
            return@observe
        }

        intent.putExtra(AlarmActivity.EXTRA_ALARM_ID, alarm.id!!)

        val targetTime = alarm.toCalendar(calendar).timeInMillis
        val clockInfo = AlarmManager.AlarmClockInfo(targetTime, null)

        alarmManager.setAlarmClock(clockInfo, pendingIntent)
        @Suppress("ktlint:max-line-length")
        Log.d(
            "RegisterAlarm",
            "Year: ${calendar[Calendar.YEAR]},Month:${calendar[Calendar.MONTH]},Day:${calendar[Calendar.DAY_OF_MONTH]},Hour:${calendar[Calendar.HOUR_OF_DAY]},Minute:${calendar[Calendar.MINUTE]}"
        )
    }
}

@VisibleForTesting
fun List<Alarm>.pickNearestTime(nowTime: Calendar): Alarm? {
    return associateWith { it.toCalendar(nowTime) }
        .minByOrNull { (_, calendar) -> calendar.timeInMillis }
        ?.key
}