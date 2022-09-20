package net.turtton.ytalarm.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.structure.Alarm
import java.util.*

@Suppress("UNUSED")
class TestAlarmManager : FunSpec({
    context("pickNearestTime") {
        val calendar = Calendar.getInstance()
        // 2022/7/1 12:00
        calendar.set(Calendar.YEAR, 2022)
        calendar.set(Calendar.MONTH, Calendar.JULY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)

        val seven = Alarm(time = "07:00")
        val sevenHalf = Alarm(time = "07:30")
        val eleven = Alarm(time = "11:00")
        val twelve = Alarm(time = "12:00")
        val fifteen = Alarm(time = "15:00")
        val nineteen = Alarm(time = "19:00")

        test("Exclude same time") {
            val target = listOf(twelve, fifteen, nineteen)
            target.pickNearestTime(calendar) shouldBe fifteen
        }

        test("next day") {
            val target = listOf(seven, sevenHalf, eleven)
            target.pickNearestTime(calendar) shouldBe seven
        }
    }
})