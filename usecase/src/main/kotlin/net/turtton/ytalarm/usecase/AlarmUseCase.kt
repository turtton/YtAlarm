package net.turtton.ytalarm.usecase

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.turtton.ytalarm.kernel.di.DependsOnAlarmRepository
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort

/**
 * アラームに関するビジネスロジックを定義するUseCaseインターフェース。
 *
 * @param LExec ローカルデータソースのExecutor型
 * @param LDS DependsOnAlarmRepositoryおよびDependsOnDataSourceを実装したローカルデータソース型
 */
interface AlarmUseCase<LExec, LDS>
    where LDS : DependsOnAlarmRepository<LExec>,
          LDS : DependsOnDataSource<LExec> {
    val localDataSource: LDS
    val alarmScheduler: AlarmSchedulerPort

    /**
     * 全アラームをFlowとして返す。
     * ViewModelのLiveDataに変換するために使用する。
     */
    fun getAllAlarmsFlow(): Flow<List<Alarm>> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.getAll(executor)
    }

    /**
     * 全アラームを同期的に返す。
     */
    suspend fun getAllAlarmsSync(): List<Alarm> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.getAllSync(executor)
    }

    /**
     * IDでアラームを取得する。
     */
    suspend fun getAlarmById(id: Long): Alarm? {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.getFromId(executor, id)
    }

    /**
     * repeatTypeに一致するアラームを取得する。
     */
    suspend fun getMatchedAlarms(repeatType: Alarm.RepeatType): List<Alarm> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.getMatched(executor, repeatType)
    }

    /**
     * アラームを挿入してIDを返す。
     */
    suspend fun insertAlarm(alarm: Alarm): Long {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.insert(executor, alarm)
    }

    /**
     * アラームを更新する。
     * lastUpdatedを自動的に現在時刻に更新する。
     */
    suspend fun updateAlarm(alarm: Alarm) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.alarmRepository.update(
            executor,
            alarm.copy(lastUpdated = Clock.System.now())
        )
    }

    /**
     * アラームを削除する。
     */
    suspend fun deleteAlarm(alarm: Alarm) {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.alarmRepository.delete(executor, alarm)
    }

    /**
     * アラーム発火後の状態遷移を処理する。
     * - Once/Date型: isEnabled=falseに設定
     * - Everyday/Days型: そのまま更新（スケジュール維持）
     * - Snooze型: アラームを削除
     */
    suspend fun processAfterFiring(alarm: Alarm): Either<AlarmScheduleError, Unit> {
        val executor = localDataSource.dataSource.createExecutor()
        when (alarm.repeatType) {
            is Alarm.RepeatType.Once,
            is Alarm.RepeatType.Date -> {
                localDataSource.alarmRepository.update(
                    executor,
                    alarm.copy(
                        repeatType = Alarm.RepeatType.Once,
                        isEnabled = false,
                        lastUpdated = Clock.System.now()
                    )
                )
            }

            is Alarm.RepeatType.Everyday,
            is Alarm.RepeatType.Days -> {
                localDataSource.alarmRepository.update(
                    executor,
                    alarm.copy(lastUpdated = Clock.System.now())
                )
            }

            is Alarm.RepeatType.Snooze -> {
                localDataSource.alarmRepository.delete(executor, alarm)
            }
        }

        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        return alarmScheduler.scheduleNextAlarm(allAlarms)
    }

    /**
     * スヌーズアラームを作成する。
     * 既存のスヌーズアラームを全て削除してから、新しいスヌーズアラームを作成する。
     *
     * @param originalAlarm 元のアラーム
     * @param clock 時刻取得用のClock（テスト可能にするためパラメータ化）
     * @return 作成されたスヌーズアラーム
     */
    suspend fun createSnoozeAlarm(
        originalAlarm: Alarm,
        clock: Clock = Clock.System
    ): Either<AlarmScheduleError, Alarm> {
        val executor = localDataSource.dataSource.createExecutor()

        // 既存のスヌーズアラームを削除
        val existingSnoozes =
            localDataSource.alarmRepository.getMatched(executor, Alarm.RepeatType.Snooze)
        existingSnoozes.forEach { localDataSource.alarmRepository.delete(executor, it) }

        // 現在時刻にsnoozeMinuteを加算してスヌーズ時刻を計算
        val tz = TimeZone.currentSystemDefault()
        val now = clock.now()
        val snoozeTime = now.plus(originalAlarm.snoozeMinute, DateTimeUnit.MINUTE, tz)
        val localSnoozeTime = snoozeTime.toLocalDateTime(tz)

        val snoozeAlarm = originalAlarm.copy(
            id = 0L,
            hour = localSnoozeTime.hour,
            minute = localSnoozeTime.minute,
            repeatType = Alarm.RepeatType.Snooze,
            isEnabled = true,
            creationDate = now,
            lastUpdated = now
        )
        val newId = localDataSource.alarmRepository.insert(executor, snoozeAlarm)
        val result = snoozeAlarm.copy(id = newId)

        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        return alarmScheduler.scheduleNextAlarm(allAlarms).map { result }
    }

    /**
     * アラームのON/OFFを切り替える。
     * 有効化・無効化どちらの場合もスケジュールを再設定する。
     *
     * @param alarmId 対象アラームのID
     * @param enabled 有効にする場合はtrue
     */
    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean): Either<AlarmScheduleError, Unit> {
        val executor = localDataSource.dataSource.createExecutor()
        val alarm = localDataSource.alarmRepository.getFromId(executor, alarmId)
            ?: return Either.Right(Unit)
        val updated = alarm.copy(isEnabled = enabled, lastUpdated = Clock.System.now())
        localDataSource.alarmRepository.update(executor, updated)
        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        return alarmScheduler.scheduleNextAlarm(allAlarms).onLeft {
            // スケジュール失敗時はDB状態をロールバック
            localDataSource.alarmRepository.update(executor, alarm)
        }
    }

    /**
     * アラームを保存してスケジュールを更新する。
     * id=0Lの場合は新規挿入、それ以外は更新。
     *
     * @param alarm 保存するアラーム
     */
    suspend fun saveAlarmAndSchedule(alarm: Alarm): Either<AlarmScheduleError, Unit> {
        val executor = localDataSource.dataSource.createExecutor()
        val alarmWithTimestamp = alarm.copy(lastUpdated = Clock.System.now())
        val isNew = alarmWithTimestamp.id == 0L
        val insertedId = if (isNew) {
            localDataSource.alarmRepository.insert(executor, alarmWithTimestamp)
        } else {
            localDataSource.alarmRepository.update(executor, alarmWithTimestamp)
            alarmWithTimestamp.id
        }
        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        return alarmScheduler.scheduleNextAlarm(allAlarms).onLeft {
            // スケジュール失敗時はDB変更をロールバック
            if (isNew) {
                val inserted = alarmWithTimestamp.copy(id = insertedId)
                localDataSource.alarmRepository.delete(executor, inserted)
            } else {
                localDataSource.alarmRepository.update(executor, alarm)
            }
        }
    }

    /**
     * アラームを削除してスケジュールを再設定する。
     *
     * @param alarm 削除するアラーム
     */
    suspend fun deleteAlarmAndReschedule(alarm: Alarm): Either<AlarmScheduleError, Unit> {
        val executor = localDataSource.dataSource.createExecutor()
        localDataSource.alarmRepository.delete(executor, alarm)
        val allAlarms = localDataSource.alarmRepository.getAllSync(executor)
        return alarmScheduler.scheduleNextAlarm(allAlarms)
    }

    /**
     * 有効なアラーム一覧を返す。
     *
     * @return isEnabled=trueのアラームリスト
     */
    suspend fun getEnabledAlarms(): List<Alarm> {
        val executor = localDataSource.dataSource.createExecutor()
        return localDataSource.alarmRepository.getAllSync(executor).filter { it.isEnabled }
    }
}