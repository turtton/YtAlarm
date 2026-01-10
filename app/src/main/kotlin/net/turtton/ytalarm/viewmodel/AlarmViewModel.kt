package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.util.extensions.hourOfDay
import net.turtton.ytalarm.util.extensions.minute
import net.turtton.ytalarm.util.extensions.plusAssign
import net.turtton.ytalarm.util.extensions.updateDate
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

class AlarmViewModel(private val repository: DataRepository) : ViewModel() {
    val allAlarms: LiveData<List<Alarm>> by lazy { repository.allAlarms.asLiveData() }

    fun getAllAlarmsAsync(): Deferred<List<Alarm>> = viewModelScope.async {
        repository.getAllAlarmsSync()
    }

    fun getFromIdAsync(id: Long): Deferred<Alarm?> = viewModelScope.async {
        repository.getAlarmFromIdSync(id)
    }

    fun getMatchedAsync(repeatType: Alarm.RepeatType) = viewModelScope.async {
        repository.getMatchedAlarmSync(repeatType)
    }

    suspend fun insert(alarm: Alarm): Long = repository.insert(alarm)

    suspend fun update(alarm: Alarm) {
        repository.update(alarm.updateDate())
    }

    /**
     * アラームを削除する（非同期）
     *
     * UI層からの呼び出し用。ViewModelScopeで新しいコルーチンを起動する。
     * suspend関数内からの呼び出しには[deleteSync]を使用すること。
     */
    fun delete(alarm: Alarm) = viewModelScope.launch {
        repository.delete(alarm)
    }

    /**
     * アラームを削除する（suspend）
     *
     * 呼び出し元のコルーチンスコープ内で処理完了まで待機する。
     * suspend関数内からの呼び出し用。
     */
    suspend fun deleteSync(alarm: Alarm) {
        repository.delete(alarm)
    }

    /**
     * アラーム発火後の状態遷移を処理する
     *
     * - Once/Date型: isEnable=falseに設定
     * - Everyday/Days型: そのまま更新
     * - Snooze型: アラームを削除
     */
    suspend fun processAfterFiring(alarm: Alarm) {
        var repeatType = alarm.repeatType
        if (repeatType is Alarm.RepeatType.Date) {
            repeatType = Alarm.RepeatType.Once
        }

        when (repeatType) {
            is Alarm.RepeatType.Once -> {
                update(alarm.copy(repeatType = repeatType, isEnable = false))
            }

            is Alarm.RepeatType.Everyday, is Alarm.RepeatType.Days -> {
                update(alarm)
            }

            is Alarm.RepeatType.Snooze -> {
                deleteSync(alarm)
            }

            else -> {}
        }
    }

    /**
     * スヌーズアラームを作成する
     *
     * 1. 既存のスヌーズアラームを全て削除
     * 2. 新しいスヌーズアラームを作成・保存
     *
     * @param originalAlarm 元のアラーム
     * @return 作成されたスヌーズアラーム
     */
    suspend fun createSnoozeAlarm(originalAlarm: Alarm): Alarm {
        // 既存のスヌーズアラームを削除
        val existingSnoozes = repository.getMatchedAlarmSync(Alarm.RepeatType.Snooze)
        existingSnoozes.forEach { deleteSync(it) }

        // 新しいスヌーズアラームを作成
        val now = Calendar.getInstance()
        now += originalAlarm.snoozeMinute.minutes
        val snoozeAlarm = originalAlarm.copy(
            id = 0,
            hour = now.hourOfDay,
            minute = now.minute,
            repeatType = Alarm.RepeatType.Snooze,
            isEnable = true
        )
        val newId = repository.insert(snoozeAlarm)
        return snoozeAlarm.copy(id = newId)
    }

    /**
     * 有効なアラームリストを取得する
     *
     * この関数はsuspend関数であり、呼び出し元のコルーチンスコープ内で
     * 処理完了まで待機する。Syncサフィックスは[getAllAlarmsAsync]のような
     * Deferred返却パターンとの区別のために使用している。
     *
     * @return 有効なアラームのリスト
     */
    suspend fun getEnabledAlarmsSync(): List<Alarm> = repository.getAllAlarmsSync().filter {
        it.isEnable
    }
}

class AlarmViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}