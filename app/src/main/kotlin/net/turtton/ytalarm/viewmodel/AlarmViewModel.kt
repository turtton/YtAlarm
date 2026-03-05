package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.usecase.UseCaseContainer
import net.turtton.ytalarm.util.extensions.toDomain
import net.turtton.ytalarm.util.extensions.toLegacy
import net.turtton.ytalarm.util.extensions.updateDate

class AlarmViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allAlarms: LiveData<List<Alarm>> by lazy {
        useCaseContainer.getAllAlarmsFlow()
            .map { list -> list.map { it.toLegacy() } }
            .asLiveData()
    }

    fun getAllAlarmsAsync(): Deferred<List<Alarm>> = viewModelScope.async {
        useCaseContainer.getAllAlarmsSync().map { it.toLegacy() }
    }

    fun getFromIdAsync(id: Long): Deferred<Alarm?> = viewModelScope.async {
        useCaseContainer.getAlarmById(id)?.toLegacy()
    }

    fun getMatchedAsync(repeatType: Alarm.RepeatType) = viewModelScope.async {
        useCaseContainer.getMatchedAlarms(repeatType.toDomain()).map { it.toLegacy() }
    }

    suspend fun insert(alarm: Alarm): Long = useCaseContainer.insertAlarm(alarm.toDomain())

    suspend fun update(alarm: Alarm) {
        useCaseContainer.updateAlarm(alarm.updateDate().toDomain())
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
        useCaseContainer.deleteAlarm(alarm.toDomain())
    }

    /**
     * アラーム発火後の状態遷移を処理する
     *
     * - Once/Date型: isEnable=falseに設定
     * - Everyday/Days型: そのまま更新
     * - Snooze型: アラームを削除
     */
    suspend fun processAfterFiring(alarm: Alarm) {
        useCaseContainer.processAfterFiring(alarm.toDomain())
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
    suspend fun createSnoozeAlarm(originalAlarm: Alarm): Alarm =
        useCaseContainer.createSnoozeAlarm(originalAlarm.toDomain()).toLegacy()

    /**
     * 有効なアラームリストを取得する
     *
     * この関数はsuspend関数であり、呼び出し元のコルーチンスコープ内で
     * 処理完了まで待機する。Syncサフィックスは[getAllAlarmsAsync]のような
     * Deferred返却パターンとの区別のために使用している。
     *
     * @return 有効なアラームのリスト
     */
    suspend fun getEnabledAlarmsSync(): List<Alarm> = useCaseContainer.getEnabledAlarms().map {
        it.toLegacy()
    }
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