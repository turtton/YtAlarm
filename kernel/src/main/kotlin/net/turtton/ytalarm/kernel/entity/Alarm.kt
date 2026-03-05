package net.turtton.ytalarm.kernel.entity

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Alarm(
    val id: Long = 0L,
    val hour: Int = 0,
    val minute: Int = 0,
    val repeatType: RepeatType = RepeatType.Once,
    val playlistIds: List<Long> = emptyList(),
    val shouldLoop: Boolean = false,
    val shouldShuffle: Boolean = false,
    val volume: Volume = Volume(),
    val snoozeMinute: Int = 10,
    val shouldVibrate: Boolean = true,
    val isEnabled: Boolean = false,
    val creationDate: Instant = Clock.System.now(),
    val lastUpdated: Instant = Clock.System.now()
) {
    init {
        require(hour in 0..MAX_HOUR) { "hour must be in 0..$MAX_HOUR but was $hour" }
        require(minute in 0..MAX_MINUTE) { "minute must be in 0..$MAX_MINUTE but was $minute" }
        require(snoozeMinute > 0) { "snoozeMinute must be > 0 but was $snoozeMinute" }
    }

    data class Volume(val volume: Int = 50) {
        init {
            require(volume in MIN_VOLUME..MAX_VOLUME) {
                "Volume must be in $MIN_VOLUME..$MAX_VOLUME but was $volume"
            }
        }

        companion object {
            const val MAX_VOLUME = 100
            const val MIN_VOLUME = 0
        }
    }

    sealed interface RepeatType {
        data object Once : RepeatType

        data object Everyday : RepeatType

        data object Snooze : RepeatType

        data class Days(val days: List<DayOfWeek>) : RepeatType {
            init {
                require(days.isNotEmpty()) { "Days list must not be empty" }
            }
        }

        data class Date(val targetDate: LocalDate) : RepeatType
    }

    companion object {
        const val MAX_HOUR = 23
        const val MAX_MINUTE = 59
    }
}