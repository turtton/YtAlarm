package net.turtton.ytalarm.ui.model

import android.content.Context
import kotlinx.datetime.Instant
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.util.extensions.getDisplay
import java.util.Locale
import kotlin.time.Clock

data class AlarmUiModel(
    val id: Long,
    val timeDisplay: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val repeatTypeDisplay: String,
    val isSnooze: Boolean,
    val playlistIds: List<Long>,
    val creationDate: Instant,
    val lastUpdated: Instant
) {
    companion object {
        fun preview(
            id: Long = 1L,
            hour: Int = 7,
            minute: Int = 30,
            isEnabled: Boolean = true,
            repeatTypeDisplay: String = "Every day"
        ) = AlarmUiModel(
            id,
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
            hour,
            minute,
            isEnabled,
            repeatTypeDisplay,
            isSnooze = false,
            playlistIds = emptyList(),
            Clock.System.now(),
            Clock.System.now()
        )
    }
}

fun Alarm.toUiModel(context: Context): AlarmUiModel = AlarmUiModel(
    id = id,
    timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
    hour = hour,
    minute = minute,
    isEnabled = isEnabled,
    repeatTypeDisplay = repeatType.getDisplay(context),
    isSnooze = repeatType is Alarm.RepeatType.Snooze,
    playlistIds = playlistIds,
    creationDate = creationDate,
    lastUpdated = lastUpdated
)