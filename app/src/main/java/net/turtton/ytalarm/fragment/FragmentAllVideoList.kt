package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.adapter.VideoListAdapter
import net.turtton.ytalarm.fragment.dialog.DialogExecuteProgress
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

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