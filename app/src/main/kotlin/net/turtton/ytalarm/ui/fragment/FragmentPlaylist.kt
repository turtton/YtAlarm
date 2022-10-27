package net.turtton.ytalarm.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import androidx.work.await
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.adapter.PlaylistAdapter
import net.turtton.ytalarm.ui.dialog.DialogRemoveVideo
import net.turtton.ytalarm.ui.menu.AttachableMenuProvider
import net.turtton.ytalarm.ui.menu.MenuProviderContainer
import net.turtton.ytalarm.ui.menu.SelectionMenuObserver
import net.turtton.ytalarm.ui.selection.SelectionTrackerContainer
import net.turtton.ytalarm.ui.selection.TagKeyProvider
import net.turtton.ytalarm.util.extensions.playlistOrderRule
import net.turtton.ytalarm.util.extensions.playlistOrderUp
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.showMessageIfNull
import net.turtton.ytalarm.util.order.PlaylistOrder
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
    SelectionTrackerContainer<Long>,
    MenuProviderContainer {
    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }
    override val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }
    val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    override var selectionTracker: SelectionTracker<Long>? = null
    override var menuProvider: MenuProvider? = null
    private lateinit var adapter: PlaylistAdapter<FragmentPlaylist>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        activity.binding.fab.visibility = View.VISIBLE

        val recyclerView = binding.recyclerList
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = PlaylistAdapter(this)
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "PlaylistTracker",
            recyclerView,
            TagKeyProvider(recyclerView),
            PlaylistAdapter.PlaylistDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).build()
        adapter.tracker = selectionTracker

        selectionTracker?.addObserver(PlaylistSelectionObserver(this))

        savedInstanceState?.let {
            selectionTracker?.onRestoreInstanceState(it)
        }

        updateObserver(view)

        PlaylistMenuProvider(this, view).also {
            menuProvider = it
            activity.addMenuProvider(it, viewLifecycleOwner)
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            val action = FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment()
            findNavController().navigate(action)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker?.onSaveInstanceState(outState)
    }

    private fun updateObserver(view: View) {
        playlistViewModel.allPlaylists.observe(viewLifecycleOwner) { immutableList ->
            if (immutableList == null) return@observe
            lifecycleScope.launch {
                val garbage = immutableList.collectGarbage(view.context)
                if (garbage.isNotEmpty()) {
                    playlistViewModel.delete(garbage)
                }
            }
            val mutableList = immutableList.toMutableList()
            val preferences = activity?.privatePreferences ?: kotlin.run {
                val message = R.string.snackbar_error_failed_to_access_settings_data
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@observe
            }
            when (preferences.playlistOrderRule) {
                PlaylistOrder.TITLE -> mutableList.sortBy { it.title }
                PlaylistOrder.CREATION_DATE -> mutableList.sortBy { it.creationDate }
                PlaylistOrder.LAST_UPDATED -> mutableList.sortBy { it.lastUpdated }
            }
            if (!preferences.playlistOrderUp) {
                mutableList.reverse()
            }
            adapter.submitList(mutableList)
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun List<Playlist>.collectGarbage(context: Context) = filter { playlist ->
        if (playlist.type !is Playlist.Type.Importing) return@filter false
        val videoId = playlist.videos.firstOrNull() ?: return@filter false
        val video = videoViewModel.getFromIdAsync(videoId).await() ?: return@filter false
        val state = when (val state = video.stateData) {
            is Video.State.Importing ->
                state.state as? Video.WorkerState.Working
            is Video.State.Downloading ->
                state.state as? Video.WorkerState.Working
            else -> return@filter false
        } ?: return@filter false
        val workerState = WorkManager.getInstance(context)
            .getWorkInfoById(state.workerId)
            .await()
            ?.state
        workerState == null || workerState.isFinished
    }

    class PlaylistSelectionObserver(
        fragment: FragmentPlaylist
    ) : SelectionMenuObserver<Long, FragmentPlaylist>(
        fragment,
        AttachableMenuProvider(
            fragment,
            R.menu.menu_playlist_action,
            R.id.menu_playlist_action_remove to to@{
                val selectionTracker = fragment.selectionTracker ?: return@to false
                val selection = selectionTracker.selection.toList()
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
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    selectionTracker.clearSelection()
                }.show(fragment.childFragmentManager, "PlaylistRemoveDialog")
                true
            }
        )
    )

    private class PlaylistMenuProvider(
        private val fragment: FragmentPlaylist,
        private val view: View
    ) : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_playlist_option, menu)
            menu.forEach {
                val icon = it.icon ?: return@forEach
                icon.mutate()
                val colorRes = fragment.resources.getColor(R.color.white, null)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    icon.colorFilter = BlendModeColorFilter(colorRes, BlendMode.SRC_ATOP)
                } else {
                    icon.colorFilter = PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_ATOP)
                }

                if (it.itemId == R.id.menu_playlist_option_order) {
                    val preferences = fragment.activity?.privatePreferences ?: kotlin.run {
                        val message = R.string.snackbar_error_failed_to_access_settings_data
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                        fragment.findNavController().navigateUp()
                        return
                    }
                    val drawable = if (preferences.playlistOrderUp) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    it.setIcon(drawable)
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_playlist_option_order -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val next = !preferences.playlistOrderUp
                    preferences.playlistOrderUp = next
                    val drawable = if (next) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    menuItem.setIcon(drawable)
                    fragment.updateObserver(view)
                    true
                }
                R.id.menu_playlist_option_sortrule -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val current = preferences.playlistOrderRule
                    val selectionString = R.array.dialog_playlist_order
                    AlertDialog.Builder(view.context)
                        .setSingleChoiceItems(
                            selectionString,
                            current.ordinal
                        ) { dialog, selected ->
                            preferences.playlistOrderRule = PlaylistOrder.values()[selected]
                            dialog.dismiss()
                            fragment.updateObserver(view)
                        }.show()
                    true
                }
                else -> false
            }
        }
    }
}