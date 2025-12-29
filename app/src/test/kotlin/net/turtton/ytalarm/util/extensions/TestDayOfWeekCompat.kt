package net.turtton.ytalarm.util.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.util.DayOfWeekCompat
import java.util.Calendar

@Suppress("UNUSED")
class TestDayOfWeekCompat :
    FunSpec({
        context("getNearestWeekOrNull") {
            test("contains same day") {
                listOf(
                    DayOfWeekCompat.SUNDAY,
                    DayOfWeekCompat.WEDNESDAY,
                    DayOfWeekCompat.FRIDAY
                ).getNearestWeekOrNull(Calendar.WEDNESDAY) shouldBe DayOfWeekCompat.WEDNESDAY
            }

            test("same week") {
                listOf(
                    DayOfWeekCompat.WEDNESDAY,
                    DayOfWeekCompat.FRIDAY,
                    DayOfWeekCompat.SATURDAY
                ).getNearestWeekOrNull(Calendar.SUNDAY) shouldBe DayOfWeekCompat.WEDNESDAY
            }

            test("next week") {
                listOf(
                    DayOfWeekCompat.SUNDAY,
                    DayOfWeekCompat.WEDNESDAY,
                    DayOfWeekCompat.THURSDAY
                ).getNearestWeekOrNull(Calendar.SATURDAY) shouldBe DayOfWeekCompat.SUNDAY
            }
        }
    })