package net.turtton.ytalarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.receiver.AlarmReceiver
import net.turtton.ytalarm.util.extensions.pickNearestTime
import java.util.Calendar

private const val ALARM_REQUEST_CODE = 0
private const val SHOW_INTENT_REQUEST_CODE = 1

fun LiveData<List<Alarm>>.observeAlarm(lifecycleOwner: LifecycleOwner, context: Context) {
    observe(lifecycleOwner) { list ->
        if (list == null) return@observe
        val alarmList = list.filter { it.isEnable }.toMutableList()
        updateAlarmSchedule(context, alarmList)
    }
}

/**
 * アラームスケジュールを更新する
 *
 * BroadcastReceiver経由でNotification + fullScreenIntentを使用することで、
 * Android 14以降のBackground Activity Launch制限を回避する。
 */
fun updateAlarmSchedule(context: Context, alarmList: List<Alarm>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val nowTime = Calendar.getInstance()
    val (alarm, calendar) = alarmList.pickNearestTime(nowTime) ?: run {
        // 有効なアラームがない場合はキャンセル
        cancelAlarm(context, alarmManager)
        return
    }

    if (!alarm.isEnable) {
        cancelAlarm(context, alarmManager)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Log.e("RegisterAlarm", "Can't schedule exact alarms - permission not granted")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Error: Can't schedule exact alarms", Toast.LENGTH_SHORT).show()
        }
        return
    }

    // BroadcastReceiver経由でアラームを発動
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val targetTime = calendar.timeInMillis

    // showIntent: システムのアラーム一覧からタップした時に開くIntent
    // Deep Link経由でアラーム詳細画面を開く
    val showIntent = Intent(Intent.ACTION_VIEW, "ytalarm://alarm/${alarm.id}".toUri())
    val showPendingIntent = PendingIntent.getActivity(
        context,
        SHOW_INTENT_REQUEST_CODE,
        showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val clockInfo = AlarmManager.AlarmClockInfo(targetTime, showPendingIntent)

    alarmManager.setAlarmClock(clockInfo, pendingIntent)

    @Suppress("ktlint:standard:max-line-length")
    Log.d(
        "RegisterAlarm",
        "Id:${alarm.id},Year:${calendar[Calendar.YEAR]},Month:${calendar[Calendar.MONTH]},Day:${calendar[Calendar.DAY_OF_MONTH]},Hour:${calendar[Calendar.HOUR_OF_DAY]},Minute:${calendar[Calendar.MINUTE]}"
    )
}

/**
 * アラームをキャンセルする
 */
private fun cancelAlarm(context: Context, alarmManager: AlarmManager) {
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    Log.d("RegisterAlarm", "Alarm cancelled")
}