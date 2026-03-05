package net.turtton.ytalarm.kernel.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class AlarmTest :
    FunSpec({
        test("Volume default value is 50") {
            val volume = Alarm.Volume()
            volume.volume shouldBe 50
        }

        test("Volume accepts values in range 0..100") {
            Alarm.Volume(0).volume shouldBe 0
            Alarm.Volume(100).volume shouldBe 100
            Alarm.Volume(50).volume shouldBe 50
        }

        test("Volume rejects values out of range") {
            shouldThrow<IllegalArgumentException> { Alarm.Volume(-1) }
            shouldThrow<IllegalArgumentException> { Alarm.Volume(101) }
        }

        test("Alarm default values are set correctly") {
            val alarm = Alarm()
            alarm.id shouldBe 0L
            alarm.hour shouldBe 0
            alarm.minute shouldBe 0
            alarm.repeatType shouldBe Alarm.RepeatType.Once
            alarm.playlistIds shouldBe emptyList()
            alarm.shouldLoop shouldBe false
            alarm.shouldShuffle shouldBe false
            alarm.snoozeMinute shouldBe 10
            alarm.shouldVibrate shouldBe true
            alarm.isEnabled shouldBe false
        }

        test("hour validation: valid range 0..23") {
            Alarm(hour = 0).hour shouldBe 0
            Alarm(hour = 23).hour shouldBe 23
        }

        test("hour validation: rejects invalid values") {
            shouldThrow<IllegalArgumentException> { Alarm(hour = -1) }
            shouldThrow<IllegalArgumentException> { Alarm(hour = 24) }
        }

        test("minute validation: valid range 0..59") {
            Alarm(minute = 0).minute shouldBe 0
            Alarm(minute = 59).minute shouldBe 59
        }

        test("minute validation: rejects invalid values") {
            shouldThrow<IllegalArgumentException> { Alarm(minute = -1) }
            shouldThrow<IllegalArgumentException> { Alarm(minute = 60) }
        }

        test("snoozeMinute validation: valid positive value") {
            Alarm(snoozeMinute = 1).snoozeMinute shouldBe 1
            Alarm(snoozeMinute = 30).snoozeMinute shouldBe 30
        }

        test("snoozeMinute validation: rejects zero and negative") {
            shouldThrow<IllegalArgumentException> { Alarm(snoozeMinute = 0) }
            shouldThrow<IllegalArgumentException> { Alarm(snoozeMinute = -1) }
        }

        test("RepeatType.Days holds list of DayOfWeek") {
            val days = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
            val repeatType = Alarm.RepeatType.Days(days)
            repeatType.days shouldBe days
        }

        test("RepeatType.Days rejects empty list") {
            shouldThrow<IllegalArgumentException> { Alarm.RepeatType.Days(emptyList()) }
        }

        test("RepeatType.Date holds LocalDate") {
            val date = LocalDate(2026, 3, 5)
            val repeatType = Alarm.RepeatType.Date(date)
            repeatType.targetDate shouldBe date
        }
    })