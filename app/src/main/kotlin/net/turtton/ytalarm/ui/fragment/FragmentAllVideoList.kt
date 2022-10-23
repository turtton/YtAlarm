package net.turtton.ytalarm.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.adapter.VideoListAdapter
import net.turtton.ytalarm.ui.dialog.DialogRemoveVideo
import net.turtton.ytalarm.ui.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.ui.menu.AttachableMenuProvider
import net.turtton.ytalarm.ui.menu.SelectionMenuObserver
import net.turtton.ytalarm.ui.selection.SelectionTrackerContainer
import net.turtton.ytalarm.ui.selection.TagKeyProvider
import net.turtton.ytalarm.util.extensions.collectGarbage
import net.turtton.ytalarm.util.extensions.deleteVideos
import net.turtton.ytalarm.util.extensions.showInsertVideoToPlaylistsDialog
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList :
    FragmentAbstractList(),
    VideoViewContainer,
    PlaylistViewContainer,
    SelectionTrackerContainer<Long> {
    override var selectionTracker: SelectionTracker<Long>? = null

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.binding.fab.show()

        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        val adapter = VideoListAdapter(this)
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "AllVideoListTracker",
            recyclerView,
            TagKeyProvider(recyclerView),
            VideoListAdapter.VideoListDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).build()
        adapter.tracker = selectionTracker

        selectionTracker?.addObserver(VideoSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker?.onRestoreInstanceState(it)
        }

        videoViewModel.allVideos.observe(requireActivity()) { videoList ->
            if (videoList == null) return@observe
            lifecycleScope.launch {
                val garbage = videoList.collectGarbage(WorkManager.getInstance(view.context))
                if (garbage.isNotEmpty()) {
                    val updatedPlaylists = playlistViewModel.allPlaylistsAsync
                        .await()
                        .filter { playlist ->
                            garbage.any { playlist.videos.contains(it.id) }
                        }.deleteVideos(garbage.map { it.id })
                    playlistViewModel.update(updatedPlaylists)
                    videoViewModel.delete(garbage)
                }
            }
            adapter.submitList(videoList)
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.visibility = View.VISIBLE

        fab.setOnClickListener { showVideoImportDialog(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker?.onSaveInstanceState(outState)
    }

    class VideoSelectionObserver(
        fragment: FragmentAllVideoList
    ) : SelectionMenuObserver<Long, FragmentAllVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_action,
            R.id.menu_video_list_action_add_to to to@{ _ ->
                val selection = fragment.selectionTracker?.selection ?: return@to false
                fragment.lifecycleScope.launch {
                    val playlists = fragment.playlistViewModel.allPlaylistsAsync
                        .await()
                        .map { it.toDisplayData(fragment.videoViewModel) }
                        .toMutableList()
                        .addNewPlaylist()

                    showInsertVideoToPlaylistsDialog(
                        fragment,
                        playlists,
                        selection,
                        "PlaylistSelectDialog"
                    )
                }
                true
            },
            R.id.menu_video_list_action_remove to to@{
                val selectionTracker = fragment.selectionTracker ?: return@to false
                val selection = selectionTracker.selection.toList()
                DialogRemoveVideo { _, _ ->
                    val videoViewModel = fragment.videoViewModel
                    val async = videoViewModel.getFromIdsAsync(selection)
                    fragment.lifecycleScope.launch {
                        val videos = async.await()
                        videoViewModel.delete(videos)
                        fragment.updatePlaylistThumbnails(selection)
                    }
                    selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )
    )

    companion object {
        suspend fun <T> T.updatePlaylistThumbnails(
            removedVideoIds: List<Long>,
            targetPlaylistId: Long? = null
        ) where T : LifecycleOwner, T : PlaylistViewContainer, T : VideoViewContainer {
            val playlists = targetPlaylistId?.let {
                playlistViewModel
                    .getFromIdAsync(it)
                    .await()
                    ?.let { playlist -> listOf(playlist) }
                    ?: emptyList()
            } ?: playlistViewModel.allPlaylistsAsync
                .await()
                .filter {
                    it.videos.any { videoId -> removedVideoIds.contains(videoId) }
                }
            val newList = playlists.map {
                it.copy(
                    videos = it.videos.filterNot { video -> removedVideoIds.contains(video) }
                )
            }.map {
                val removedVideos = videoViewModel.getFromIdsAsync(removedVideoIds).await()
                val thumbnail = it.thumbnail
                val shouldUpdate = thumbnail is Playlist.Thumbnail.Video &&
                    removedVideos.any { video ->
                        video.id == thumbnail.id
                    }
                if (shouldUpdate) {
                    it.videos.firstOrNull()?.let { newVideoId ->
                        videoViewModel.getFromIdAsync(newVideoId).await()
                            ?.let { video ->
                                it.copy(thumbnail = Playlist.Thumbnail.Video(video.id))
                            }
                    } ?: it
                } else {
                    it
                }
            }
            playlistViewModel.update(newList)
        }
    }
}