package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.adapter.VideoListAdapter
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList : FragmentAbstractList(), VideoViewContainer {

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory((requireActivity().application as YtApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerList.layoutManager = LinearLayoutManager(view.context)
        val adapter = VideoListAdapter()
        binding.recyclerList.adapter = adapter

        videoViewModel.allVideos.observe(requireActivity()) {
            it.let { adapter.submitList(it) }
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.visibility = View.VISIBLE

        fab.setOnClickListener { showVideoImportDialog(it) }
    }
}