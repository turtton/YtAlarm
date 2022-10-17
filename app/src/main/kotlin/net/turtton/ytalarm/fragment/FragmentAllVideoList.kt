package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.adapter.VideoListAdapter
import net.turtton.ytalarm.fragment.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.fragment.dialog.DialogRemoveVideo
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.AttachableMenuProvider
import net.turtton.ytalarm.util.SelectionMenuObserver
import net.turtton.ytalarm.util.SelectionTrackerContainer
import net.turtton.ytalarm.util.TagKeyProvider
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
    override lateinit var selectionTracker: SelectionTracker<Long>

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

        selectionTracker.addObserver(VideoSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker.onRestoreInstanceState(it)
        }

        videoViewModel.allVideos.observe(requireActivity()) {
            it.let {
                it.filter { video ->
                    when (video.stateData) {
                        is Video.State.Importing,
                        is Video.State.Downloading -> true
                        else -> false
                    }
                }.forEach { video ->
                    lifecycleScope.launch {
                        when (val status = video.stateData) {
                            is Video.State.Importing -> status.workerId
                            is Video.State.Downloading -> status.workerId
                            else -> return@launch
                        }.let { workerId ->
                            WorkManager.getInstance(view.context)
                                .getWorkInfoById(workerId)
                                .await()
                                ?.state
                        }.also { state ->
                            if (state == null || state.isFinished) {
                                videoViewModel.delete(video)
                            }
                        }
                    }
                }
                adapter.submitList(it)
            }
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.visibility = View.VISIBLE

        fab.setOnClickListener { showVideoImportDialog(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
    }

    class VideoSelectionObserver(
        fragment: FragmentAllVideoList
    ) : SelectionMenuObserver<Long, FragmentAllVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_action,
            R.id.menu_video_list_action_add_to to { _ ->
                val selection = fragment.selectionTracker.selection
                fragment.lifecycleScope.launch {
                    val playlists = fragment.playlistViewModel.allPlaylistsAsync
                        .await()
                        .map { it.toDisplayData(fragment.videoViewModel) }
                    launch(Dispatchers.Main) {
                        DialogMultiChoiceVideo(playlists) { _, selectedId ->
                            val targetIdList = selectedId.toList()
                            val targetList = fragment.playlistViewModel
                                .getFromIdsAsync(targetIdList)
                            fragment.lifecycleScope.launch {
                                val targetPlaylist = targetList.await()
                                targetPlaylist.map {
                                    val videoList = it.videos.toMutableSet()
                                    videoList += selection
                                    it.copy(videos = videoList.toList())
                                }.also {
                                    fragment.playlistViewModel.update(it)
                                }
                            }
                        }.show(fragment.childFragmentManager, "PlaylistSelectDialog")
                    }
                }
                true
            },
            R.id.menu_video_list_action_remove to {
                val selection = fragment.selectionTracker.selection.toList()
                DialogRemoveVideo { _, _ ->
                    val videoViewModel = fragment.videoViewModel
                    val async = videoViewModel.getFromIdsAsync(selection)
                    fragment.lifecycleScope.launch {
                        val videos = async.await()
                        videoViewModel.delete(videos)
                        fragment.updatePlaylistThumbnails(selection)
                    }
                    fragment.selectionTracker.clearSelection()
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