package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
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
import net.turtton.ytalarm.util.AttachableMenuProvider
import net.turtton.ytalarm.util.SelectionMenuObserver
import net.turtton.ytalarm.util.SelectionTrackerContainer
import net.turtton.ytalarm.util.TagKeyProvider
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList :
    FragmentAbstractList(), VideoViewContainer, SelectionTrackerContainer<String> {
    override lateinit var selectionTracker: SelectionTracker<String>

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.binding.fab.show()

        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        val adapter = VideoListAdapter()
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "AllVideoListTracker",
            recyclerView,
            TagKeyProvider(recyclerView),
            VideoListAdapter.VideoListDetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).build()
        adapter.tracker = selectionTracker

        selectionTracker.addObserver(VideoSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker.onRestoreInstanceState(it)
        }

        videoViewModel.allVideos.observe(requireActivity()) {
            it.let { adapter.submitList(it) }
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
    ) : SelectionMenuObserver<String, FragmentAllVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_action,
            R.id.menu_video_list_action_add_to to { _ ->
                val selection = fragment.selectionTracker.selection
                fragment.lifecycleScope.launch {
                    val playlists = fragment.playlistViewModel.allPlaylistsAsync
                        .await()
                        .map { it.toDisplayData() }
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
                        val playlists = fragment.playlistViewModel
                            .getFromContainsVideoIdsAsync(selection)
                            .await()
                        val newList = playlists.map {
                            it.copy(
                                videos = it.videos.filterNot { video -> selection.contains(video) }
                            )
                        }.map {
                            if (videos.any { video -> video.thumbnailUrl == it.thumbnailUrl }) {
                                val newVideoId = it.videos.firstOrNull() ?: ""
                                videoViewModel.getFromIdAsync(newVideoId).await()?.let { video ->
                                    it.copy(thumbnailUrl = video.thumbnailUrl)
                                } ?: it
                            } else {
                                it
                            }
                        }
                        fragment.playlistViewModel.update(newList)
                    }
                    fragment.selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )
    )
}