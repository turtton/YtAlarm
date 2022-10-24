package net.turtton.ytalarm.ui.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.fragment.FragmentAllVideoList
import net.turtton.ytalarm.ui.fragment.FragmentVideoList
import net.turtton.ytalarm.ui.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.util.extensions.deleteVideo
import net.turtton.ytalarm.util.extensions.showInsertVideoToPlaylistsDialog
import net.turtton.ytalarm.util.extensions.updateThumbnail
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

class VideoListAdapter<T>(
    private val fragment: T
) : ListAdapter<Video, VideoListAdapter.ViewHolder>(BasicComparator<Video>())
        where T : Fragment,
              T : LifecycleOwner,
              T : PlaylistViewContainer,
              T : VideoViewContainer {
    private val currentCheckBox = hashSetOf<ViewContainer>()

    var tracker: SelectionTracker<Long>? = null
        set(value) {
            value?.let {
                it.addObserver(object : SelectionObserver<Long>() {
                    override fun onSelectionChanged() {
                        val selected = currentCheckBox.filter { current ->
                            it.isSelected(current.id)
                        }
                        val unSelectable = selected.filter { current ->
                            !current.selectable
                        }
                        if (unSelectable.isNotEmpty() && unSelectable.size == selected.size) {
                            fragment.lifecycleScope.launch(Dispatchers.Main) {
                                it.clearSelection()
                            }
                            return
                        }

                        currentCheckBox.forEach { (id, box, option, selectable) ->
                            if (!selectable) {
                                fragment.lifecycleScope.launch(Dispatchers.Main) {
                                    it.deselect(id)
                                }
                                return@forEach
                            }
                            box.visibility = if (it.hasSelection()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                            box.isChecked = it.isSelected(id)

                            option.visibility = if (it.hasSelection()) {
                                View.GONE
                            } else {
                                View.VISIBLE
                            }
                        }
                    }
                })
            }
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_video_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        val itemView = holder.itemView
        itemView.tag = data.id
        val checkBox = holder.checkBox

        when (val state = data.stateData) {
            is Video.State.Importing -> setUpAsImporting(itemView, holder, data, state.state)
            is Video.State.Downloading -> setUpAsDownloading(holder, state.state)
            else -> setUpNormally(itemView, holder, data, state)
        }
        tracker?.applyTrackerState(itemView, holder, holder.selectable, data.id)
        val container = ViewContainer(data.id, checkBox, holder.optionButton, holder.selectable)
        currentCheckBox.add(container)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        currentCheckBox.remove(
            ViewContainer(
                holder.itemView.tag as Long,
                holder.checkBox,
                holder.optionButton,
                holder.selectable
            )
        )
    }

    private fun setUpAsImporting(
        itemView: View,
        holder: ViewHolder,
        video: Video,
        workerState: Video.WorkerState
    ) {
        val context = itemView.context
        val title = holder.title
        val domainOrSize = holder.domainOrSize
        val thumbnail = holder.thumbnail
        if (workerState is Video.WorkerState.Failed) {
            title.text = context.getString(R.string.item_video_list_import_failed)
            domainOrSize.text = context.getString(R.string.item_video_list_click_to_retry)
            thumbnail.setImageResource(R.drawable.ic_error)

            val url = workerState.url
            itemView.setOnClickListener { view ->
                val retryButtonMessage = R.string.dialog_video_import_failed_retry
                val clearButtonMessage = R.string.dialog_video_import_failed_clear

                AlertDialog.Builder(view.context)
                    .setTitle(R.string.dialog_video_import_failed_title)
                    .setMessage(url)
                    .setPositiveButton(retryButtonMessage) { _, _ ->
                        fragment.lifecycleScope.launch {
                            val targetPlaylists = fragment.playlistViewModel
                                .allPlaylistsAsync
                                .await()
                                .filter { it.videos.contains(video.id) }
                                .apply {
                                    fragment.playlistViewModel.update(deleteVideo(video.id)).join()
                                }
                                .map { it.id }
                                .toLongArray()
                            VideoInfoDownloadWorker.registerWorker(context, url, targetPlaylists)
                            fragment.videoViewModel.delete(video)
                        }
                    }.setNegativeButton(clearButtonMessage) { _, _ ->
                        fragment.lifecycleScope.launch {
                            val targetPlaylists = fragment.playlistViewModel
                                .allPlaylistsAsync
                                .await()
                                .filter { it.videos.contains(video.id) }
                                .deleteVideo(video.id)
                            fragment.playlistViewModel.update(targetPlaylists)
                            fragment.videoViewModel.delete(video)
                        }
                    }.show()
                holder.selectable = false
            }
        } else {
            title.text = video.title
            domainOrSize.visibility = View.GONE
            thumbnail.setImageResource(R.drawable.ic_download)
        }
    }

    private fun setUpAsDownloading(holder: ViewHolder, workerState: Video.WorkerState) {
        if (workerState is Video.WorkerState.Failed) {
            holder.selectable = false
            TODO("implement in #65")
        }
    }

    private fun setUpNormally(
        itemView: View,
        holder: ViewHolder,
        video: Video,
        state: Video.State
    ) {
        val context = itemView.context
        val title = holder.title
        val domainOrSize = holder.domainOrSize
        val thumbnail = holder.thumbnail
        title.text = video.title
        domainOrSize.text = if (state is Video.State.Downloaded) {
            context.getString(
                R.string.item_video_list_data_size,
                state.fileSize / BYTE_CARRY_IN / BYTE_CARRY_IN
            )
        } else {
            video.domain
        }
        Glide.with(itemView).load(video.thumbnailUrl).into(thumbnail)
        itemView.setOnClickListener {
            val navController = it.findFragment<Fragment>().findNavController()

            val args = FragmentVideoPlayerArgs(video.videoId).toBundle()
            navController.navigate(R.id.nav_graph_video_player, args)
        }
        addVideoMenu(holder, video, state)
    }

    private fun addVideoMenu(holder: ViewHolder, video: Video, state: Video.State) {
        if (state.isUpdating()) {
            holder.optionButton.visibility = View.GONE
            return
        } else {
            holder.optionButton.visibility = View.VISIBLE
        }
        when (fragment) {
            is FragmentVideoList -> addPlaylistVideoMenu(fragment, holder, video)
            is FragmentAllVideoList -> addAllVideoListMenu(fragment, holder, video)
        }
    }

    private fun addPlaylistVideoMenu(
        fragment: FragmentVideoList,
        holder: ViewHolder,
        video: Video
    ) {
        holder.optionButton.setOnClickListener { view ->
            val menu = PopupMenu(view.context, view.findViewById(R.id.item_video_option_button))
            menu.inflate(R.menu.menu_video_list_item_option)
            menu.setOnMenuItemClickListener { menuItem ->
                onVideoListMenuClicked(view, fragment, menuItem, video)
            }
            menu.show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onVideoListMenuClicked(
        view: View,
        fragment: FragmentVideoList,
        menuItem: MenuItem,
        video: Video
    ): Boolean {
        when (menuItem.itemId) {
            R.id.menu_video_list_item_option_set_thumbnail -> {
                fragment.lifecycleScope.launch {
                    val playlistId = fragment.currentId.value
                    val playlist = fragment.playlistViewModel.getFromIdAsync(playlistId).await()
                    if (playlist == null) {
                        val message = R.string.snackbar_error_failed_to_get_playlist
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                        fragment.findNavController().navigateUp()
                        return@launch
                    }
                    val thumbnail = Playlist.Thumbnail.Video(video.id)
                    val newPlaylist = playlist.copy(thumbnail = thumbnail)
                    fragment.playlistViewModel.update(newPlaylist).join()
                    val message = R.string.snackbar_thumbnail_updated
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                }
            }
            R.id.menu_video_list_item_option_download -> downloadVideo(view)
            R.id.menu_video_list_item_option_reimport -> reimportVideo(view, fragment as T, video)
            R.id.menu_video_list_item_option_delete -> deleteVideo(fragment as T, video)
            else -> return false
        }
        return true
    }

    private fun addAllVideoListMenu(
        fragment: FragmentAllVideoList,
        holder: ViewHolder,
        video: Video
    ) {
        holder.optionButton.setOnClickListener { view ->
            val menu = PopupMenu(view.context, view.findViewById(R.id.item_video_option_button))
            menu.inflate(R.menu.menu_all_video_list_item_option)
            menu.setOnMenuItemClickListener { menuItem ->
                onAllVideoListMenuClicked(view, fragment, menuItem, video)
            }
            menu.show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onAllVideoListMenuClicked(
        view: View,
        fragment: FragmentAllVideoList,
        menuItem: MenuItem,
        video: Video
    ): Boolean {
        when (menuItem.itemId) {
            R.id.menu_all_video_list_option_add_to_playlist -> fragment.lifecycleScope.launch {
                val playlists = fragment.playlistViewModel.allPlaylistsAsync
                    .await()
                    .map { it.toDisplayData(fragment.videoViewModel) }
                    .toMutableList()
                    .addNewPlaylist()

                showInsertVideoToPlaylistsDialog(
                    fragment,
                    playlists,
                    listOf(video.id),
                    "PlaylistSelectDialog"
                )
            }
            R.id.menu_all_video_list_option_reimport -> reimportVideo(view, fragment as T, video)
            R.id.menu_all_video_list_option_download -> downloadVideo(view)
            R.id.menu_all_video_list_option_delete -> deleteVideo(fragment as T, video)
            else -> return false
        }
        return true
    }

    private fun downloadVideo(view: View) {
        // TODO implement download feature
        val message = R.string.snackbar_not_implemented
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun reimportVideo(view: View, fragment: T, video: Video) {
        fragment.lifecycleScope.launch {
            com.github.michaelbull.result.runCatching {
                val targetPlaylists = fragment.playlistViewModel
                    .allPlaylistsAsync
                    .await()
                    .removeVideoAndUpdateThumbnail(video.id)
                fragment.playlistViewModel.update(targetPlaylists).join()
                fragment.videoViewModel.delete(video).join()
                val targetPlaylistIds = targetPlaylists.map { it.id }.toLongArray()
                VideoInfoDownloadWorker.registerWorker(
                    view.context,
                    video.videoUrl,
                    targetPlaylistIds
                )
            }
        }
    }

    private fun deleteVideo(fragment: T, video: Video) {
        fragment.lifecycleScope.launch {
            val targetPlaylists = fragment.playlistViewModel
                .allPlaylistsAsync
                .await()
                .removeVideoAndUpdateThumbnail(video.id)
            fragment.playlistViewModel.update(targetPlaylists)
            fragment.videoViewModel.delete(video)
        }
    }

    private fun SelectionTracker<Long>.applyTrackerState(
        view: View,
        holder: ViewHolder,
        selectable: Boolean,
        videoId: Long
    ) {
        var isSelected = isSelected(videoId)
        if (isSelected && !selectable) {
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                deselect(videoId)
            }
        }
        isSelected = isSelected && selectable
        view.isActivated = isSelected
        val checkBox = holder.checkBox
        checkBox.isChecked = isSelected
        checkBox.visibility = if (hasSelection() && selectable) {
            View.VISIBLE
        } else {
            View.GONE
        }
        holder.optionButton.visibility = if (hasSelection() || !selectable) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    /**
     * returns only playlist which contains target videoId
     */
    private fun List<Playlist>.removeVideoAndUpdateThumbnail(videoId: Long): List<Playlist> =
        filter { it.videos.contains(videoId) }
            .deleteVideo(videoId)
            .map {
                it.takeIf {
                    (it.thumbnail as? Playlist.Thumbnail.Video)?.id == videoId
                }?.updateThumbnail() ?: it
            }

    companion object {
        const val BYTE_CARRY_IN = 1024f
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_video_list_title)
        val domainOrSize: TextView = view.findViewById(R.id.item_video_domain_or_size)
        val thumbnail: ImageView = view.findViewById(R.id.item_video_list_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_video_checkbox)
        val optionButton: ImageButton = view.findViewById(R.id.item_video_option_button)

        var selectable: Boolean = true

        init {
            checkBox.visibility = View.GONE
        }

        fun toItemDetail(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = absoluteAdapterPosition
                override fun getSelectionKey(): Long? = itemView.tag as? Long
            }
    }

    class VideoListDetailsLookup(val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            return recyclerView.findChildViewUnder(e.x, e.y)?.let { view ->
                val viewHolder = recyclerView.getChildViewHolder(view)
                if (viewHolder is ViewHolder) {
                    viewHolder.toItemDetail()
                } else {
                    null
                }
            }
        }
    }

    private data class ViewContainer(
        val id: Long,
        val checkBox: CheckBox,
        val optionButton: ImageButton,
        val selectable: Boolean
    )
}