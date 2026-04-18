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
            test("midnight boundary: now=00:00, alarm=00:00") {
                val midnight = makeNow(hour = 0, minute = 0)
                midnight.isPrevOrSameTime(0, 0, tz) shouldBe true
            }
            test("midnight boundary: now=00:00, alarm=00:01") {
                val midnight = makeNow(hour = 0, minute = 0)
                midnight.isPrevOrSameTime(0, 1, tz) shouldBe false
            }
            test("end of day: now=23:59, alarm=23:59") {
                val endOfDay = makeNow(hour = 23, minute = 59)
                endOfDay.isPrevOrSameTime(23, 59, tz) shouldBe true
            }
            test("end of day: now=23:59, alarm=0:00") {
                val endOfDay = makeNow(hour = 23, minute = 59)
                endOfDay.isPrevOrSameTime(0, 0, tz) shouldBe true
            }
        }

        context("toNextFireTime — midnight boundary (00:00)") {
            test("Once: alarm at 00:00, now=23:59 → next day") {
                val now2359 = makeNow(hour = 23, minute = 59)
                val alarm = Alarm(hour = 0, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(now2359, tz)
                val expected = makeNow(dayOfMonth = 2, hour = 0, minute = 0)
                target shouldBe expected
            }

            test("Once: alarm at 00:00, now=00:00 → fires next day") {
                val nowMidnight = makeNow(hour = 0, minute = 0)
                val alarm = Alarm(hour = 0, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(nowMidnight, tz)
                val expected = makeNow(dayOfMonth = 2, hour = 0, minute = 0)
                target shouldBe expected
            }

            test("Everyday: alarm at 23:59, now=23:58 → fires same day") {
                val now2358 = makeNow(hour = 23, minute = 58)
                val alarm = Alarm(hour = 23, minute = 59, repeatType = Alarm.RepeatType.Everyday)
                val target = alarm.toNextFireTime(now2358, tz)
                val expected = makeNow(hour = 23, minute = 59)
                target shouldBe expected
            }

            test("Everyday: alarm at 23:59, now=23:59 → fires next day") {
                val now2359 = makeNow(hour = 23, minute = 59)
                val alarm = Alarm(hour = 23, minute = 59, repeatType = Alarm.RepeatType.Everyday)
                val target = alarm.toNextFireTime(now2359, tz)
                val expected = makeNow(dayOfMonth = 2, hour = 23, minute = 59)
                target shouldBe expected
            }
        }

        context("toNextFireTime — month-end crossing") {
            test("Once: July 31st, time passed → August 1st") {
                val july31 = makeNow(dayOfMonth = 31, hour = 12, minute = 0)
                val alarm = Alarm(hour = 11, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(july31, tz)
                val expected = LocalDateTime(2022, 8, 1, 11, 0, 0).toInstant(tz)
                target shouldBe expected
            }

            test("Everyday: month with 30 days — June 30th next day → July 1st") {
                val june30 = LocalDateTime(2022, 6, 30, 18, 0, 0).toInstant(tz)
                val alarm = Alarm(hour = 8, minute = 0, repeatType = Alarm.RepeatType.Everyday)
                val target = alarm.toNextFireTime(june30, tz)
                val expected = LocalDateTime(2022, 7, 1, 8, 0, 0).toInstant(tz)
                target shouldBe expected
            }
        }

        context("toNextFireTime — year-end crossing") {
            test("Once: December 31st next day → January 1st next year") {
                val dec31 = LocalDateTime(2022, 12, 31, 20, 0, 0).toInstant(tz)
                val alarm = Alarm(hour = 6, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(dec31, tz)
                val expected = LocalDateTime(2023, 1, 1, 6, 0, 0).toInstant(tz)
                target shouldBe expected
            }
        }

        context("toNextFireTime — leap year") {
            test("Once: Feb 28 in leap year → Feb 29") {
                val feb28 = LocalDateTime(2024, 2, 28, 20, 0, 0).toInstant(tz)
                val alarm = Alarm(hour = 7, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(feb28, tz)
                val expected = LocalDateTime(2024, 2, 29, 7, 0, 0).toInstant(tz)
                target shouldBe expected
            }

            test("Once: Feb 28 in non-leap year → March 1") {
                val feb28 = LocalDateTime(2023, 2, 28, 20, 0, 0).toInstant(tz)
                val alarm = Alarm(hour = 7, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(feb28, tz)
                val expected = LocalDateTime(2023, 3, 1, 7, 0, 0).toInstant(tz)
                target shouldBe expected
            }

            test("Once: Feb 29 in leap year → March 1") {
                val feb29 = LocalDateTime(2024, 2, 29, 20, 0, 0).toInstant(tz)
                val alarm = Alarm(hour = 7, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(feb29, tz)
                val expected = LocalDateTime(2024, 3, 1, 7, 0, 0).toInstant(tz)
                target shouldBe expected
            }

            test("Date: Feb 29 in leap year — exact date used") {
                val alarm = Alarm(
                    hour = 8,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(LocalDate(2024, 2, 29))
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = LocalDateTime(2024, 2, 29, 8, 0, 0).toInstant(tz)
                target shouldBe expected
            }
        }

        context("toNextFireTime — RepeatType.Date edge cases") {
            test("Date: past date returns past instant") {
                val pastDate = LocalDate(2020, 1, 1)
                val alarm = Alarm(
                    hour = 9,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(pastDate)
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = LocalDateTime(2020, 1, 1, 9, 0, 0).toInstant(tz)
                target shouldBe expected
                (target < now) shouldBe true
            }

            test("Date: today's date with future time") {
                val todayDate = LocalDate(2022, 7, 1)
                val alarm = Alarm(
                    hour = 15,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(todayDate)
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(hour = 15, minute = 0)
                target shouldBe expected
            }

            test("Date: today's date with past time still returns today") {
                val todayDate = LocalDate(2022, 7, 1)
                val alarm = Alarm(
                    hour = 8,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(todayDate)
                )
                val target = alarm.toNextFireTime(now, tz)
                // Date type does NOT auto-advance to next day
                val expected = makeNow(hour = 8, minute = 0)
                target shouldBe expected
            }
        }

        context("toNextFireTime — RepeatType.Days edge cases") {
            test("all 7 days, time in future → fires same day") {
                val allDays = DayOfWeek.entries.toList()
                val alarm = Alarm(
                    hour = 18,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(allDays)
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(hour = 18, minute = 0)
                target shouldBe expected
            }

            test("all 7 days, time passed → fires next day") {
                val allDays = DayOfWeek.entries.toList()
                val alarm = Alarm(
                    hour = 8,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(allDays)
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(dayOfMonth = 2, hour = 8, minute = 0)
                target shouldBe expected
            }

            test("two days (Mon, Thu), now is Fri past time → skips to Mon") {
                val alarm = Alarm(
                    hour = 8,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(
                        listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
                    )
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(dayOfMonth = 4, hour = 8, minute = 0)
                target shouldBe expected
            }

            test("two days (Fri, Sun), now is Fri time passed → fires Sun") {
                val alarm = Alarm(
                    hour = 8,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(
                        listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
                    )
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(dayOfMonth = 3, hour = 8, minute = 0)
                target shouldBe expected
            }

            test("Saturday only, now is Friday → fires next day (Saturday)") {
                val alarm = Alarm(
                    hour = 6,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.SATURDAY))
                )
                val target = alarm.toNextFireTime(now, tz)
                val expected = makeNow(dayOfMonth = 2, hour = 6, minute = 0)
                target shouldBe expected
            }
        }

        context("pickNearestTime — additional cases") {
            test("single alarm in list") {
                val alarm = Alarm(hour = 15, minute = 0)
                val result = listOf(alarm).pickNearestTime(now, tz)
                result?.first shouldBe alarm
                result?.second shouldBe makeNow(hour = 15, minute = 0)
            }

            test("mixed RepeatTypes: nearest is selected correctly") {
                val onceAlarm = Alarm(hour = 14, minute = 0, repeatType = Alarm.RepeatType.Once)
                val everydayAlarm = Alarm(
                    hour = 13,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Everyday
                )
                val daysAlarm = Alarm(
                    hour = 15,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.FRIDAY))
                )
                // now is Fri 12:00
                // Once 14:00 today, Everyday 13:00 today, Days(Fri) 15:00 today
                // nearest = Everyday 13:00
                val result = listOf(onceAlarm, everydayAlarm, daysAlarm)
                    .pickNearestTime(now, tz)
                result?.first shouldBe everydayAlarm
            }

            test("all alarms have Date in past: returns earliest past alarm") {
                val alarm1 = Alarm(
                    hour = 9,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(LocalDate(2021, 6, 1))
                )
                val alarm2 = Alarm(
                    hour = 9,
                    minute = 0,
                    repeatType = Alarm.RepeatType.Date(LocalDate(2022, 1, 1))
                )
                val result = listOf(alarm1, alarm2).pickNearestTime(now, tz)
                result?.first shouldBe alarm1
            }
        }

        context("getNearestDayOfWeekOrNull — additional cases") {
            test("empty list returns null") {
                emptyList<DayOfWeek>().getNearestDayOfWeekOrNull(DayOfWeek.MONDAY) shouldBe null
            }

            test("single day, same as from") {
                val days = listOf(DayOfWeek.WEDNESDAY)
                days.getNearestDayOfWeekOrNull(DayOfWeek.WEDNESDAY) shouldBe DayOfWeek.WEDNESDAY
            }

            test("single day, before from → wraps to next week") {
                val days = listOf(DayOfWeek.MONDAY)
                days.getNearestDayOfWeekOrNull(DayOfWeek.FRIDAY) shouldBe DayOfWeek.MONDAY
            }

            test("all 7 days → returns from itself") {
                val allDays = DayOfWeek.entries.toList()
                allDays.getNearestDayOfWeekOrNull(DayOfWeek.THURSDAY) shouldBe DayOfWeek.THURSDAY
            }

            test("from=SUNDAY, list has MON and WED → wraps to MONDAY") {
                val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
                // SUNDAY iso=7, no days >= 7 in list → wraps → min = MONDAY
                days.getNearestDayOfWeekOrNull(DayOfWeek.SUNDAY) shouldBe DayOfWeek.MONDAY
            }

            test("from=MONDAY, list has SUN only → returns SUN (wrap)") {
                val days = listOf(DayOfWeek.SUNDAY)
                // SUNDAY iso=7 >= MONDAY iso=1 → same week, returns SUNDAY
                days.getNearestDayOfWeekOrNull(DayOfWeek.MONDAY) shouldBe DayOfWeek.SUNDAY
            }
        }

        context("isSameDate — timezone boundary") {
            test("same instant, different timezone can yield different dates") {
                // 2022-07-01 23:30 UTC = 2022-07-02 08:30 JST
                val instant = LocalDateTime(2022, 7, 1, 23, 30, 0).toInstant(tz)
                val jst = TimeZone.of("Asia/Tokyo")
                // In UTC it's July 1, in JST it's July 2
                val july1Noon = makeNow() // 2022-07-01 12:00 UTC
                instant.isSameDate(july1Noon, tz) shouldBe true
                instant.isSameDate(july1Noon, jst) shouldBe false
            }
        }

        context("toNextFireTime — non-UTC timezone") {
            test("alarm in JST respects timezone offset") {
                val jst = TimeZone.of("Asia/Tokyo") // UTC+9
                // 2022-07-01 21:00 JST = 2022-07-01 12:00 UTC
                val nowJst = LocalDateTime(2022, 7, 1, 21, 0, 0).toInstant(jst)
                val alarm = Alarm(hour = 22, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(nowJst, jst)
                // 22:00 JST same day
                val expected = LocalDateTime(2022, 7, 1, 22, 0, 0).toInstant(jst)
                target shouldBe expected
            }

            test("alarm in JST crosses day differently than UTC") {
                val jst = TimeZone.of("Asia/Tokyo")
                // 2022-07-01 23:30 JST
                val nowJst = LocalDateTime(2022, 7, 1, 23, 30, 0).toInstant(jst)
                val alarm = Alarm(hour = 6, minute = 0, repeatType = Alarm.RepeatType.Once)
                val target = alarm.toNextFireTime(nowJst, jst)
                // 06:00 < 23:30 → next day in JST = July 2 06:00 JST
                val expected = LocalDateTime(2022, 7, 2, 6, 0, 0).toInstant(jst)
                target shouldBe expected
            }
        }
    })