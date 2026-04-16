package net.turtton.ytalarm.kernel.fake

import arrow.core.Either
import arrow.core.right
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.port.AlarmScheduleError
import net.turtton.ytalarm.kernel.port.AlarmSchedulerPort

class FakeAlarmSchedulerPort : AlarmSchedulerPort {
    var scheduleResult: Either<AlarmScheduleError, Unit> = Unit.right()
    val scheduledCalls: MutableList<List<Alarm>> = mutableListOf()
    var cancelCount = 0

    override suspend fun scheduleNextAlarm(alarms: List<Alarm>): Either<AlarmScheduleError, Unit> {
        scheduledCalls.add(alarms)
        return scheduleResult
    }

    override fun cancelAlarm() {
        cancelCount++
    }
}