package net.turtton.ytalarm.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.adapter.VideoListAdapter
import net.turtton.ytalarm.ui.dialog.DialogExecuteProgress
import net.turtton.ytalarm.ui.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.ui.dialog.DialogRemoveVideo
import net.turtton.ytalarm.ui.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.ui.fragment.FragmentAllVideoList.Companion.updatePlaylistThumbnails
import net.turtton.ytalarm.ui.menu.AttachableMenuProvider
import net.turtton.ytalarm.ui.menu.MenuProviderContainer
import net.turtton.ytalarm.ui.menu.SelectionMenuObserver
import net.turtton.ytalarm.ui.selection.SelectionTrackerContainer
import net.turtton.ytalarm.ui.selection.TagKeyProvider
import net.turtton.ytalarm.util.extensions.createImportingPlaylist
import net.turtton.ytalarm.util.extensions.insertVideos
import net.turtton.ytalarm.util.extensions.takeStateIfNotFinished
import net.turtton.ytalarm.util.extensions.updateThumbnail
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker
import java.util.*

class FragmentVideoList :
    FragmentAbstractList(),
    VideoViewContainer,
    PlaylistViewContainer,
    SelectionTrackerContainer<Long>,
    MenuProviderContainer {
    lateinit var animFabAppear: Animation
    lateinit var animFabDisappear: Animation
    lateinit var animFabRotateForward: Animation
    lateinit var animFabRotateBackward: Animation
    lateinit var animFabRotateInfinity: Animation

    var isAddVideoFabRotated = false

    override var selectionTracker: SelectionTracker<Long>? = null
    lateinit var adapter: VideoListAdapter<FragmentVideoList>
    override var menuProvider: MenuProvider? = null

    private val args by navArgs<FragmentVideoListArgs>()
    val currentId = MutableStateFlow(0L)

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentId.update { args.playlistId }
        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = VideoListAdapter(this)
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "VideoListTracker",
            recyclerView,
            TagKeyProvider(recyclerView),
            VideoListAdapter.VideoListDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).build()
        adapter.tracker = selectionTracker

        selectionTracker?.addObserver(VideoSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker?.onRestoreInstanceState(it)
        }

        updateListObserver()

        lifecycleScope.launch {
            createMenuProvider(view)?.let {
                menuProvider = it
                activity?.addMenuProvider(it, viewLifecycleOwner)
            }
        }

        val activity = requireActivity() as MainActivity
        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

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
            val playlistType = playlistViewModel.getFromIdAsync(currentId.value).await()?.type
            launch(Dispatchers.Main) {
                when (playlistType) {
                    null,
                    is Playlist.Type.Importing,
                    is Playlist.Type.Original -> listenFabWithOriginalMode()
                    is Playlist.Type.CloudPlaylist -> listenFabWithSyncMode(
                        view.context,
                        playlistType
                    )
                }
                addVideoFab.show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker?.onSaveInstanceState(outState)
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

    private suspend fun createMenuProvider(view: View): MenuProvider? {
        val type = playlistViewModel.getFromIdAsync(currentId.value).await()?.type ?: return null
        return when (type) {
            is Playlist.Type.Importing -> null
            is Playlist.Type.Original -> null
            is Playlist.Type.CloudPlaylist -> AttachableMenuProvider(
                this,
                R.menu.menu_video_list_option_syncmode,
                R.id.menu_video_list_option_sync_rule to {
                    lifecycleScope.launch {
                        showSyncRuleSelectDialog(view)
                    }
                    true
                }
            )
        }
    }

    private suspend fun showSyncRuleSelectDialog(view: View) {
        val playlist = playlistViewModel.getFromIdAsync(currentId.value).await() ?: kotlin.run {
            val message = R.string.snackbar_error_failed_to_get_playlist
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        val current = playlist.type.let { it as? Playlist.Type.CloudPlaylist }
            ?.syncRule
            ?.ordinal
            ?: kotlin.run {
                val message = R.string.snackbar_error_unexpected_playlist_state
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return
            }
        AlertDialog.Builder(view.context)
            .setSingleChoiceItems(R.array.dialog_video_list_syncrule, current) { dialog, i ->
                when (i) {
                    DIALOG_ITEM_SYNC_ALWAYS_ADD -> {
                        lifecycleScope.launch {
                            updateCloudPlaylistSyncRule(view, Playlist.SyncRule.ALWAYS_ADD)
                            dialog.dismiss()
                        }
                    }
                    DIALOG_ITEM_SYNC_DELETE_IF_NOT_EXIST -> {
                        lifecycleScope.launch {
                            updateCloudPlaylistSyncRule(view, Playlist.SyncRule.DELETE_IF_NOT_EXIST)
                            dialog.dismiss()
                        }
                    }
                    else -> {
                        val message = R.string.snackbar_error_unknown
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }.show()
    }

    private suspend fun updateCloudPlaylistSyncRule(view: View, rule: Playlist.SyncRule) {
        val id = currentId.value
        val playlist = playlistViewModel.getFromIdAsync(id).await() ?: kotlin.run {
            val message = R.string.snackbar_error_failed_to_get_playlist
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            return
        }
        var type = playlist.type
        if (type !is Playlist.Type.CloudPlaylist) {
            val message = R.string.snackbar_error_unexpected_playlist_state
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        type = type.copy(syncRule = rule)
        playlistViewModel.update(playlist.copy(type = type))
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
                if (currentId.value == 0L) {
                    val newPlaylist = createImportingPlaylist()
                    val newId = playlistViewModel.insertAsync(newPlaylist).await()
                    currentId.update { newId }
                    updateListObserver()
                }
                launch(Dispatchers.Main) {
                    showVideoImportDialog(it, currentId.value)
                }
            }
        }
        addFromVideoFab.setOnClickListener { view ->
            animateFab(view)
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
                    .map { it.toDisplayData() }
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    DialogMultiChoiceVideo(targetVideos) { _, selectedId ->
                        // I don't know why but without lifecycleScope, it do not work
                        lifecycleScope.launch(Dispatchers.IO) {
                            var playlist = playlistViewModel.getFromIdAsync(currentId.value)
                                .await()
                                ?: kotlin.run {
                                    val icon = R.drawable.ic_download
                                    Playlist(thumbnail = Playlist.Thumbnail.Drawable(icon))
                                }
                            playlist = playlist.insertVideos(selectedId)
                            if (playlist.id == 0L) {
                                playlist.updateThumbnail()?.let { playlist = it }
                                val newId = playlistViewModel.insertAsync(playlist).await()
                                currentId.update { newId }
                                updateListObserver()
                            } else {
                                playlistViewModel.update(playlist)
                            }
                        }
                    }.show(childFragmentManager, "SelectVideos")
                }
            }
        }
    }

    private fun listenFabWithSyncMode(context: Context, typeState: Playlist.Type.CloudPlaylist) {
        val binding = (activity as MainActivity).binding
        val addVideoFab = binding.fabAddVideo
        addVideoFab.setImageResource(R.drawable.ic_sync)
        addVideoFab.isClickable = true

        val workManager = WorkManager.getInstance(context)
        lifecycleScope.launch {
            val workerId = typeState.workerId
            workManager.getWorkInfoById(workerId)
                .takeStateIfNotFinished()
                ?.let { _ ->
                    val workInfo = workManager.getWorkInfoByIdLiveData(workerId)
                    linkAnimation(addVideoFab, workInfo, true)
                }
        }

        addVideoFab.setOnClickListener {
            val animation = addVideoFab.animation
            if (animation == null || animation.hasEnded()) {
                addVideoFab.startAnimation(animFabRotateInfinity)
            }
            lifecycleScope.launch {
                val currentPlaylist = playlistViewModel.getFromIdAsync(currentId.value).await()
                val currentState = currentPlaylist?.type

                if (currentPlaylist == null || currentState !is Playlist.Type.CloudPlaylist) {
                    launch(Dispatchers.Main) {
                        val errorMessage = R.string.snackbar_error_unexpected_playlist_state
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }.join()
                    return@launch
                }

                workManager.getWorkInfoById(currentState.workerId)
                    .takeStateIfNotFinished()
                    ?.let { _ ->
                        launch(Dispatchers.Main) {
                            val message = R.string.snackbar_sync_is_running
                            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
                        }.join()
                        return@launch
                    }

                val playlistArray = longArrayOf(currentPlaylist.id)
                val (request, enqueueTask) =
                    VideoInfoDownloadWorker.prepareWorker(currentState.url, playlistArray)

                val newState = currentState.copy(workerId = request.id)
                playlistViewModel.update(currentPlaylist.copy(type = newState)).join()

                workManager.enqueueTask()
                val workInfo = workManager.getWorkInfoByIdLiveData(request.id)
                linkAnimation(addVideoFab, workInfo, false)
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
            val icAddVideo = R.drawable.ic_add_video
            fabAddVideo.setImageResource(icAddVideo)
            fabAddVideo.tag = icAddVideo

            fabAddVideoFromVideo.startAnimation(animFabDisappear)
            fabAddVideoFromVideo.isClickable = false

            fabAddVideoFromLink.startAnimation(animFabDisappear)
            fabAddVideoFromLink.isClickable = false

            isAddVideoFabRotated = false
        } else {
            fabAddVideo.startAnimation(animFabRotateForward)
            val icAdd = R.drawable.ic_add
            fabAddVideo.setImageResource(icAdd)
            fabAddVideo.tag = icAdd

            fabAddVideoFromVideo.startAnimation(animFabAppear)
            fabAddVideoFromVideo.isClickable = true

            fabAddVideoFromLink.startAnimation(animFabAppear)
            fabAddVideoFromLink.isClickable = true

            isAddVideoFabRotated = true
        }
    }

    private fun updateListObserver() {
        lifecycleScope.launch(Dispatchers.Main) mainThread@{
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

    private fun linkAnimation(
        fab: FloatingActionButton,
        workLiveData: LiveData<WorkInfo>,
        stopIfNull: Boolean
    ) {
        workLiveData.observe(viewLifecycleOwner) {
            if (!stopIfNull && it == null) return@observe
            if (it == null || it.state.isFinished) {
                lifecycleScope.launch(Dispatchers.Main) {
                    fab.clearAnimation()
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    fab.startAnimation(animFabRotateInfinity)
                }
            }
        }
    }

    companion object {
        const val DIALOG_ITEM_SYNC_ALWAYS_ADD = 0
        const val DIALOG_ITEM_SYNC_DELETE_IF_NOT_EXIST = 1
    }

    class VideoSelectionObserver(
        fragment: FragmentVideoList
    ) : SelectionMenuObserver<Long, FragmentVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_in_playlist,
            R.id.menu_video_list_in_pl_action_remove to to@{
                val selectionTracker = fragment.selectionTracker ?: return@to false
                val selection = selectionTracker.selection.distinct()
                DialogRemoveVideo { _, _ ->
                    fragment.lifecycleScope.launch {
                        fragment.updatePlaylistThumbnails(selection, fragment.currentId.value)
                    }
                    selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )
    )
}