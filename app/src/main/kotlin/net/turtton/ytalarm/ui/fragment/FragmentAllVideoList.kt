package net.turtton.ytalarm.ui.fragment

import android.app.AlertDialog
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.adapter.VideoListAdapter
import net.turtton.ytalarm.ui.dialog.DialogRemoveVideo
import net.turtton.ytalarm.ui.dialog.DialogUrlInput.Companion.showVideoImportDialog
import net.turtton.ytalarm.ui.menu.AttachableMenuProvider
import net.turtton.ytalarm.ui.menu.MenuProviderContainer
import net.turtton.ytalarm.ui.menu.SelectionMenuObserver
import net.turtton.ytalarm.ui.selection.SelectionTrackerContainer
import net.turtton.ytalarm.ui.selection.TagKeyProvider
import net.turtton.ytalarm.util.extensions.collectGarbage
import net.turtton.ytalarm.util.extensions.deleteVideos
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.showInsertVideoToPlaylistsDialog
import net.turtton.ytalarm.util.extensions.showMessageIfNull
import net.turtton.ytalarm.util.extensions.videoOrderRule
import net.turtton.ytalarm.util.extensions.videoOrderUp
import net.turtton.ytalarm.util.order.VideoOrder
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAllVideoList :
    FragmentAbstractList(),
    VideoViewContainer,
    PlaylistViewContainer,
    SelectionTrackerContainer<Long>,
    MenuProviderContainer {
    override var selectionTracker: SelectionTracker<Long>? = null
    override var menuProvider: MenuProvider? = null

    lateinit var adapter: VideoListAdapter<FragmentAllVideoList>

    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.binding.fab.show()

        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = VideoListAdapter(this)
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "AllVideoListTracker",
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

        updateObserver()

        AllVideoListMenuProvider(this).also {
            menuProvider = it
            activity.addMenuProvider(it, viewLifecycleOwner)
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.visibility = View.VISIBLE

        fab.setOnClickListener { showVideoImportDialog(it) }
    }

    private fun updateObserver() {
        videoViewModel.allVideos.observe(requireActivity()) { videoList ->
            if (videoList == null) return@observe
            lifecycleScope.launch {
                val context = context ?: return@launch
                val garbage = videoList.collectGarbage(WorkManager.getInstance(context))
                if (garbage.isNotEmpty()) {
                    val updatedPlaylists = playlistViewModel.allPlaylistsAsync
                        .await()
                        .filter { playlist ->
                            garbage.any { playlist.videos.contains(it.id) }
                        }.deleteVideos(garbage.map { it.id })
                    playlistViewModel.update(updatedPlaylists)
                    videoViewModel.delete(garbage)
                }
            }
            val targetList = videoList.toMutableList()
            val preferences = activity?.privatePreferences ?: kotlin.run {
                val view = view ?: return@observe
                val message =
                    R.string.snackbar_error_failed_to_access_settings_data
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@observe
            }
            when (preferences.videoOrderRule) {
                VideoOrder.TITLE -> targetList.sortBy { it.title }
                VideoOrder.CREATION_DATE -> targetList.sortBy {
                    it.creationDate.timeInMillis
                }
            }
            if (!preferences.videoOrderUp) {
                targetList.reverse()
            }
            adapter.submitList(targetList)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker?.onSaveInstanceState(outState)
    }

    class VideoSelectionObserver(
        fragment: FragmentAllVideoList
    ) : SelectionMenuObserver<Long, FragmentAllVideoList>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_video_list_action,
            R.id.menu_video_list_action_add_to to to@{ _ ->
                val selection = fragment.selectionTracker?.selection ?: return@to false
                fragment.lifecycleScope.launch {
                    val playlists = fragment.playlistViewModel.allPlaylistsAsync
                        .await()
                        .map { it.toDisplayData(fragment.videoViewModel) }
                        .toMutableList()
                        .addNewPlaylist()

                    showInsertVideoToPlaylistsDialog(
                        fragment,
                        playlists,
                        selection,
                        "PlaylistSelectDialog"
                    )
                }
                true
            },
            R.id.menu_video_list_action_remove to to@{
                val selectionTracker = fragment.selectionTracker ?: return@to false
                val selection = selectionTracker.selection.toList()
                DialogRemoveVideo { _, _ ->
                    val videoViewModel = fragment.videoViewModel
                    val async = videoViewModel.getFromIdsAsync(selection)
                    fragment.lifecycleScope.launch {
                        val videos = async.await()
                        videoViewModel.delete(videos)
                        fragment.updatePlaylistThumbnails(selection)
                    }
                    selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "VideoRemoveDialog")
                true
            }
        )
    )

    companion object {
        suspend fun <T> T.updatePlaylistThumbnails(
            removedVideoIds: List<Long>,
            targetPlaylistId: Long? = null
        ) where T : LifecycleOwner, T : PlaylistViewContainer, T : VideoViewContainer {
            val playlists = targetPlaylistId?.let {
                playlistViewModel
                    .getFromIdAsync(it)
                    .await()
                    ?.let { playlist -> listOf(playlist) }
                    ?: emptyList()
            } ?: playlistViewModel.allPlaylistsAsync
                .await()
                .filter {
                    it.videos.any { videoId -> removedVideoIds.contains(videoId) }
                }
            val newList = playlists.map {
                it.copy(
                    videos = it.videos.filterNot { video -> removedVideoIds.contains(video) }
                )
            }.map {
                val removedVideos = videoViewModel.getFromIdsAsync(removedVideoIds).await()
                val thumbnail = it.thumbnail
                val shouldUpdate = thumbnail is Playlist.Thumbnail.Video &&
                    removedVideos.any { video ->
                        video.id == thumbnail.id
                    }
                if (shouldUpdate) {
                    it.videos.firstOrNull()?.let { newVideoId ->
                        videoViewModel.getFromIdAsync(newVideoId).await()
                            ?.let { video ->
                                it.copy(thumbnail = Playlist.Thumbnail.Video(video.id))
                            }
                    } ?: it
                } else {
                    it
                }
            }
            playlistViewModel.update(newList)
        }
    }

    private class AllVideoListMenuProvider(
        private val fragment: FragmentAllVideoList
    ) : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_allvideos_option, menu)
            menu.forEach {
                val icon = it.icon ?: return@forEach
                icon.mutate()
                val colorRes = fragment.resources.getColor(R.color.white, null)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    icon.colorFilter = BlendModeColorFilter(colorRes, BlendMode.SRC_ATOP)
                } else {
                    icon.colorFilter = PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_ATOP)
                }

                if (it.itemId == R.id.menu_allvideos_option_sync_order) {
                    val preferences = fragment.activity?.privatePreferences ?: kotlin.run {
                        val message = R.string.snackbar_error_failed_to_access_settings_data
                        Snackbar.make(fragment.requireView(), message, Snackbar.LENGTH_SHORT).show()
                        fragment.findNavController().navigateUp()
                        return
                    }
                    val drawable = if (preferences.videoOrderUp) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    it.setIcon(drawable)
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val view = fragment.requireView()
            return when (menuItem.itemId) {
                R.id.menu_allvideos_option_sync_order -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val next = !preferences.videoOrderUp
                    preferences.videoOrderUp = next
                    val drawable = if (next) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    menuItem.setIcon(drawable)
                    fragment.updateObserver()
                    true
                }
                R.id.menu_allvideos_option_sync_sortrule -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val current = preferences.videoOrderRule
                    val selectionString = R.array.dialog_video_order
                    AlertDialog.Builder(view.context)
                        .setSingleChoiceItems(
                            selectionString,
                            current.ordinal
                        ) { dialog, selected ->
                            preferences.videoOrderRule = VideoOrder.values()[selected]
                            dialog.dismiss()
                            fragment.updateObserver()
                        }.show()
                    true
                }
                else -> false
            }
        }
    }
}