package net.turtton.ytalarm.usecase

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.fake.FakeAlarmRepository
import net.turtton.ytalarm.kernel.fake.FakeAlarmSchedulerPort
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort
import net.turtton.ytalarm.usecase.fake.FakeLocalDataSourceContainer

class AlarmUseCaseTest :
    FunSpec({
        // テスト用のFake実装
        lateinit var fakeAlarmRepo: FakeAlarmRepository
        lateinit var fakeScheduler: FakeAlarmSchedulerPort
        lateinit var useCase: AlarmUseCase<Unit, FakeLocalDataSourceContainer>

        beforeEach {
            fakeAlarmRepo = FakeAlarmRepository()
            fakeScheduler = FakeAlarmSchedulerPort()
            val ds = FakeLocalDataSourceContainer(alarmRepository = fakeAlarmRepo)
            useCase = object : AlarmUseCase<Unit, FakeLocalDataSourceContainer> {
                override val localDataSource: FakeLocalDataSourceContainer = ds
                override val alarmScheduler: AlarmSchedulerPort = fakeScheduler
            }
        }

        context("toggleAlarm") {
            test("disable alarm: update and reschedule") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                fakeAlarmRepo.seed(alarm)

                useCase.toggleAlarm(1L, false)

                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe false
                fakeScheduler.scheduledCalls shouldHaveSize 1
            }

            test("enable alarm: update and reschedule") {
                val alarm = Alarm(id = 1L, isEnabled = false)
                fakeAlarmRepo.seed(alarm)

                useCase.toggleAlarm(1L, true)

                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe true
                fakeScheduler.scheduledCalls shouldHaveSize 1
            }

            test("alarm not found: do nothing") {
                useCase.toggleAlarm(99L, true)

                fakeAlarmRepo.currentData shouldBe emptyList()
                fakeScheduler.scheduledCalls shouldHaveSize 0
            }

            test("disable last alarm: NoEnabledAlarm should not rollback") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                fakeAlarmRepo.seed(alarm)
                fakeScheduler.scheduleResult = Either.Left(AlarmScheduleError.NoEnabledAlarm)

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Left<AlarmScheduleError.NoEnabledAlarm>>()
                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe false
            }

            test("schedule error other than NoEnabledAlarm: should rollback") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                fakeAlarmRepo.seed(alarm)
                fakeScheduler.scheduleResult = Either.Left(AlarmScheduleError.PermissionDenied)

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Left<AlarmScheduleError.PermissionDenied>>()
                fakeAlarmRepo.currentData.first { it.id == 1L } shouldBe alarm
            }

            test("disable one of multiple alarms: others remain enabled") {
                val alarm = Alarm(id = 1L, isEnabled = true)
                val otherAlarm = Alarm(id = 2L, isEnabled = true)
                fakeAlarmRepo.seed(alarm, otherAlarm)

                val result = useCase.toggleAlarm(1L, false)

                result.shouldBeInstanceOf<Either.Right<Unit>>()
                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe false
                fakeAlarmRepo.currentData.first { it.id == 2L }.isEnabled shouldBe true
                fakeScheduler.scheduledCalls shouldHaveSize 1
            }
        }

        context("processAfterFiring") {
            test("Once: disable alarm") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Once, isEnabled = true)
                fakeAlarmRepo.seed(alarm)

                useCase.processAfterFiring(alarm)

                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe false
            }

            test("Everyday: keep enabled") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Everyday, isEnabled = true)
                fakeAlarmRepo.seed(alarm)

                useCase.processAfterFiring(alarm)

                fakeAlarmRepo.currentData.first { it.id == 1L }.isEnabled shouldBe true
            }

            test("Snooze: delete alarm") {
                val alarm = Alarm(id = 1L, repeatType = Alarm.RepeatType.Snooze, isEnabled = true)
                fakeAlarmRepo.seed(alarm)

                useCase.processAfterFiring(alarm)

                fakeAlarmRepo.currentData.none { it.id == 1L } shouldBe true
            }

            test("Date: convert to Once and disable") {
                val targetDate = LocalDate(2030, 1, 1)
                val alarm = Alarm(
                    id = 1L,
                    repeatType = Alarm.RepeatType.Date(targetDate),
                    isEnabled = true
                )
                fakeAlarmRepo.seed(alarm)

                useCase.processAfterFiring(alarm)

                val updated = fakeAlarmRepo.currentData.first { it.id == 1L }
                updated.isEnabled shouldBe false
                updated.repeatType shouldBe Alarm.RepeatType.Once
            }

            test("Days: keep enabled") {
                val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                val alarm = Alarm(
                    id = 1L,
                    repeatType = Alarm.RepeatType.Days(days),
                    isEnabled = true
                )
                fakeAlarmRepo.seed(alarm)

                useCase.processAfterFiring(alarm)

                val updated = fakeAlarmRepo.currentData.first { it.id == 1L }
                updated.isEnabled shouldBe true
                updated.repeatType shouldBe Alarm.RepeatType.Days(days)
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
                fakeAlarmRepo.seed(originalAlarm)

                val either = useCase.createSnoozeAlarm(originalAlarm)

                either.shouldBeInstanceOf<Either.Right<Alarm>>()
                val result = either.value
                result.repeatType shouldBe Alarm.RepeatType.Snooze
                result.isEnabled shouldBe true
                result.id shouldBe 2L
            }

            test("deletes existing snooze alarms before creating new one") {
                val existingSnooze = Alarm(id = 99L, repeatType = Alarm.RepeatType.Snooze)
                val originalAlarm = Alarm(id = 1L, snoozeMinute = 5)
                fakeAlarmRepo.seed(existingSnooze, originalAlarm)

                useCase.createSnoozeAlarm(originalAlarm)

                fakeAlarmRepo.currentData.none { it.id == 99L } shouldBe true
            }
        }

        context("saveAlarmAndSchedule") {
            test("inserts new alarm and schedules") {
                val alarm = Alarm(id = 0L)

                useCase.saveAlarmAndSchedule(alarm)

                fakeAlarmRepo.currentData shouldHaveSize 1
                fakeScheduler.scheduledCalls shouldHaveSize 1
            }

            test("updates existing alarm and schedules") {
                val alarm = Alarm(id = 5L)
                fakeAlarmRepo.seed(alarm)

                useCase.saveAlarmAndSchedule(alarm)

                fakeScheduler.scheduledCalls shouldHaveSize 1
            }
        }

        context("deleteAlarmAndReschedule") {
            test("deletes alarm and reschedules") {
                val alarm = Alarm(id = 1L)
                fakeAlarmRepo.seed(alarm)

                useCase.deleteAlarmAndReschedule(alarm)

                fakeAlarmRepo.currentData.none { it.id == 1L } shouldBe true
                fakeScheduler.scheduledCalls shouldHaveSize 1
            }
        }

        context("getEnabledAlarms") {
            test("returns only enabled alarms") {
                val enabled = Alarm(id = 1L, isEnabled = true)
                val disabled = Alarm(id = 2L, isEnabled = false)
                fakeAlarmRepo.seed(enabled, disabled)

                val result = useCase.getEnabledAlarms()

                result shouldHaveSize 1
                result.first().id shouldBe 1L
            }
        }
    })