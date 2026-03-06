package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.usecase.UseCaseContainer

class AlarmViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allAlarms: LiveData<List<Alarm>> by lazy {
        useCaseContainer.getAllAlarmsFlow().asLiveData()
    }

    fun getAllAlarmsAsync(): Deferred<List<Alarm>> = viewModelScope.async {
        useCaseContainer.getAllAlarmsSync()
    }

    fun getFromIdAsync(id: Long): Deferred<Alarm?> = viewModelScope.async {
        useCaseContainer.getAlarmById(id)
    }

    fun getMatchedAsync(repeatType: Alarm.RepeatType) = viewModelScope.async {
        useCaseContainer.getMatchedAlarms(repeatType)
    }

    suspend fun insert(alarm: Alarm): Long = useCaseContainer.insertAlarm(alarm)

    suspend fun update(alarm: Alarm) {
        useCaseContainer.updateAlarm(alarm)
    }

    /**
     * アラームを削除する（非同期）
     *
     * UI層からの呼び出し用。ViewModelScopeで新しいコルーチンを起動する。
     * suspend関数内からの呼び出しには[deleteSync]を使用すること。
     */
    fun delete(alarm: Alarm) = viewModelScope.launch {
        deleteSync(alarm)
    }

    /**
     * アラームを削除する（suspend）
     *
     * 呼び出し元のコルーチンスコープ内で処理完了まで待機する。
     * suspend関数内からの呼び出し用。
     */
    suspend fun deleteSync(alarm: Alarm) {
        useCaseContainer.deleteAlarm(alarm)
    }

    suspend fun saveAlarmAndSchedule(alarm: Alarm): Either<AlarmScheduleError, Unit> =
        useCaseContainer.saveAlarmAndSchedule(alarm)

    suspend fun deleteAlarmAndReschedule(alarm: Alarm): Either<AlarmScheduleError, Unit> =
        useCaseContainer.deleteAlarmAndReschedule(alarm)

    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean): Either<AlarmScheduleError, Unit> =
        useCaseContainer.toggleAlarm(alarmId, enabled)

    suspend fun processAfterFiring(alarm: Alarm): Either<AlarmScheduleError, Unit> =
        useCaseContainer.processAfterFiring(alarm)

    suspend fun createSnoozeAlarm(originalAlarm: Alarm): Either<AlarmScheduleError, Alarm> =
        useCaseContainer.createSnoozeAlarm(originalAlarm)

    suspend fun getEnabledAlarmsSync(): List<Alarm> = useCaseContainer.getEnabledAlarms()
}

class AlarmViewModelFactory(private val useCaseContainer: UseCaseContainer<*, *, *, *>) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(useCaseContainer) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}