package net.turtton.ytalarm.util.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter
import net.turtton.ytalarm.ui.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer

suspend fun <T> showInsertVideoToPlaylistsDialog(
    fragment: T,
    playlists: List<MultiChoiceVideoListAdapter.DisplayData<Long>>,
    selectedVideoId: Iterable<Long>,
    tag: String
) where T : Fragment, T : PlaylistViewContainer {
    DialogMultiChoiceVideo(playlists) { _, selectedId ->
        val targetIdList = selectedId.toList()
        val targetList = fragment.playlistViewModel.getFromIdsAsync(targetIdList)
        fragment.lifecycleScope.launch {
            val targetPlaylist = targetList.await()
            targetPlaylist.map {
                val videoList = it.videos.toMutableSet()
                videoList += selectedVideoId
                it.copy(videos = videoList.toList())
            }.also {
                fragment.playlistViewModel.update(it)
            }
            if (targetIdList.contains(0L)) {
                Playlist(videos = selectedVideoId.toList()).updateThumbnail()?.also {
                    @Suppress("DeferredResultUnused")
                    fragment.playlistViewModel.insertAsync(it)
                }
            }
        }
    }.show(fragment.childFragmentManager, tag)
}