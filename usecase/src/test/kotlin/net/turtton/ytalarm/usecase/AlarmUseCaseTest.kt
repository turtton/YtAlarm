package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import net.turtton.ytalarm.kernel.di.DataSource
import net.turtton.ytalarm.kernel.di.DependsOnAlarmRepository
import net.turtton.ytalarm.kernel.di.DependsOnDataSource
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort
import net.turtton.ytalarm.kernel.repository.AlarmRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AlarmUseCaseTest :
    FunSpec({
        // テスト用のUnitExecutorを使った実装
        lateinit var mockAlarmRepo: AlarmRepository<Unit>
        lateinit var mockScheduler: AlarmSchedulerPort
        lateinit var useCase: AlarmUseCase<Unit, TestAlarmLocalDS>

        beforeEach {
            mockAlarmRepo = mock()
            mockScheduler = mock()
            whenever(mockScheduler.scheduleNextAlarm(any())).thenReturn(Either.Right(Unit))

            val ds = TestAlarmLocalDS(mockAlarmRepo)
            useCase = object : AlarmUseCase<Unit, TestAlarmLocalDS> {
                override val localDataSource: TestAlarmLocalDS = ds
                override val alarmScheduler: AlarmSchedulerPort = mockScheduler
            }
        }

        context("toggleAlarm") {
            test("disable alarm: update and reschedule") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                whenever(mockAlarmRepo.getFromId(Unit, 1L)).thenReturn(alarm)
                whenever(
                    mockAlarmRepo.getAllSync(Unit)
                ).thenReturn(listOf(alarm.copy(isEnabled = false)))

                useCase.toggleAlarm(1L, false)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe false
                verify(mockScheduler).scheduleNextAlarm(any())
            }

            test("enable alarm: update and reschedule") {
                val alarm = Alarm(id = 1L, isEnabled = false)
                whenever(mockAlarmRepo.getFromId(Unit, 1L)).thenReturn(alarm)
                whenever(
                    mockAlarmRepo.getAllSync(Unit)
                ).thenReturn(listOf(alarm.copy(isEnabled = true)))

                useCase.toggleAlarm(1L, true)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe true
                verify(mockScheduler).scheduleNextAlarm(any())
            }

            test("alarm not found: do nothing") {
                whenever(mockAlarmRepo.getFromId(Unit, 99L)).thenReturn(null)

                useCase.toggleAlarm(99L, true)

                verify(mockAlarmRepo, never()).update(any(), any())
                verify(mockScheduler, never()).scheduleNextAlarm(any())
            }

            test("disable last alarm: NoEnabledAlarm should not rollback") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                whenever(mockAlarmRepo.getFromId(Unit, 1L)).thenReturn(alarm)
                whenever(mockAlarmRepo.getAllSync(Unit))
                    .thenReturn(listOf(alarm.copy(isEnabled = false)))
                whenever(mockScheduler.scheduleNextAlarm(any()))
                    .thenReturn(Either.Left(AlarmScheduleError.NoEnabledAlarm))

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Left<AlarmScheduleError.NoEnabledAlarm>>()
                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo, times(1)).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe false
            }

            test("schedule error other than NoEnabledAlarm: should rollback") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                whenever(mockAlarmRepo.getFromId(Unit, 1L)).thenReturn(alarm)
                whenever(mockAlarmRepo.getAllSync(Unit))
                    .thenReturn(listOf(alarm.copy(isEnabled = false)))
                whenever(mockScheduler.scheduleNextAlarm(any()))
                    .thenReturn(Either.Left(AlarmScheduleError.PermissionDenied))

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Left<AlarmScheduleError.PermissionDenied>>()
                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo, times(2)).update(any(), captor.capture())
                captor.allValues[0].isEnabled shouldBe false
                captor.allValues[1] shouldBe alarm
            }

            test("disable one of multiple alarms: others remain enabled") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                val otherAlarm = Alarm(id = 2L, isEnabled = true)
                whenever(mockAlarmRepo.getFromId(Unit, 1L)).thenReturn(alarm)
                whenever(mockAlarmRepo.getAllSync(Unit))
                    .thenReturn(listOf(alarm.copy(isEnabled = false), otherAlarm))

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Right<Unit>>()
                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo, times(1)).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe false
                verify(mockScheduler).scheduleNextAlarm(any())
            }
        }

        context("processAfterFiring") {
            test("Once: disable alarm") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Once, isEnabled = true)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                useCase.processAfterFiring(alarm)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe false
            }

            test("Everyday: keep enabled") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Everyday, isEnabled = true)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(alarm))

                useCase.processAfterFiring(alarm)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe true
            }

            test("Snooze: delete alarm") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Snooze, isEnabled = true)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                useCase.processAfterFiring(alarm)

                verify(mockAlarmRepo).delete(any(), any())
                verify(mockAlarmRepo, never()).update(any(), any())
            }

            test("Date: convert to Once and disable") {
                val targetDate = LocalDate(2030, 1, 1)
                val alarm = Alarm(
                    id = 1L,
                    repeatType = Alarm.RepeatType.Date(targetDate),
                    isEnabled = true
                )
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                useCase.processAfterFiring(alarm)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe false
                captor.firstValue.repeatType shouldBe Alarm.RepeatType.Once
            }

            test("Days: keep enabled") {
                val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                val alarm = Alarm(
                    id = 1L,
                    repeatType = Alarm.RepeatType.Days(days),
                    isEnabled = true
                )
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(alarm))

                useCase.processAfterFiring(alarm)

                val captor = argumentCaptor<Alarm>()
                verify(mockAlarmRepo).update(any(), captor.capture())
                captor.firstValue.isEnabled shouldBe true
                captor.firstValue.repeatType shouldBe Alarm.RepeatType.Days(days)
            }
        }

        context("createSnoozeAlarm") {
            test("creates snooze alarm with snoozeMinute offset") {
                val originalAlarm = Alarm(
                    id = 1L,
                    hour = 7,
                    minute = 0,
                    snoozeMinute = 10,
                    isEnabled = true
                )
                // 既存スヌーズなし
                whenever(mockAlarmRepo.getMatched(Unit, Alarm.RepeatType.Snooze))
                    .thenReturn(emptyList())
                whenever(mockAlarmRepo.insert(any(), any())).thenReturn(100L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                val either = useCase.createSnoozeAlarm(originalAlarm)

                either.shouldBeInstanceOf<Either.Right<Alarm>>()
                val result = either.value
                result.repeatType shouldBe Alarm.RepeatType.Snooze
                result.isEnabled shouldBe true
                result.id shouldBe 100L
            }

            test("deletes existing snooze alarms before creating new one") {
                val existingSnooze = Alarm(id = 99L, repeatType = Alarm.RepeatType.Snooze)
                whenever(mockAlarmRepo.getMatched(Unit, Alarm.RepeatType.Snooze))
                    .thenReturn(listOf(existingSnooze))
                whenever(mockAlarmRepo.insert(any(), any())).thenReturn(100L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                val originalAlarm = Alarm(id = 1L, snoozeMinute = 5)
                useCase.createSnoozeAlarm(originalAlarm)

                verify(mockAlarmRepo).delete(any(), any())
            }
        }

        context("saveAlarmAndSchedule") {
            test("inserts new alarm and schedules") {
                val alarm = Alarm(id = 0L)
                whenever(mockAlarmRepo.insert(any(), any())).thenReturn(1L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(alarm.copy(id = 1L)))

                useCase.saveAlarmAndSchedule(alarm)

                verify(mockAlarmRepo).insert(any(), any())
                verify(mockScheduler).scheduleNextAlarm(any())
            }

            test("updates existing alarm and schedules") {
                val alarm = Alarm(id = 5L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(alarm))

                useCase.saveAlarmAndSchedule(alarm)

                verify(mockAlarmRepo).update(any(), any())
                verify(mockScheduler).scheduleNextAlarm(any())
            }
        }

        context("deleteAlarmAndReschedule") {
            test("deletes alarm and reschedules") {
                val alarm = Alarm(id = 1L)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(emptyList())

                useCase.deleteAlarmAndReschedule(alarm)

                verify(mockAlarmRepo).delete(any(), any())
                verify(mockScheduler).scheduleNextAlarm(any())
            }
        }

        context("getEnabledAlarms") {
            test("returns only enabled alarms") {
                val enabled = Alarm(id = 1L, isEnabled = true)
                val disabled = Alarm(id = 2L, isEnabled = false)
                whenever(mockAlarmRepo.getAllSync(Unit)).thenReturn(listOf(enabled, disabled))

                val result = useCase.getEnabledAlarms()

                result shouldHaveSize 1
                result.first().id shouldBe 1L
            }
        }
    })

// テスト用のLocalDataSource実装
class TestAlarmLocalDS(override val alarmRepository: AlarmRepository<Unit>) :
    DependsOnAlarmRepository<Unit>,
    DependsOnDataSource<Unit> {
    override val dataSource: DataSource<Unit> = object : DataSource<Unit> {
        override fun createExecutor(): Unit = Unit
    }
}