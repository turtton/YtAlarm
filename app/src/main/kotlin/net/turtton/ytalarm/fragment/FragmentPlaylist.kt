package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.PlaylistAdapter
import net.turtton.ytalarm.fragment.dialog.DialogRemoveVideo
import net.turtton.ytalarm.util.AttachableMenuProvider
import net.turtton.ytalarm.util.SelectionMenuObserver
import net.turtton.ytalarm.util.SelectionTrackerContainer
import net.turtton.ytalarm.util.TagKeyProvider
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory

class FragmentPlaylist : FragmentAbstractList(), SelectionTrackerContainer<Long> {
    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override lateinit var selectionTracker: SelectionTracker<Long>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        val adapter = PlaylistAdapter()
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

        playlistViewModel.allPlaylists.observe(requireActivity()) {
            it?.let { adapter.submitList(it) }
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            Snackbar.make(view, "CreatePlaylist!!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            // TODO("open playlist name input dialog and create new Playlist in this fragment")
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
                val selection = fragment.selectionTracker.selection.map { it.toInt() }
                DialogRemoveVideo { _, _ ->
                    val async = fragment.playlistViewModel.getFromIdsAsync(selection)
                    fragment.lifecycleScope.launch {
                        val playlists = async.await()
                        fragment.playlistViewModel.delete(playlists)
                    }
                    fragment.selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "PlaylistRemoveDialog")
                true
            }
        )
    )
}