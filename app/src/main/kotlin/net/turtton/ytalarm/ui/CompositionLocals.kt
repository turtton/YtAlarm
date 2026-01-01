package net.turtton.ytalarm.ui

import androidx.compose.runtime.compositionLocalOf
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModel

/**
 * PlaylistViewModelを提供するCompositionLocal
 *
 * ViewModelは状態を持つため、compositionLocalOf（staticではない）を使用し、
 * 変更時にRecompositionが適切にトリガーされるようにする。
 */
val LocalPlaylistViewModel = compositionLocalOf<PlaylistViewModel> {
    error("PlaylistViewModel not provided")
}

/**
 * VideoViewModelを提供するCompositionLocal
 *
 * ViewModelは状態を持つため、compositionLocalOf（staticではない）を使用し、
 * 変更時にRecompositionが適切にトリガーされるようにする。
 */
val LocalVideoViewModel = compositionLocalOf<VideoViewModel> {
    error("VideoViewModel not provided")
}

/**
 * VideoPlayerLoadingResourceContainerを提供するCompositionLocal
 *
 * ResourceContainerは状態を持つため、compositionLocalOf（staticではない）を使用し、
 * 変更時にRecompositionが適切にトリガーされるようにする。
 *
 * nullableにすることで、AlarmActivity以外（MainActivity等）で使用時もクラッシュしない。
 * テスト用IdlingResourceが必要な場合のみ、CompositionLocalProviderで提供する。
 */
val LocalVideoPlayerResourceContainer = compositionLocalOf<VideoPlayerLoadingResourceContainer?> {
    null
}