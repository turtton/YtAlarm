package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.MenuHost
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
import net.turtton.ytalarm.fragment.dialog.DialogExecuteProgress
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
            VIDEO_SELECT_TRACKER,
            recyclerView,
            StringKeyProvider(recyclerView),
            VideoListAdapter.VideoListDetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).build()
        adapter.tracker = selectionTracker

        val menuHost = requireActivity() as MenuHost
        selectionTracker.addObserver(object : SelectionObserver<String>() {
            val provider = AttachableMenuProvider(
                this@FragmentAllVideoList,
                R.menu.menu_video_list_action,
                R.id.menu_video_list_action_add_to to { _ ->
                    val selection = selectionTracker.selection
                    val loadingTitle = R.string.dialog_execute_progress_title_loading
                    val progressDialog = DialogExecuteProgress(loadingTitle)
                    progressDialog.show(childFragmentManager, "LoadPlaylistDialog")
                    lifecycleScope.launch {
                        val playlists = playlistViewModel.allPlaylistsAsync
                            .await()
                            .map { it.toDisplayData() }
                        launch(Dispatchers.Main) {
                            progressDialog.dismissNow()
                            DialogMultiChoiceVideo(playlists) { _, selectedId ->
                                val targetIdList = selectedId.toList()
                                val targetList = playlistViewModel.getFromIdsAsync(targetIdList)
                                lifecycleScope.launch {
                                    val targetPlaylist = targetList.await()
                                    targetPlaylist.map {
                                        val videoList = it.videos.toMutableSet()
                                        videoList += selection
                                        it.copy(videos = videoList.toList())
                                    }.also {
                                        playlistViewModel.update(it)
                                    }
                                }
                            }.show(childFragmentManager, "PlaylistSelectDialog")
                        }
                    }
                    true
                },
                R.id.menu_video_list_action_remove to {
                    val selection = selectionTracker.selection
                    DialogRemoveVideo { _, _ ->
                        val async = videoViewModel.getFromIdsAsync(selection.toList())
                        lifecycleScope.launch {
                            val videos = async.await()
                            videoViewModel.delete(videos)
                            val ids = videos.map { it.id }
                            val playlists = playlistViewModel
                                .getFromContainsVideoIdsAsync(ids)
                                .await()
                            playlists.forEach { playlist ->
                                playlist.videos = playlist.videos.toMutableList().apply {
                                    removeIf {
                                        ids.contains(it)
                                    }
                                }
                            }
                            playlistViewModel.update(playlists)
                        }
                        selectionTracker.clearSelection()
                    }.show(childFragmentManager, "VideoRemoveDialog")
                    true
                }
            )

            var isAdded = false

            override fun onSelectionChanged() {
                if (selectionTracker.hasSelection()) {
                    if (!isAdded) {
                        menuHost.addMenuProvider(provider, viewLifecycleOwner)
                        isAdded = true
                    }
                } else {
                    menuHost.removeMenuProvider(provider)
                    isAdded = false
                }
            }

            override fun onSelectionRestored() {
                onSelectionChanged()
            }
        })

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

    companion object {
        const val VIDEO_SELECT_TRACKER = "AllVideoListTracker"
    }
}