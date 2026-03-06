package net.turtton.ytalarm.kernel.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AlarmSchedulingTest :
    FunSpec({
        // 2022/7/1(Fri) 12:00 UTC
        fun makeNow(
            year: Int = 2022,
            month: Int = 7,
            dayOfMonth: Int = 1,
            hour: Int = 12,
            minute: Int = 0
        ) = LocalDateTime(year, month, dayOfMonth, hour, minute, 0)
            .toInstant(TimeZone.UTC)

        val tz = TimeZone.UTC
        val now = makeNow()

        context("toNextFireTime") {
            context("RepeatType.Once") {
                val alarm = Alarm(repeatType = Alarm.RepeatType.Once)

                test("near future: same day") {
                    val target = alarm.copy(hour = 12, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(hour = 12, minute = 30)
                    target shouldBe expected
                }

                test("previous time: next day") {
                    val target = alarm.copy(hour = 11, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(dayOfMonth = 2, hour = 11, minute = 30)
                    target shouldBe expected
                }

                test("same time: next day") {
                    val target = alarm.copy(hour = 12, minute = 0).toNextFireTime(now, tz)
                    val expected = makeNow(dayOfMonth = 2, hour = 12, minute = 0)
                    target shouldBe expected
                }
            }

            context("RepeatType.Everyday") {
                val alarm = Alarm(repeatType = Alarm.RepeatType.Everyday)

                test("near future: same day") {
                    val target = alarm.copy(hour = 12, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(hour = 12, minute = 30)
                    target shouldBe expected
                }

                test("previous time: next day") {
                    val target = alarm.copy(hour = 11, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(dayOfMonth = 2, hour = 11, minute = 30)
                    target shouldBe expected
                }
            }

            context("RepeatType.Snooze") {
                val alarm = Alarm(repeatType = Alarm.RepeatType.Snooze)

                test("near future: same day") {
                    val target = alarm.copy(hour = 12, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(hour = 12, minute = 30)
                    target shouldBe expected
                }

                test("previous time: next day") {
                    val target = alarm.copy(hour = 11, minute = 30).toNextFireTime(now, tz)
                    val expected = makeNow(dayOfMonth = 2, hour = 11, minute = 30)
                    target shouldBe expected
                }
            }

            context("RepeatType.Days") {
                test("near future in same day of week (Friday)") {
                    val repeatType =
                        Alarm.RepeatType.Days(listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
                    val alarm = Alarm(hour = 12, minute = 30, repeatType = repeatType)
                    val target = alarm.toNextFireTime(now, tz)

                    // Same date: 2022/7/1 (Friday)
                    val expected = makeNow(hour = 12, minute = 30)
                    target shouldBe expected
                }

                test("previous time in same day of week (Friday): next week") {
                    val repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.FRIDAY))
                    val alarm = Alarm(hour = 11, minute = 30, repeatType = repeatType)
                    val target = alarm.toNextFireTime(now, tz)

                    // 7 days later: 2022/7/8 (Friday)
                    val expected = makeNow(dayOfMonth = 8, hour = 11, minute = 30)
                    target shouldBe expected
                }

                test("next day of week (Saturday)") {
                    val repeatType =
                        Alarm.RepeatType.Days(listOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY))
                    val alarm = Alarm(hour = 11, minute = 30, repeatType = repeatType)
                    val target = alarm.toNextFireTime(now, tz)

                    // 1 day later: 2022/7/2 (Saturday)
                    val expected = makeNow(dayOfMonth = 2, hour = 11, minute = 30)
                    target shouldBe expected
                }

                test("next week (Monday)") {
                    val repeatType =
                        Alarm.RepeatType.Days(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                    val alarm = Alarm(hour = 11, minute = 30, repeatType = repeatType)
                    val target = alarm.toNextFireTime(now, tz)

                    // 3 days later: 2022/7/4 (Monday)
                    val expected = makeNow(dayOfMonth = 4, hour = 11, minute = 30)
                    target shouldBe expected
                }
            }

            context("RepeatType.Date") {
                test("specify date") {
                    val targetDate = LocalDate(2022, 12, 15)
                    val alarm = Alarm(
                        hour = 12,
                        minute = 30,
                        repeatType = Alarm.RepeatType.Date(targetDate)
                    )
                    val target = alarm.toNextFireTime(now, tz)

                    val expected = LocalDateTime(2022, 12, 15, 12, 30, 0).toInstant(tz)
                    target shouldBe expected
                }
            }
        }

        context("pickNearestTime") {
            val seven = Alarm(hour = 7)
            val sevenHalf = Alarm(hour = 7, minute = 30)
            val eleven = Alarm(hour = 11)
            val twelve = Alarm(hour = 12)
            val fifteen = Alarm(hour = 15)
            val nineteen = Alarm(hour = 19)

            test("exclude same time (returns next future time)") {
                val target = listOf(twelve, fifteen, nineteen)
                // now is 12:00, so twelve (12:00) is same time -> goes to next day
                // fifteen (15:00) and nineteen (19:00) are same day
                // nearest future is fifteen at 15:00
                target.pickNearestTime(now, tz)?.first shouldBe fifteen
            }

            test("next day (all alarms are before/at now time)") {
                val target = listOf(seven, sevenHalf, eleven)
                // now is 12:00, all alarms are before 12:00 -> all go to next day
                // nearest is seven (7:00 next day)
                target.pickNearestTime(now, tz)?.first shouldBe seven
            }

            test("empty list returns null") {
                emptyList<Alarm>().pickNearestTime(now, tz) shouldBe null
            }
        }

        context("RepeatType.Days wrap-around from Sunday") {
            // now is 2022/7/1 (Fri) 12:00 UTC
            // Use a Sunday scenario: 2022/7/3 (Sun) 12:00 UTC
            val sunday = makeNow(dayOfMonth = 3)

            test("Sunday alarm with only MONDAY: fires next day (Monday)") {
                val repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.MONDAY))
                val alarm = Alarm(hour = 8, minute = 0, repeatType = repeatType)
                val target = alarm.toNextFireTime(sunday, tz)
                // 2022/7/4 (Monday) 08:00
                val expected = makeNow(dayOfMonth = 4, hour = 8, minute = 0)
                target shouldBe expected
            }

            test("Sunday alarm with SUNDAY in past: wraps to next Sunday") {
                val repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.SUNDAY))
                val alarm = Alarm(hour = 8, minute = 0, repeatType = repeatType)
                val target = alarm.toNextFireTime(sunday, tz)
                // 2022/7/10 (next Sunday) 08:00 — alarm time 08:00 is before 12:00 so it passed
                val expected = makeNow(dayOfMonth = 10, hour = 8, minute = 0)
                target shouldBe expected
            }

            test("Sunday alarm with SUNDAY in future: fires same day") {
                val repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.SUNDAY))
                val alarm = Alarm(hour = 15, minute = 0, repeatType = repeatType)
                val target = alarm.toNextFireTime(sunday, tz)
                // 2022/7/3 (Sunday) 15:00
                val expected = makeNow(dayOfMonth = 3, hour = 15, minute = 0)
                target shouldBe expected
            }
        }

        context("getNearestDayOfWeekOrNull") {
            test("contains same day of week") {
                // now is Friday (DayOfWeek.FRIDAY = 5)
                val days = listOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                days.getNearestDayOfWeekOrNull(DayOfWeek.WEDNESDAY) shouldBe DayOfWeek.WEDNESDAY
            }

            test("same week (looking ahead)") {
                val days = listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                // from SUNDAY, nearest in same week is WEDNESDAY
                days.getNearestDayOfWeekOrNull(DayOfWeek.SUNDAY) shouldBe DayOfWeek.WEDNESDAY
            }

            test("next week (wrap around)") {
                val days = listOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
                // from SATURDAY, no days later this week -> wraps to next week SUNDAY
                days.getNearestDayOfWeekOrNull(DayOfWeek.SATURDAY) shouldBe DayOfWeek.SUNDAY
            }
        }

        context("isSameDate") {
            test("same date") {
                val a = makeNow()
                val b = makeNow()
                a.isSameDate(b, tz) shouldBe true
            }

            test("different day") {
                val a = makeNow()
                val b = makeNow(dayOfMonth = 5)
                a.isSameDate(b, tz) shouldBe false
            }

            test("different year") {
                val a = makeNow()
                val b = makeNow(year = 2021)
                a.isSameDate(b, tz) shouldBe false
            }
        }

        context("isPrevOrSameTime (time-of-day comparison)") {
            test("previous hour: alarm hour < now hour") {
                // now=12:00, alarm=11:00 -> alarm is in past
                now.isPrevOrSameTime(11, 0, tz) shouldBe true
            }
            test("following hour: alarm hour > now hour") {
                // now=12:00, alarm=13:00 -> alarm is in future
                now.isPrevOrSameTime(13, 0, tz) shouldBe false
            }
            test("same hour, previous minute") {
                val nowAt1230 = makeNow(hour = 12, minute = 30)
                nowAt1230.isPrevOrSameTime(12, 0, tz) shouldBe true
            }
            test("same hour, same minute") {
                val nowAt1230 = makeNow(hour = 12, minute = 30)
                nowAt1230.isPrevOrSameTime(12, 30, tz) shouldBe true
            }
            test("same hour, future minute") {
                val nowAt1230 = makeNow(hour = 12, minute = 30)
                nowAt1230.isPrevOrSameTime(12, 50, tz) shouldBe false
            }
        }
    })