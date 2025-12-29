package net.turtton.ytalarm.util.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

@Suppress("UNUSED")
class TestCalendar :
    FunSpec({
        val calendar = Calendar.getInstance()

        // 2022/6/1 12:00
        fun init(target: Calendar) {
            target.set(Calendar.YEAR, 2022)
            target.set(Calendar.MONTH, 6)
            target.set(Calendar.DAY_OF_MONTH, 1)
            target.set(Calendar.HOUR_OF_DAY, 12)
            target.set(Calendar.MINUTE, 0)
        }

        beforeTest {
            init(calendar)
        }

        context("isSameDate") {
            test("same year") {
                val target = Calendar.getInstance()
                init(target)

                calendar.isSameDate(target) shouldBe true

                target.set(Calendar.DAY_OF_MONTH, 5)
                calendar.isSameDate(target) shouldBe false
            }

            test("wrong year") {
                val target = Calendar.getInstance()
                init(target)
                target.set(Calendar.YEAR, 2021)

                calendar.isSameDate(target) shouldBe false

                target.set(Calendar.DAY_OF_MONTH, 5)
                calendar.isSameDate(target) shouldBe false
            }
        }

        context("isPrevOrSameTime") {
            test("previous hour") {
                calendar.isPrevOrSameTime(11, 0) shouldBe true
            }
            test("following hour") {
                calendar.isPrevOrSameTime(13, 0) shouldBe false
            }
            test("same hour") {
                calendar.set(Calendar.MINUTE, 30)
                calendar.isPrevOrSameTime(12, 0) shouldBe true
                calendar.isPrevOrSameTime(12, 30) shouldBe true
                calendar.isPrevOrSameTime(12, 50) shouldBe false
            }
        }
    })