package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.PlaylistAdapter
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory

class FragmentPlaylist : FragmentAbstractList() {
    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerList.layoutManager = LinearLayoutManager(view.context)
        val adapter = PlaylistAdapter()
        binding.recyclerList.adapter = adapter

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
}