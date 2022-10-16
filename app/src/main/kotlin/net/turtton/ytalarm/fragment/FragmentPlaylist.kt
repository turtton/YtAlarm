package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.PlaylistAdapter
import net.turtton.ytalarm.fragment.dialog.DialogRemoveVideo
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.util.AttachableMenuProvider
import net.turtton.ytalarm.util.SelectionMenuObserver
import net.turtton.ytalarm.util.SelectionTrackerContainer
import net.turtton.ytalarm.util.TagKeyProvider
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentPlaylist :
    FragmentAbstractList(),
    VideoViewContainer,
    PlaylistViewContainer,
    SelectionTrackerContainer<Long> {
    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }
    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }
    val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    override lateinit var selectionTracker: SelectionTracker<Long>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        activity.binding.fab.visibility = View.VISIBLE

        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        val adapter = PlaylistAdapter(this)
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "PlaylistTracker",
            recyclerView,
            TagKeyProvider(recyclerView),
            PlaylistAdapter.PlaylistDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).build()
        adapter.tracker = selectionTracker

        selectionTracker.addObserver(PlaylistSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker.onRestoreInstanceState(it)
        }

        playlistViewModel.allPlaylists.observe(requireActivity()) { list ->
            list?.let { playlists ->
                playlists.filter { playlist -> playlist.videos.isEmpty() }
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        playlistViewModel.delete(it)
                    }
                adapter.submitList(playlists)
            }
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            val action = FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment()
            findNavController().navigate(action)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
    }

    class PlaylistSelectionObserver(
        fragment: FragmentPlaylist
    ) : SelectionMenuObserver<Long, FragmentPlaylist>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_playlist_action,
            R.id.menu_playlist_action_remove to {
                val selection = fragment.selectionTracker.selection.toList()
                DialogRemoveVideo { _, _ ->
                    val alarmsAsync = fragment.alarmViewModel.getAllAlarmsAsync()
                    val async = fragment.playlistViewModel.getFromIdsAsync(selection)
                    fragment.lifecycleScope.launch {
                        val usingList = alarmsAsync.await().flatMap { it.playListId }.distinct()
                        val deletable = arrayListOf<Playlist>()
                        var detectUsage = false
                        async.await().forEach {
                            if (!usingList.contains(it.id)) {
                                deletable += it
                            } else {
                                detectUsage = true
                            }
                        }
                        if (deletable.isNotEmpty()) {
                            fragment.playlistViewModel.delete(deletable)
                        }
                        if (detectUsage) {
                            launch(Dispatchers.Main) {
                                Snackbar.make(
                                    fragment.requireView(),
                                    R.string.snackbar_detect_playlist_usage,
                                    1200
                                ).show()
                            }
                        }
                    }
                    fragment.selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "PlaylistRemoveDialog")
                true
            }
        )
    )
}