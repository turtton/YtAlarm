package net.turtton.ytalarm.kernel.port

import arrow.core.Either
import net.turtton.ytalarm.kernel.entity.Alarm

/**
 * アラームスケジュールのプラットフォームPortインターフェース。
 * Kernel層はこのインターフェースに依存し、実装はApp層（AndroidAlarmScheduler）で提供する。
 */
interface AlarmSchedulerPort {
    suspend fun scheduleNextAlarm(alarms: List<Alarm>): Either<AlarmScheduleError, Unit>

    /**
     * 現在スケジュールされているアラームをキャンセルする。
     *
     * この関数は suspend ではない。Android の [AlarmManager.cancel][android.app.AlarmManager.cancel]
     * が同期処理であり、コルーチンのサスペンドを必要としないためである。
     */
    fun cancelAlarm()
}

/**
 * アラームスケジュール更新時のエラー。
 */
sealed interface AlarmScheduleError {
    data object NoAlarmManager : AlarmScheduleError

    data object PermissionDenied : AlarmScheduleError

    data object NoEnabledAlarm : AlarmScheduleError
}