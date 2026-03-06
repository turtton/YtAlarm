package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import net.turtton.ytalarm.datasource.dao.AlarmDao
import net.turtton.ytalarm.datasource.entity.AlarmEntity
import net.turtton.ytalarm.datasource.repository.RoomAlarmRepository
import net.turtton.ytalarm.kernel.entity.Alarm
import java.util.Calendar

/**
 * AlarmDao のフェイク実装（テスト用）
 */
private class FakeAlarmDao(private val alarms: MutableList<AlarmEntity> = mutableListOf()) :
    AlarmDao {
    override fun getAll(): Flow<List<AlarmEntity>> = flowOf(alarms.toList())

    override suspend fun getAllSync(): List<AlarmEntity> = alarms.toList()

    override suspend fun getFromIdSync(id: Long): AlarmEntity? = alarms.find { it.id == id }

    override suspend fun getMatchedSync(repeatType: AlarmEntity.RepeatType): List<AlarmEntity> =
        alarms.filter { it.repeatType == repeatType }

    override suspend fun insert(alarm: AlarmEntity): Long {
        val id = (alarms.maxOfOrNull { it.id } ?: 0L) + 1
        alarms.add(alarm.copy(id = id))
        return id
    }

    override suspend fun update(alarm: AlarmEntity) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index >= 0) alarms[index] = alarm
    }

    override suspend fun delete(alarm: AlarmEntity) {
        alarms.removeAll { it.id == alarm.id }
    }

    override suspend fun deleteAll() {
        alarms.clear()
    }
}

/**
 * AppDatabase の代わりに使うフェイク Executor
 */
private class FakeAlarmExecutor(val dao: AlarmDao)

/**
 * テスト用 RoomAlarmRepository の実装
 */
private val repository = RoomAlarmRepository<FakeAlarmExecutor> { it.dao }

class RoomAlarmRepositoryTest :
    FunSpec({
        val calendar = Calendar.getInstance().apply { timeInMillis = 0L }

        fun makeExecutor(): FakeAlarmExecutor = FakeAlarmExecutor(FakeAlarmDao())

        test("getAll returns mapped domain alarms") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 1L,
                        hour = 7,
                        minute = 30,
                        repeatType = AlarmEntity.RepeatType.Once,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = true,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val result = repository.getAll(executor).first()
            result.size shouldBe 1
            result[0].id shouldBe 1L
            result[0].hour shouldBe 7
            result[0].minute shouldBe 30
            result[0].repeatType shouldBe Alarm.RepeatType.Once
        }

        test("getAllSync returns all alarms") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 1L,
                        hour = 6,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Everyday,
                        playListId = emptyList(),
                        shouldLoop = true,
                        shouldShuffle = false,
                        volume = 80,
                        snoozeMinute = 5,
                        shouldVibrate = false,
                        isEnabled = true,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val result = repository.getAllSync(executor)
            result.size shouldBe 1
            result[0].repeatType shouldBe Alarm.RepeatType.Everyday
        }

        test("getFromId returns alarm by id") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 42L,
                        hour = 8,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Once,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = false,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val result = repository.getFromId(executor, 42L)
            result?.id shouldBe 42L
        }

        test("getFromId returns null when not found") {
            val executor = FakeAlarmExecutor(FakeAlarmDao())
            val result = repository.getFromId(executor, 99L)
            result shouldBe null
        }

        test("getMatched returns alarms matching repeatType") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 1L,
                        hour = 7,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Once,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = false,
                        creationDate = calendar,
                        lastUpdated = calendar
                    ),
                    AlarmEntity(
                        id = 2L,
                        hour = 8,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Everyday,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = true,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val result = repository.getMatched(executor, Alarm.RepeatType.Everyday)
            result.size shouldBe 1
            result[0].repeatType shouldBe Alarm.RepeatType.Everyday
        }

        test("insert adds alarm and returns id") {
            val executor = makeExecutor()
            val alarm = Alarm(
                id = 0L,
                hour = 9,
                minute = 15,
                repeatType = Alarm.RepeatType.Once,
                playlistIds = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = Alarm.Volume(60),
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            val id = repository.insert(executor, alarm)
            id shouldBe 1L
        }

        test("update modifies existing alarm") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 1L,
                        hour = 7,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Once,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = false,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val updatedAlarm = Alarm(
                id = 1L,
                hour = 10,
                minute = 30,
                repeatType = Alarm.RepeatType.Everyday,
                playlistIds = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = Alarm.Volume(50),
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = true,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            repository.update(executor, updatedAlarm)
            val result = repository.getFromId(executor, 1L)
            result?.hour shouldBe 10
            result?.isEnabled shouldBe true
        }

        test("delete removes alarm") {
            val dao = FakeAlarmDao(
                mutableListOf(
                    AlarmEntity(
                        id = 1L,
                        hour = 7,
                        minute = 0,
                        repeatType = AlarmEntity.RepeatType.Once,
                        playListId = emptyList(),
                        shouldLoop = false,
                        shouldShuffle = false,
                        volume = 50,
                        snoozeMinute = 10,
                        shouldVibrate = true,
                        isEnabled = false,
                        creationDate = calendar,
                        lastUpdated = calendar
                    )
                )
            )
            val executor = FakeAlarmExecutor(dao)
            val alarm = Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatType = Alarm.RepeatType.Once,
                playlistIds = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = Alarm.Volume(50),
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            repository.delete(executor, alarm)
            val result = repository.getAllSync(executor)
            result.size shouldBe 0
        }
    })