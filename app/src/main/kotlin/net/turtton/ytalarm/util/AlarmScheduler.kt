package net.turtton.ytalarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import net.turtton.ytalarm.activity.AlarmActivity
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.util.extensions.compatPendingIntentFlag
import net.turtton.ytalarm.util.extensions.pickNearestTime
import java.util.*

fun LiveData<List<Alarm>>.observeAlarm(lifecycleOwner: LifecycleOwner, context: Context) {
    observe(lifecycleOwner) { list ->
        if (list == null) return@observe
        val alarmList = list.filter { it.isEnable }.toMutableList()
        updateAlarmSchedule(context, alarmList)
    }
}

fun updateAlarmSchedule(context: Context, alarmList: List<Alarm>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, AlarmActivity::class.java)

    val nowTime = Calendar.getInstance()
    val (alarm, calendar) = alarmList.pickNearestTime(nowTime) ?: run {
        Alarm(id = -1, isEnable = false) to nowTime
    }

    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(AlarmActivity.EXTRA_ALARM_ID, alarm.id)

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        compatPendingIntentFlag
    )

    if (!alarm.isEnable) {
        alarmManager.cancel(pendingIntent)
        return
    }

    val targetTime = calendar.timeInMillis
    val clockInfo = AlarmManager.AlarmClockInfo(targetTime, null)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // TODO create more user friendly process
        Toast.makeText(context, "Error: Can't schedule exact alarms", Toast.LENGTH_SHORT).show()
        return
    }

    alarmManager.setAlarmClock(clockInfo, pendingIntent)
    @Suppress("ktlint:standard:max-line-length")
    Log.d(
        "RegisterAlarm",
        "Id:${alarm.id},Year:${calendar[Calendar.YEAR]},Month:${calendar[Calendar.MONTH]},Day:${calendar[Calendar.DAY_OF_MONTH]},Hour:${calendar[Calendar.HOUR_OF_DAY]},Minute:${calendar[Calendar.MINUTE]}"
    )
}