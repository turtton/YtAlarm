package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.adapter.VideoListAdapter
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.util.StringKeyProvider
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList : FragmentAbstractList(), VideoViewContainer {
    lateinit var selectionTracker: SelectionTracker<String>

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory((requireActivity().application as YtApplication).repository)
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