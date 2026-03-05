package net.turtton.ytalarm.datasource.mapper

import net.turtton.ytalarm.datasource.entity.VideoEntity
import net.turtton.ytalarm.kernel.entity.Video

fun VideoEntity.toDomain(): Video = Video(
    id = id,
    videoId = videoId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    domain = domain,
    state = stateData.toDomain(),
    creationDate = creationDate.toKotlinInstant()
)

internal fun Video.toEntity(): VideoEntity = VideoEntity(
    id = id,
    videoId = videoId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    domain = domain,
    stateData = state.toEntity(),
    creationDate = creationDate.toCalendar()
)

private fun VideoEntity.State.toDomain(): Video.State = when (this) {
    is VideoEntity.State.Importing -> Video.State.Importing

    is VideoEntity.State.Information -> Video.State.Information(isStreamable = isStreamable)

    is VideoEntity.State.Downloading -> Video.State.Downloading

    is VideoEntity.State.Downloaded -> Video.State.Downloaded(
        internalLink = internalLink,
        fileSize = fileSize,
        isStreamable = isStreamable
    )

    is VideoEntity.State.Failed -> Video.State.Failed(sourceUrl = sourceUrl)
}

private fun Video.State.toEntity(): VideoEntity.State = when (this) {
    is Video.State.Importing -> VideoEntity.State.Importing

    is Video.State.Information -> VideoEntity.State.Information(isStreamable = isStreamable)

    is Video.State.Downloading -> VideoEntity.State.Downloading

    is Video.State.Downloaded -> VideoEntity.State.Downloaded(
        internalLink = internalLink,
        fileSize = fileSize,
        isStreamable = isStreamable
    )

    is Video.State.Failed -> VideoEntity.State.Failed(sourceUrl = sourceUrl)
}