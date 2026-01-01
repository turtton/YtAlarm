package net.turtton.ytalarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import net.turtton.ytalarm.R
import net.turtton.ytalarm.activity.AlarmActivity
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.receiver.AlarmReceiver
import net.turtton.ytalarm.util.extensions.pickNearestTime
import java.util.Calendar

private const val ALARM_REQUEST_CODE = 0
private const val SHOW_INTENT_REQUEST_CODE = 1

/**
 * アラームスケジュール更新時のエラー
 */
sealed interface AlarmScheduleError {
    data object NoAlarmManager : AlarmScheduleError
    data class PermissionDenied(val message: String) : AlarmScheduleError
    data object NoEnabledAlarm : AlarmScheduleError
}

fun LiveData<List<Alarm>>.observeAlarm(lifecycleOwner: LifecycleOwner, context: Context) {
    observe(lifecycleOwner) { list ->
        if (list == null) return@observe
        val alarmList = list.filter { it.isEnable }.toMutableList()
        updateAlarmSchedule(context, alarmList).onLeft { error ->
            val message = when (error) {
                is AlarmScheduleError.PermissionDenied -> error.message

                AlarmScheduleError.NoAlarmManager ->
                    context.getString(R.string.error_no_alarm_manager)

                AlarmScheduleError.NoEnabledAlarm -> null
            }
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * アラームスケジュールを更新する
 *
 * BroadcastReceiver経由でNotification + fullScreenIntentを使用することで、
 * Android 14以降のBackground Activity Launch制限を回避する。
 *
 * @return 成功時はRight(Unit)、失敗時はLeft(AlarmScheduleError)
 */
fun updateAlarmSchedule(
    context: Context,
    alarmList: List<Alarm>
): Either<AlarmScheduleError, Unit> = either {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        ?: raise(AlarmScheduleError.NoAlarmManager)

    val nowTime = Calendar.getInstance()
    val (alarm, calendar) = alarmList.pickNearestTime(nowTime) ?: run {
        // 有効なアラームがない場合はキャンセル
        cancelAlarm(context, alarmManager)
        raise(AlarmScheduleError.NoEnabledAlarm)
    }

    ensure(alarm.isEnable) {
        cancelAlarm(context, alarmManager)
        AlarmScheduleError.NoEnabledAlarm
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ensure(alarmManager.canScheduleExactAlarms()) {
            Log.e("RegisterAlarm", "Can't schedule exact alarms - permission not granted")
            AlarmScheduleError.PermissionDenied(
                context.getString(R.string.error_schedule_exact_alarm)
            )
        }
    }

    // BroadcastReceiver経由でアラームを発動
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra(AlarmActivity.EXTRA_ALARM_ID, alarm.id)
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