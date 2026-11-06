package net.turtton.ytalarm.worker

import kotlinx.datetime.TimeZone
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.entity.toNextFireTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Includes the firing minute: toNextFireTime normally skips an alarm at exactly now. */
internal fun shouldDeferYtDlpUpdate(
    isRinging: Boolean,
    alarms: List<Alarm>,
    now: Instant,
    timeZone: TimeZone,
    scheduledAlarm: Instant? = null
): Boolean {
    if (isRinging) return true
    val earliest = now - 1.minutes
    val latest = now + 30.minutes
    if (scheduledAlarm != null && scheduledAlarm in earliest..latest) return true
    return alarms.asSequence().filter { it.isEnabled }.any {
        it.toNextFireTime(earliest, timeZone) in earliest..latest
    }
}