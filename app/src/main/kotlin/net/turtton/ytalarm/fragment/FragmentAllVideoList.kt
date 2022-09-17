package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
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
import net.turtton.ytalarm.util.StringKeyProvider
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList : FragmentAbstractList(), VideoViewContainer {
    lateinit var selectionTracker: SelectionTracker<String>

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        val adapter = VideoListAdapter()
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "AllVideoListTracker",
            recyclerView,
            StringKeyProvider(recyclerView),
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
        private val fragment: FragmentAllVideoList
    ) : SelectionObserver<String>() {
        private val provider = AttachableMenuProvider(
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
                val selection = fragment.selectionTracker.selection
                DialogRemoveVideo { _, _ ->
                    val async = fragment.videoViewModel.getFromIdsAsync(selection.toList())
                    fragment.lifecycleScope.launch {
                        val videos = async.await()
                        fragment.videoViewModel.delete(videos)
                        val ids = videos.map { it.id }
                        val playlists = fragment.playlistViewModel
                            .getFromContainsVideoIdsAsync(ids)
                            .await()
                        playlists.forEach { playlist ->
                            playlist.videos = playlist.videos.toMutableList().apply {
                                removeIf {
                                    ids.contains(it)
                                }
                            }
                        }
                        fragment.playlistViewModel.update(playlists)
                    }
                    fragment.selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )

        var isAdded = false

        override fun onSelectionChanged() {
            if (fragment.selectionTracker.hasSelection()) {
                if (!isAdded) {
                    fragment.requireActivity()
                        .addMenuProvider(provider, fragment.viewLifecycleOwner)
                    isAdded = true
                }
            } else {
                fragment.requireActivity().removeMenuProvider(provider)
                isAdded = false
            }
        }

        override fun onSelectionRestored() {
            onSelectionChanged()
        }
    }
}