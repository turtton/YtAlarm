package net.turtton.ytalarm.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import net.turtton.ytalarm.activity.AlarmActivity
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.pickNearestTime
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort
import net.turtton.ytalarm.receiver.AlarmReceiver

private const val ALARM_REQUEST_CODE = 0
private const val SHOW_INTENT_REQUEST_CODE = 1
private const val LOG_TAG = "AndroidAlarmScheduler"

/**
 * Android platform implementation of [AlarmSchedulerPort].
 *
 * Wraps Android's [AlarmManager] to schedule and cancel alarms.
 * Uses [BroadcastReceiver] via [AlarmReceiver] to trigger the alarm notification,
 * which then launches [AlarmActivity].
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmSchedulerPort {

    /**
     * Schedules the next alarm from the given list.
     *
     * Finds the alarm with the nearest fire time using [pickNearestTime],
     * then registers it with the system [AlarmManager].
     *
     * @param alarms The list of enabled alarms to schedule from.
     * @return [Either.Right] on success, [Either.Left] with [AlarmScheduleError] on failure.
     */
    override suspend fun scheduleNextAlarm(alarms: List<Alarm>): Either<AlarmScheduleError, Unit> =
        either {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: raise(AlarmScheduleError.NoAlarmManager)

            val enabledAlarms = alarms.filter { it.isEnabled }
            val now = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()

            val (alarm, fireInstant) = enabledAlarms.pickNearestTime(now, timeZone) ?: run {
                cancelAlarmInternal(alarmManager)
                raise(AlarmScheduleError.NoEnabledAlarm)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ensure(alarmManager.canScheduleExactAlarms()) {
                    Log.e(LOG_TAG, "Can't schedule exact alarms - permission not granted")
                    AlarmScheduleError.PermissionDenied
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

            val targetTime = fireInstant.toEpochMilliseconds()

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

            Log.d(LOG_TAG, "Scheduled alarm: id=${alarm.id}, fireTime=$fireInstant")
        }

    /**
     * Cancels the currently scheduled alarm, if any.
     */
    override fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        cancelAlarmInternal(alarmManager)
    }

    private fun cancelAlarmInternal(alarmManager: AlarmManager) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d(LOG_TAG, "Alarm cancelled")
        }
    }
}