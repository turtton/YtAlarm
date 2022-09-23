package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import androidx.work.await
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.util.AttachableMenuProvider
import net.turtton.ytalarm.util.SelectionMenuObserver
import net.turtton.ytalarm.util.SelectionTrackerContainer
import net.turtton.ytalarm.util.TagKeyProvider
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker
import java.util.*

class FragmentVideoList :
    FragmentAbstractList(), VideoViewContainer, SelectionTrackerContainer<String> {
    lateinit var animFabAppear: Animation
    lateinit var animFabDisappear: Animation
    lateinit var animFabRotateForward: Animation
    lateinit var animFabRotateBackward: Animation
    lateinit var animFabRotateInfinity: Animation

    var isAddVideoFabRotated = false

    override lateinit var selectionTracker: SelectionTracker<String>
    lateinit var adapter: VideoListAdapter

    private val args by navArgs<FragmentVideoListArgs>()
    val currentId = MutableStateFlow(-1L)

    private var currentSyncWorker = MutableStateFlow<UUID?>(null)

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentId.update { args.playlistId }
        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = VideoListAdapter()
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "VideoListTracker",
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

        updateListObserver()

        val activity = requireActivity() as MainActivity

        animFabAppear = AnimationUtils.loadAnimation(activity, R.anim.fab_appear)
        animFabDisappear = AnimationUtils.loadAnimation(activity, R.anim.fab_disappear)
        animFabRotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        animFabRotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
        animFabRotateInfinity = AnimationUtils.loadAnimation(activity, R.anim.rotate_infinity)

        val binding = activity.binding
        binding.fab.visibility = View.GONE

        isAddVideoFabRotated = false
        val addVideoFab = binding.fabAddVideo
        val addFromLinkFab = binding.fabAddVideoFromLink
        val addFromVideoFab = binding.fabAddVideoFromVideo

        addFromLinkFab.visibility = View.INVISIBLE
        addFromLinkFab.isClickable = false
        addFromVideoFab.visibility = View.INVISIBLE
        addFromVideoFab.isClickable = false

        lifecycleScope.launch {
            val playlist = playlistViewModel.getFromIdAsync(currentId.value).await()
            launch(Dispatchers.Main) {
                if (playlist?.originUrl == null) {
                    listenFabWithOriginalMode()
                } else {
                    listenFabWithSyncMode(playlist.id!!, playlist.originUrl)
                }
                addVideoFab.show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val binding = (requireActivity() as MainActivity).binding

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

    private fun listenFabWithOriginalMode() {
        val binding = (activity as MainActivity).binding
        val addVideoFab = binding.fabAddVideo
        val addFromLinkFab = binding.fabAddVideoFromLink
        val addFromVideoFab = binding.fabAddVideoFromVideo

        addVideoFab.setOnClickListener(::animateFab)
        addFromLinkFab.setOnClickListener {
            animateFab(it)
            lifecycleScope.launch {
                val playlist = playlistViewModel.getFromIdAsync(currentId.value)
                    .await()
                    ?: Playlist()
                if (playlist.id == null) {
                    val newId = playlistViewModel.insertAsync(playlist).await()
                    currentId.update { newId }
                    updateListObserver()
                }
                launch(Dispatchers.Main) {
                    showVideoImportDialog(it, currentId.value)
                }
            }
        }
        addFromVideoFab.setOnClickListener {
            animateFab(it)
            val progressDialog =
                DialogExecuteProgress(R.string.dialog_execute_progress_title_loading)
            progressDialog.show(childFragmentManager, "LoadPlaylist")
            lifecycleScope.launch {
                val currentVideo = playlistViewModel.getFromIdAsync(currentId.value)
                    .await()
                    ?.videos
                    ?: emptyList()
                val targetVideos = videoViewModel.getExceptIdsAsync(currentVideo)
                    .await()
                    .map { video -> video.toDisplayData() }
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    DialogMultiChoiceVideo(targetVideos) { _, selectedId ->
                        // I don't know why but without lifecycleScope, it do not work
                        lifecycleScope.launch(Dispatchers.IO) {
                            val playlist = playlistViewModel.getFromIdAsync(currentId.value)
                                .await()
                                ?: Playlist()
                            // Converts set to avoid duplicating ids
                            val newVideoTargets = (playlist.videos + selectedId).distinct()
                            val newPlaylist = playlist.copy(videos = newVideoTargets)
                            if (playlist.id == null) {
                                val newId = playlistViewModel.insertAsync(newPlaylist).await()
                                currentId.update { newId }
                                updateListObserver()
                            } else {
                                playlistViewModel.update(newPlaylist)
                            }
                        }
                    }.show(childFragmentManager, "SelectVideos")
                }
            }
        }
    }

    private fun listenFabWithSyncMode(playlistId: Long, originalUrl: String) {
        val binding = (activity as MainActivity).binding
        val addVideoFab = binding.fabAddVideo
        addVideoFab.setImageResource(R.drawable.ic_sync)
        addVideoFab.isClickable = true

        addVideoFab.setOnClickListener {
            val animation = addVideoFab.animation
            if (animation == null || animation.hasEnded()) {
                addVideoFab.startAnimation(animFabRotateInfinity)
            }
            lifecycleScope.launch {
                val syncWorkerId = currentSyncWorker.value
                if (syncWorkerId != null) {
                    val worker = WorkManager.getInstance(it.context)
                        .getWorkInfoById(syncWorkerId).await()
                    if (worker.state.isFinished.not()) {
                        return@launch
                    }
                }

                val currentWork = WorkManager.getInstance(it.context)
                    .getWorkInfosForUniqueWork(VideoInfoDownloadWorker.WORKER_ID).await()
                if (currentWork.any { !it.state.isFinished }) {
                    Log.i("current", currentWork.toString())
                    launch(Dispatchers.Main) {
                        addVideoFab.clearAnimation()
                        Snackbar.make(it, R.string.snackbar_sync_is_running, 900).show()
                    }.join()
                    return@launch
                }

                val worker =
                    VideoInfoDownloadWorker.registerWorker(it.context, originalUrl, playlistId)
                currentSyncWorker.update { worker.id }
                WorkManager.getInstance(it.context)
                    .getWorkInfoByIdLiveData(worker.id)
                    .observe(viewLifecycleOwner) {
                        if (it.state.isFinished) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                addVideoFab.clearAnimation()
                            }
                        }
                    }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun animateFab(ignore: View) {
        val binding = (requireActivity() as MainActivity).binding
        val fabAddVideoFromVideo = binding.fabAddVideoFromVideo
        val fabAddVideo = binding.fabAddVideo
        val fabAddVideoFromLink = binding.fabAddVideoFromLink
        if (isAddVideoFabRotated) {
            fabAddVideo.startAnimation(animFabRotateBackward)
            fabAddVideo.setImageResource(R.drawable.ic_add_video)

            fabAddVideoFromVideo.startAnimation(animFabDisappear)
            fabAddVideoFromVideo.isClickable = false

            fabAddVideoFromLink.startAnimation(animFabDisappear)
            fabAddVideoFromLink.isClickable = false

            isAddVideoFabRotated = false
        } else {
            fabAddVideo.startAnimation(animFabRotateForward)
            fabAddVideo.setImageResource(R.drawable.ic_add)

            fabAddVideoFromVideo.startAnimation(animFabAppear)
            fabAddVideoFromVideo.isClickable = true

            fabAddVideoFromLink.startAnimation(animFabAppear)
            fabAddVideoFromLink.isClickable = true

            isAddVideoFabRotated = true
        }
    }

    private fun updateListObserver() {
        lifecycleScope.launch(Dispatchers.Main) {
            playlistViewModel.getFromId(currentId.value).observe(viewLifecycleOwner) { playlist ->
                playlist?.videos?.also { videos ->
                    videoViewModel.getFromIds(videos)
                        .observe(viewLifecycleOwner) { list ->
                            list?.also {
                                adapter.submitList(it)
                            }
                        }
                }
            }
        }
    }

    class VideoSelectionObserver(
        fragment: FragmentVideoList
    ) : SelectionMenuObserver<String, FragmentVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_in_playlist,
            R.id.menu_video_list_in_pl_action_remove to {
                val selection = fragment.selectionTracker.selection.toSet()
                DialogRemoveVideo { _, _ ->
                    val async = fragment.playlistViewModel.getFromIdAsync(fragment.currentId.value)
                    fragment.lifecycleScope.launch {
                        val playlist = async.await() ?: return@launch
                        val videoList = playlist.videos.toMutableSet()
                        videoList.removeAll(selection)
                        val newList = playlist.copy(videos = videoList.toList())
                        if (playlist.id == null) {
                            @Suppress("DeferredResultUnused")
                            fragment.playlistViewModel.insertAsync(newList)
                        } else {
                            fragment.playlistViewModel.update(newList)
                        }
                    }
                    fragment.selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )
    )
}