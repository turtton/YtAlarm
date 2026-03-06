package net.turtton.ytalarm.util.extensions

import androidx.annotation.DrawableRes
import net.turtton.ytalarm.R
import net.turtton.ytalarm.kernel.entity.Playlist

/**
 * Returns a drawable resource ID for this [Playlist.Thumbnail], or null if no drawable is available.
 *
 * - [Playlist.Thumbnail.None]: returns [R.drawable.ic_no_image]
 * - [Playlist.Thumbnail.Video]: returns null (video thumbnails are loaded via Coil from URL)
 */
@DrawableRes
fun Playlist.Thumbnail.toDrawableRes(): Int? = when (this) {
    is Playlist.Thumbnail.None -> R.drawable.ic_no_image
    is Playlist.Thumbnail.Video -> null
}