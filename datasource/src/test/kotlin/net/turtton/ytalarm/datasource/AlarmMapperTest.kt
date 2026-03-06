package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import net.turtton.ytalarm.datasource.entity.AlarmEntity
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Alarm
import java.util.Calendar

class AlarmMapperTest :
    FunSpec({

        test("AlarmEntity.toDomain - Once RepeatType") {
            val calendar = Calendar.getInstance().apply { timeInMillis = 1000L }
            val entity = AlarmEntity(
                id = 1L,
                hour = 8,
                minute = 30,
                repeatType = AlarmEntity.RepeatType.Once,
                playListId = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = 50,
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = calendar,
                lastUpdated = calendar
            )
            val domain = entity.toDomain()

            domain.id shouldBe 1L
            domain.hour shouldBe 8
            domain.minute shouldBe 30
            domain.repeatType shouldBe Alarm.RepeatType.Once
            domain.volume shouldBe Alarm.Volume(50)
            domain.snoozeMinute shouldBe 10
            domain.shouldVibrate shouldBe true
            domain.isEnabled shouldBe false
            domain.creationDate shouldBe Instant.fromEpochMilliseconds(1000L)
        }

        test("AlarmEntity.toDomain - Everyday RepeatType") {
            val entity = AlarmEntity(
                id = 2L,
                hour = 7,
                minute = 0,
                repeatType = AlarmEntity.RepeatType.Everyday,
                playListId = listOf(1L, 2L),
                shouldLoop = true,
                shouldShuffle = true,
                volume = 75,
                snoozeMinute = 5,
                shouldVibrate = false,
                isEnabled = true,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            domain.repeatType shouldBe Alarm.RepeatType.Everyday
            domain.playlistIds shouldBe listOf(1L, 2L)
        }

        test("AlarmEntity.toDomain - Days RepeatType with DayOfWeek conversion") {
            val entity = AlarmEntity(
                id = 3L,
                hour = 6,
                minute = 0,
                repeatType = AlarmEntity.RepeatType.Days(
                    listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
                ),
                playListId = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = 50,
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = true,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            val expectedDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            domain.repeatType shouldBe Alarm.RepeatType.Days(expectedDays)
        }

        test("AlarmEntity.toDomain - Date RepeatType") {
            val targetDate = java.util.Date(1700000000000L)
            val entity = AlarmEntity(
                id = 4L,
                hour = 9,
                minute = 0,
                repeatType = AlarmEntity.RepeatType.Date(targetDate),
                playListId = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = 50,
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            val expectedDate = LocalDate(2023, 11, 14)
            domain.repeatType shouldBe Alarm.RepeatType.Date(expectedDate)
        }

        test("Alarm.toEntity - roundtrip") {
            val instant = Instant.fromEpochMilliseconds(2000L)
            val alarm = Alarm(
                id = 5L,
                hour = 10,
                minute = 45,
                repeatType = Alarm.RepeatType.Once,
                playlistIds = listOf(3L),
                shouldLoop = true,
                shouldShuffle = false,
                volume = Alarm.Volume(60),
                snoozeMinute = 15,
                shouldVibrate = false,
                isEnabled = true,
                creationDate = instant,
                lastUpdated = instant
            )
            val entity = alarm.toEntity()
            val backToDomain = entity.toDomain()

            backToDomain.id shouldBe alarm.id
            backToDomain.hour shouldBe alarm.hour
            backToDomain.minute shouldBe alarm.minute
            backToDomain.repeatType shouldBe alarm.repeatType
            backToDomain.playlistIds shouldBe alarm.playlistIds
            backToDomain.shouldLoop shouldBe alarm.shouldLoop
            backToDomain.shouldShuffle shouldBe alarm.shouldShuffle
            backToDomain.volume shouldBe alarm.volume
            backToDomain.snoozeMinute shouldBe alarm.snoozeMinute
            backToDomain.shouldVibrate shouldBe alarm.shouldVibrate
            backToDomain.isEnabled shouldBe alarm.isEnabled
            backToDomain.creationDate shouldBe alarm.creationDate
            backToDomain.lastUpdated shouldBe alarm.lastUpdated
        }

        test("Days with all days of week - Calendar to DayOfWeek conversion") {
            val allCalendarDays = listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )
            val entity = AlarmEntity(
                id = 6L,
                hour = 0,
                minute = 0,
                repeatType = AlarmEntity.RepeatType.Days(allCalendarDays),
                playListId = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = 50,
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
            val domain = entity.toDomain()
            val expectedDays = listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
            domain.repeatType shouldBe Alarm.RepeatType.Days(expectedDays)
        }

        test("Alarm.toEntity - Days with DayOfWeek to Calendar conversion") {
            val alarm = Alarm(
                id = 7L,
                hour = 7,
                minute = 0,
                repeatType = Alarm.RepeatType.Days(
                    listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                ),
                playlistIds = emptyList(),
                shouldLoop = false,
                shouldShuffle = false,
                volume = Alarm.Volume(50),
                snoozeMinute = 10,
                shouldVibrate = true,
                isEnabled = false,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            val entity = alarm.toEntity()
            val repeatType = entity.repeatType as AlarmEntity.RepeatType.Days
            repeatType.days shouldBe listOf(Calendar.MONDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        }
    })