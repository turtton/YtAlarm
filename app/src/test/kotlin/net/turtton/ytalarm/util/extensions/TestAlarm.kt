package net.turtton.ytalarm.util.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.util.RepeatType
import java.util.Calendar
import java.util.Date

@Suppress("UNUSED")
class TestAlarm : FunSpec({
    val calendar = Calendar.getInstance()

    // 2022/7/1(Fri) 12:00
    fun init(target: Calendar) {
        target.set(Calendar.YEAR, 2022)
        // Calendar.JULY == 6
        target.set(Calendar.MONTH, Calendar.JULY)
        target.set(Calendar.DAY_OF_MONTH, 1)
        target.set(Calendar.HOUR_OF_DAY, 12)
        target.set(Calendar.MINUTE, 0)
    }

    beforeTest {
        init(calendar)
    }

    context("toCalendar") {
        fun Calendar.checkTimeSame(hour: Int, minute: Int) {
            get(Calendar.HOUR_OF_DAY) shouldBeExactly hour
            get(Calendar.MINUTE) shouldBeExactly minute
        }
        context("no date specification") {
            val alarm = Alarm(repeatType = RepeatType.Once)
            test("near future") {
                val hour = 12
                val minute = 30
                val target = alarm.copy(hour = hour, minute = minute).toCalendar(calendar)

                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE]
                target.checkTimeSame(hour, minute)
            }
            test("previous") {
                val hour = 11
                val minute = 30
                val target = alarm.copy(hour = hour, minute = minute).toCalendar(calendar)

                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE] + 1
                target.checkTimeSame(hour, minute)
            }
        }
        context("specify day of week") {
            test("near future in same day of week") {
                val hour = 12
                val minute = 30

                val repeatType =
                    RepeatType.Days(listOf(DayOfWeekCompat.FRIDAY, DayOfWeekCompat.SATURDAY))
                val target = Alarm(hour = hour, minute = minute, repeatType = repeatType)
                    .toCalendar(calendar)

                // Means Calendar.FRIDAY
                target[Calendar.DAY_OF_WEEK] shouldBeExactly calendar[Calendar.DAY_OF_WEEK]
                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE]
                target.checkTimeSame(hour, minute)
                target[Calendar.MONTH] shouldBeExactly calendar[Calendar.MONTH]
            }
            test("previous time in same day of week") {
                val hour = 11
                val minute = 30

                val repeatType =
                    RepeatType.Days(listOf(DayOfWeekCompat.FRIDAY))
                val target = Alarm(hour = hour, minute = minute, repeatType = repeatType)
                    .toCalendar(calendar)

                target[Calendar.DAY_OF_WEEK] shouldBeExactly calendar[Calendar.DAY_OF_WEEK]
                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE] + 7
                target.checkTimeSame(hour, minute)
                target[Calendar.MONTH] shouldBeExactly calendar[Calendar.MONTH]
            }
            test("next day of week") {
                val hour = 11
                val minute = 30

                val repeatType =
                    RepeatType.Days(listOf(DayOfWeekCompat.SATURDAY, DayOfWeekCompat.MONDAY))
                val target = Alarm(hour = hour, minute = minute, repeatType = repeatType)
                    .toCalendar(calendar)

                target[Calendar.DAY_OF_WEEK] shouldBeExactly Calendar.SATURDAY
                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE] + 1
                target.checkTimeSame(hour, minute)
                target[Calendar.MONTH] shouldBeExactly calendar[Calendar.MONTH]
            }
            test("next week") {
                val hour = 11
                val minute = 30

                val repeatType =
                    RepeatType.Days(listOf(DayOfWeekCompat.MONDAY, DayOfWeekCompat.WEDNESDAY))
                val target = Alarm(hour = hour, minute = minute, repeatType = repeatType)
                    .toCalendar(calendar)

                target[Calendar.DAY_OF_WEEK] shouldBeExactly Calendar.MONDAY
                target[Calendar.DATE] shouldBeExactly calendar[Calendar.DATE] + 3
                target.checkTimeSame(hour, minute)
                target[Calendar.MONTH] shouldBeExactly calendar[Calendar.MONTH]
            }
        }
        test("specify date") {
            val targetCalendar = Calendar.getInstance()
            init(targetCalendar)

            val hour = 12
            val minute = 30
            val targetMonth = Calendar.DECEMBER
            val targetDayOfMonth = 15

            targetCalendar.set(Calendar.MONTH, targetMonth)
            targetCalendar.set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
            val targetDate = Date(targetCalendar.timeInMillis)

            val alarm = Alarm(
                hour = hour,
                minute = minute,
                repeatType = RepeatType.Date(targetDate)
            )
            val target = alarm.toCalendar(calendar)

            target[Calendar.MONTH] shouldBeExactly targetMonth
            target[Calendar.DAY_OF_MONTH] shouldBeExactly targetDayOfMonth
            target.checkTimeSame(hour, minute)
        }
    }

    context("pickNearestTime") {
        val seven = Alarm(hour = 7)
        val sevenHalf = Alarm(hour = 7, minute = 30)
        val eleven = Alarm(hour = 11)
        val twelve = Alarm(hour = 12)
        val fifteen = Alarm(hour = 15)
        val nineteen = Alarm(hour = 19)

        test("Exclude same time") {
            val target = listOf(twelve, fifteen, nineteen)
            target.pickNearestTime(calendar)?.first shouldBe fifteen
        }

        test("next day") {
            val target = listOf(seven, sevenHalf, eleven)
            target.pickNearestTime(calendar)?.first shouldBe seven
        }
    }
})