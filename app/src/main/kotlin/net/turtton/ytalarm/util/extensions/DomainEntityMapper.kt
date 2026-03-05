@file:Suppress("NewApi")

package net.turtton.ytalarm.util.extensions

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.DayOfWeekCompat
import java.util.Calendar
import java.util.Date
import java.util.UUID
import net.turtton.ytalarm.database.structure.Alarm as LegacyAlarm
import net.turtton.ytalarm.database.structure.Playlist as LegacyPlaylist
import net.turtton.ytalarm.database.structure.Video as LegacyVideo
import net.turtton.ytalarm.kernel.entity.Alarm as DomainAlarm
import net.turtton.ytalarm.kernel.entity.Playlist as DomainPlaylist
import net.turtton.ytalarm.kernel.entity.Video as DomainVideo

// -----------------------------------------------
// Alarm conversions
// -----------------------------------------------

/**
 * Converts a domain [DomainAlarm] to the legacy [LegacyAlarm].
 *
 * This is a transitional mapper used during Clean Architecture migration (Phase 4).
 * Once Screen/Activity layer is migrated (Task 12), this mapper can be removed.
 */
fun DomainAlarm.toLegacy(): LegacyAlarm = LegacyAlarm(
    id = id,
    hour = hour,
    minute = minute,
    repeatType = repeatType.toLegacy(),
    playListId = playlistIds,
    shouldLoop = shouldLoop,
    shouldShuffle = shouldShuffle,
    volume = LegacyAlarm.Volume(volume.volume),
    snoozeMinute = snoozeMinute,
    shouldVibrate = shouldVibrate,
    isEnabled = isEnabled,
    creationDate = creationDate.toCalendar(),
    lastUpdated = lastUpdated.toCalendar()
)

fun DomainAlarm.RepeatType.toLegacy(): LegacyAlarm.RepeatType = when (this) {
    is DomainAlarm.RepeatType.Once -> LegacyAlarm.RepeatType.Once

    is DomainAlarm.RepeatType.Everyday -> LegacyAlarm.RepeatType.Everyday

    is DomainAlarm.RepeatType.Snooze -> LegacyAlarm.RepeatType.Snooze

    is DomainAlarm.RepeatType.Days -> LegacyAlarm.RepeatType.Days(
        days.map { it.toDayOfWeekCompat() }
    )

    is DomainAlarm.RepeatType.Date -> LegacyAlarm.RepeatType.Date(
        targetDate.toJavaDate()
    )
}

/**
 * Converts a legacy [LegacyAlarm] to the domain [DomainAlarm].
 */
fun LegacyAlarm.toDomain(): DomainAlarm = DomainAlarm(
    id = id,
    hour = hour,
    minute = minute,
    repeatType = repeatType.toDomain(),
    playlistIds = playListId,
    shouldLoop = shouldLoop,
    shouldShuffle = shouldShuffle,
    volume = DomainAlarm.Volume(volume.volume),
    snoozeMinute = snoozeMinute,
    shouldVibrate = shouldVibrate,
    isEnabled = isEnabled,
    creationDate = creationDate.toKotlinInstant(),
    lastUpdated = lastUpdated.toKotlinInstant()
)

fun LegacyAlarm.RepeatType.toDomain(): DomainAlarm.RepeatType = when (this) {
    is LegacyAlarm.RepeatType.Once -> DomainAlarm.RepeatType.Once

    is LegacyAlarm.RepeatType.Everyday -> DomainAlarm.RepeatType.Everyday

    is LegacyAlarm.RepeatType.Snooze -> DomainAlarm.RepeatType.Snooze

    is LegacyAlarm.RepeatType.Days -> DomainAlarm.RepeatType.Days(
        days.map { it.toDayOfWeek() }
    )

    is LegacyAlarm.RepeatType.Date -> DomainAlarm.RepeatType.Date(
        targetDate.toLocalDate()
    )
}

// -----------------------------------------------
// Playlist conversions
// -----------------------------------------------

/**
 * Converts a domain [DomainPlaylist] to the legacy [LegacyPlaylist].
 *
 * Note: [DomainPlaylist.Thumbnail.None] is mapped to [LegacyPlaylist.Thumbnail.Drawable] with
 * [R.drawable.ic_no_image] as there is no direct equivalent in the legacy model.
 *
 * Note: [DomainPlaylist.Type.CloudPlaylist] loses the [UUID] workerId field in the domain model.
 * A random UUID is assigned during conversion; the worker state should be tracked separately.
 */
fun DomainPlaylist.toLegacy(): LegacyPlaylist = LegacyPlaylist(
    id = id,
    title = title,
    thumbnail = thumbnail.toLegacy(),
    videos = videos,
    type = type.toLegacy(),
    creationDate = creationDate.toCalendar(),
    lastUpdated = lastUpdated.toCalendar()
)

private fun DomainPlaylist.Thumbnail.toLegacy(): LegacyPlaylist.Thumbnail = when (this) {
    is DomainPlaylist.Thumbnail.Video -> LegacyPlaylist.Thumbnail.Video(id = id)
    is DomainPlaylist.Thumbnail.None -> LegacyPlaylist.Thumbnail.Drawable(R.drawable.ic_no_image)
}

private fun DomainPlaylist.Type.toLegacy(): LegacyPlaylist.Type = when (this) {
    is DomainPlaylist.Type.Importing -> LegacyPlaylist.Type.Importing

    is DomainPlaylist.Type.Original -> LegacyPlaylist.Type.Original

    is DomainPlaylist.Type.CloudPlaylist -> LegacyPlaylist.Type.CloudPlaylist(
        url = url,
        workerId = UUID.randomUUID(),
        syncRule = syncRule.toLegacy()
    )
}

private fun DomainPlaylist.SyncRule.toLegacy(): LegacyPlaylist.SyncRule = when (this) {
    DomainPlaylist.SyncRule.ALWAYS_ADD -> LegacyPlaylist.SyncRule.ALWAYS_ADD
    DomainPlaylist.SyncRule.DELETE_IF_NOT_EXIST -> LegacyPlaylist.SyncRule.DELETE_IF_NOT_EXIST
}

/**
 * Converts a legacy [LegacyPlaylist] to the domain [DomainPlaylist].
 *
 * Note: [LegacyPlaylist.Thumbnail.Drawable] is mapped to [DomainPlaylist.Thumbnail.None] as
 * drawable resources are Android-specific and not part of the domain model.
 */
fun LegacyPlaylist.toDomain(): DomainPlaylist = DomainPlaylist(
    id = id,
    title = title,
    thumbnail = thumbnail.toDomain(),
    videos = videos,
    type = type.toDomain(),
    creationDate = creationDate.toKotlinInstant(),
    lastUpdated = lastUpdated.toKotlinInstant()
)

private fun LegacyPlaylist.Thumbnail.toDomain(): DomainPlaylist.Thumbnail = when (this) {
    is LegacyPlaylist.Thumbnail.Video -> DomainPlaylist.Thumbnail.Video(id = id)
    is LegacyPlaylist.Thumbnail.Drawable -> DomainPlaylist.Thumbnail.None
}

private fun LegacyPlaylist.Type.toDomain(): DomainPlaylist.Type = when (this) {
    is LegacyPlaylist.Type.Importing -> DomainPlaylist.Type.Importing

    is LegacyPlaylist.Type.Original -> DomainPlaylist.Type.Original

    is LegacyPlaylist.Type.CloudPlaylist -> DomainPlaylist.Type.CloudPlaylist(
        url = url,
        syncRule = syncRule.toDomain()
    )
}

private fun LegacyPlaylist.SyncRule.toDomain(): DomainPlaylist.SyncRule = when (this) {
    LegacyPlaylist.SyncRule.ALWAYS_ADD -> DomainPlaylist.SyncRule.ALWAYS_ADD
    LegacyPlaylist.SyncRule.DELETE_IF_NOT_EXIST -> DomainPlaylist.SyncRule.DELETE_IF_NOT_EXIST
}

// -----------------------------------------------
// Video conversions
// -----------------------------------------------

/**
 * Converts a domain [DomainVideo] to the legacy [LegacyVideo].
 *
 * Note: [DomainVideo.State.Importing] does not carry a worker UUID in the domain model.
 * A random UUID is assigned during conversion for compatibility with legacy model.
 *
 * Note: [DomainVideo.State.Failed] carries a sourceUrl. In the legacy model, Failed is
 * represented as [LegacyVideo.State.Importing] with [LegacyVideo.WorkerState.Failed].
 */
fun DomainVideo.toLegacy(): LegacyVideo = LegacyVideo(
    id = id,
    videoId = videoId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    domain = domain,
    stateData = state.toLegacy(),
    creationDate = creationDate.toCalendar()
)

@Suppress("DEPRECATION")
private fun DomainVideo.State.toLegacy(): LegacyVideo.State = when (this) {
    is DomainVideo.State.Importing -> LegacyVideo.State.Importing(
        LegacyVideo.WorkerState.Working(UUID.randomUUID())
    )

    is DomainVideo.State.Information -> LegacyVideo.State.Information(
        isStreamable = isStreamable
    )

    is DomainVideo.State.Downloading -> LegacyVideo.State.Downloading(
        LegacyVideo.WorkerState.Working(UUID.randomUUID())
    )

    is DomainVideo.State.Downloaded -> LegacyVideo.State.Downloaded(
        internalLink = internalLink,
        fileSize = fileSize.toInt(),
        isStreamable = isStreamable
    )

    is DomainVideo.State.Failed -> LegacyVideo.State.Importing(
        LegacyVideo.WorkerState.Failed(sourceUrl)
    )
}

/**
 * Converts a legacy [LegacyVideo] to the domain [DomainVideo].
 */
fun LegacyVideo.toDomain(): DomainVideo = DomainVideo(
    id = id,
    videoId = videoId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    domain = domain,
    state = stateData.toDomain(),
    creationDate = creationDate.toKotlinInstant()
)

private fun LegacyVideo.State.toDomain(): DomainVideo.State = when (this) {
    is LegacyVideo.State.Importing -> when (val workerState = state) {
        is LegacyVideo.WorkerState.Failed -> DomainVideo.State.Failed(workerState.url)
        is LegacyVideo.WorkerState.Working -> DomainVideo.State.Importing
    }

    is LegacyVideo.State.Information -> DomainVideo.State.Information(
        isStreamable = isStreamable
    )

    is LegacyVideo.State.Downloading -> DomainVideo.State.Downloading

    is LegacyVideo.State.Downloaded -> DomainVideo.State.Downloaded(
        internalLink = internalLink,
        fileSize = fileSize.toLong(),
        isStreamable = isStreamable
    )
}

// -----------------------------------------------
// Helper functions
// -----------------------------------------------

private fun Instant.toCalendar(): Calendar = Calendar.getInstance().also {
    it.timeInMillis = toEpochMilliseconds()
}

private fun Calendar.toKotlinInstant(): Instant = Instant.fromEpochMilliseconds(timeInMillis)

private fun DayOfWeek.toDayOfWeekCompat(): DayOfWeekCompat = when (this) {
    DayOfWeek.MONDAY -> DayOfWeekCompat.MONDAY
    DayOfWeek.TUESDAY -> DayOfWeekCompat.TUESDAY
    DayOfWeek.WEDNESDAY -> DayOfWeekCompat.WEDNESDAY
    DayOfWeek.THURSDAY -> DayOfWeekCompat.THURSDAY
    DayOfWeek.FRIDAY -> DayOfWeekCompat.FRIDAY
    DayOfWeek.SATURDAY -> DayOfWeekCompat.SATURDAY
    DayOfWeek.SUNDAY -> DayOfWeekCompat.SUNDAY
}

private fun DayOfWeekCompat.toDayOfWeek(): DayOfWeek = when (this) {
    DayOfWeekCompat.MONDAY -> DayOfWeek.MONDAY
    DayOfWeekCompat.TUESDAY -> DayOfWeek.TUESDAY
    DayOfWeekCompat.WEDNESDAY -> DayOfWeek.WEDNESDAY
    DayOfWeekCompat.THURSDAY -> DayOfWeek.THURSDAY
    DayOfWeekCompat.FRIDAY -> DayOfWeek.FRIDAY
    DayOfWeekCompat.SATURDAY -> DayOfWeek.SATURDAY
    DayOfWeekCompat.SUNDAY -> DayOfWeek.SUNDAY
}

private fun LocalDate.toJavaDate(): Date {
    val instant = atStartOfDayIn(TimeZone.UTC)
    return Date(instant.toEpochMilliseconds())
}

private fun Date.toLocalDate(): LocalDate {
    val instant = Instant.fromEpochMilliseconds(time)
    return instant.toLocalDateTime(TimeZone.UTC).date
}