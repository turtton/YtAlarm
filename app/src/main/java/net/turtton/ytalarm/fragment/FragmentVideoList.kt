package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
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
import net.turtton.ytalarm.fragment.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.viewmodel.*

class FragmentVideoList : FragmentAbstractList(), VideoViewContainer {
    lateinit var animFabAppear: Animation
    lateinit var animFabDisappear: Animation
    lateinit var animFabRotateForward: Animation
    lateinit var animFabRotateBackward: Animation

    var isAddVideoFabRotated = false

    private val args by navArgs<FragmentVideoListArgs>()

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerList.layoutManager = LinearLayoutManager(view.context)
        val adapter = VideoListAdapter()
        binding.recyclerList.adapter = adapter

        val id = args.playlistId
        playlistViewModel.getFromId(id).observe(requireActivity()) { playlist ->
            playlist?.videos?.also { videos ->
                videoViewModel.getFromIds(videos)
                    .observe(requireActivity()) { list ->
                        list?.also {
                            adapter.submitList(it)
                        }
                    }
            }
        }

        val activity = requireActivity() as MainActivity

        animFabAppear = AnimationUtils.loadAnimation(activity, R.anim.fab_appear)
        animFabDisappear = AnimationUtils.loadAnimation(activity, R.anim.fab_disappear)
        animFabRotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        animFabRotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)

        val binding = activity.binding
        binding.fab.visibility = View.GONE

        isAddVideoFabRotated = false
        val addVideoFab = binding.fabAddVideo
        val addFromLinkFab = binding.fabAddVideoFromLink
        val addFromVideoFab = binding.fabAddVideoFromVideo
        addVideoFab.visibility = View.VISIBLE

        addFromLinkFab.visibility = View.INVISIBLE
        addFromLinkFab.isClickable = false
        addFromVideoFab.visibility = View.INVISIBLE
        addFromVideoFab.isClickable = false

        addVideoFab.setOnClickListener(::animateFab)
        addFromLinkFab.setOnClickListener {
            animateFab(it)
            showVideoImportDialog(it) {
                lifecycleScope.launch {
                    val playlist = playlistViewModel.getFromIdAsync(id).await()
                    val newList = playlist.videos + it.id
                    playlistViewModel.update(playlist.copy(videos = newList))
                }
            }
        }
        addFromVideoFab.setOnClickListener {
            animateFab(it)
            val progressDialog =
                DialogExecuteProgress(R.string.dialog_execute_progress_title_loading)
            progressDialog.show(childFragmentManager, "LoadPlaylist")
            lifecycleScope.launch {
                val currentVideo = playlistViewModel.getFromIdAsync(id).await().videos
                val targetVideos = videoViewModel.getExceptIdsAsync(currentVideo)
                    .await()
                    .map { video -> video.toDisplayData() }
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    DialogMultiChoiceVideo(targetVideos) { _, selectedId ->
                        // I don't know why but without lifecycleScope, it do not work
                        lifecycleScope.launch(Dispatchers.IO) {
                            val playlist = playlistViewModel.getFromIdAsync(id).await()
                            // Converts set to avoid duplicating ids
                            val newVideoTargets = (playlist.videos + selectedId).toSet().toList()
                            playlistViewModel.update(playlist.copy(videos = newVideoTargets))
                        }
                    }.show(childFragmentManager, "SelectVideos")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val binding = (requireActivity() as MainActivity).binding
        binding.fab.visibility = View.VISIBLE
        binding.fab.shrink()

        val fabAddVideo = binding.fabAddVideo
        fabAddVideo.setImageResource(R.drawable.ic_add_video)
        fabAddVideo.clearAnimation()
        fabAddVideo.visibility = View.GONE

        val fabAddVideoFromVideo = binding.fabAddVideoFromVideo
        fabAddVideoFromVideo.visibility = View.GONE
        fabAddVideoFromVideo.clearAnimation()
        val fabAddVideoFromLink = binding.fabAddVideoFromLink
        fabAddVideoFromLink.visibility = View.GONE
        fabAddVideoFromLink.clearAnimation()
    }

    private fun animateFab(ignore: View) {
        val binding = (requireActivity() as MainActivity).binding
        if (isAddVideoFabRotated) {
            val fabAddVideo = binding.fabAddVideo
            fabAddVideo.startAnimation(animFabRotateBackward)
            fabAddVideo.setImageResource(R.drawable.ic_add_video)

            val fabAddVideoFromVideo = binding.fabAddVideoFromVideo
            fabAddVideoFromVideo.startAnimation(animFabDisappear)
            fabAddVideoFromVideo.isClickable = false

            val fabAddVideoFromLink = binding.fabAddVideoFromLink
            fabAddVideoFromLink.startAnimation(animFabDisappear)
            fabAddVideoFromLink.isClickable = false

            isAddVideoFabRotated = false
        } else {
            val fabAddVideo = binding.fabAddVideo
            fabAddVideo.startAnimation(animFabRotateForward)
            fabAddVideo.setImageResource(R.drawable.ic_add)

            val fabAddVideoFromVideo = binding.fabAddVideoFromVideo
            fabAddVideoFromVideo.startAnimation(animFabAppear)
            fabAddVideoFromVideo.isClickable = true

            val fabAddVideoFromLink = binding.fabAddVideoFromLink
            fabAddVideoFromLink.startAnimation(animFabAppear)
            fabAddVideoFromLink.isClickable = true

            isAddVideoFabRotated = true
        }
    }
}