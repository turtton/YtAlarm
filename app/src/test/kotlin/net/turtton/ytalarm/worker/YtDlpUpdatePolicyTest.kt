package net.turtton.ytalarm.worker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import net.turtton.ytalarm.kernel.entity.Alarm
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class YtDlpUpdatePolicyTest :
    FunSpec({
        val now = Instant.parse("2026-03-02T06:00:00Z")
        val zone = TimeZone.UTC
        fun defer(alarms: List<Alarm>, ringing: Boolean = false, scheduled: Instant? = null) =
            shouldDeferYtDlpUpdate(ringing, alarms, now, zone, scheduled)

        test("ringing always wins; no alarms permits updating") {
            defer(emptyList(), ringing = true) shouldBe true
            defer(emptyList()) shouldBe false
        }
        test("inclusive thirty-minute boundary and disabled alarms") {
            val alarm = Alarm(hour = 6, minute = 30, isEnabled = true)
            defer(listOf(alarm)) shouldBe true
            defer(listOf(alarm.copy(minute = 31))) shouldBe false
            defer(listOf(alarm.copy(isEnabled = false))) shouldBe false
        }
        test("exact firing minute is not moved to tomorrow") {
            defer(listOf(Alarm(hour = 6, isEnabled = true))) shouldBe true
        }
        test("once everyday snooze days and date are covered") {
            listOf(
                Alarm.RepeatType.Once,
                Alarm.RepeatType.Everyday,
                Alarm.RepeatType.Snooze,
                Alarm.RepeatType.Days(listOf(DayOfWeek.MONDAY)),
                Alarm.RepeatType.Date(LocalDate(2026, 3, 2))
            ).forEach { type ->
                defer(
                    listOf(Alarm(hour = 6, minute = 15, repeatType = type, isEnabled = true))
                ) shouldBe
                    true
            }
        }
        test("stale date does not hide a future alarm") {
            defer(
                listOf(
                    Alarm(
                        hour = 6,
                        repeatType = Alarm.RepeatType.Date(LocalDate(2026, 3, 1)),
                        isEnabled = true
                    ),
                    Alarm(hour = 6, minute = 10, isEnabled = true)
                )
            ) shouldBe true
        }
        test("actual OS time catches late scheduling changes") {
            defer(emptyList(), scheduled = now + 30.minutes) shouldBe true
            defer(emptyList(), scheduled = now + 30.minutes + 1.milliseconds) shouldBe false
        }
        test("midnight uses the next calendar day") {
            shouldDeferYtDlpUpdate(
                false,
                listOf(Alarm(hour = 0, minute = 5, isEnabled = true)),
                Instant.parse("2026-03-02T23:50:00Z"),
                zone
            ) shouldBe true
        }
    })