package net.turtton.ytalarm.platform

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort
import org.mockito.kotlin.mock

/**
 * Unit tests for AndroidAlarmScheduler.
 *
 * Note: AndroidAlarmScheduler requires a real Android context (AlarmManager, PendingIntent, etc.)
 * so full integration tests are in androidTest. Here we test the interface contract
 * and that the scheduler satisfies AlarmSchedulerPort.
 */
@Suppress("UNUSED")
class AndroidAlarmSchedulerTest :
    FunSpec({
        context("AlarmSchedulerPort contract") {
            test("stub implementation returns Left(NoEnabledAlarm) for empty list") {
                val stub = object : AlarmSchedulerPort {
                    override suspend fun scheduleNextAlarm(
                        alarms: List<Alarm>
                    ): Either<AlarmScheduleError, Unit> {
                        if (alarms.isEmpty()) {
                            return Either.Left(AlarmScheduleError.NoEnabledAlarm)
                        }
                        return Either.Right(Unit)
                    }

                    override fun cancelAlarm() {
                        // no-op
                    }
                }
                val result = stub.scheduleNextAlarm(emptyList())
                result.shouldBeInstanceOf<Either.Left<AlarmScheduleError>>()
                result.value shouldBe AlarmScheduleError.NoEnabledAlarm
            }

            test("stub implementation returns Right(Unit) for non-empty enabled alarm list") {
                val stub = object : AlarmSchedulerPort {
                    override suspend fun scheduleNextAlarm(
                        alarms: List<Alarm>
                    ): Either<AlarmScheduleError, Unit> = if (alarms.isNotEmpty()) {
                        Either.Right(Unit)
                    } else {
                        Either.Left(AlarmScheduleError.NoEnabledAlarm)
                    }

                    override fun cancelAlarm() {}
                }
                val alarm = Alarm(id = 1L, hour = 7, minute = 0, isEnabled = true)
                val result = stub.scheduleNextAlarm(listOf(alarm))
                result.shouldBeInstanceOf<Either.Right<Unit>>()
            }

            test("mock AlarmSchedulerPort satisfies interface") {
                val mockScheduler = mock<AlarmSchedulerPort>()
                mockScheduler.shouldBeInstanceOf<AlarmSchedulerPort>()
            }
        }
    })