package net.turtton.ytalarm.ui.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.util.BasicComparator
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

class VideoListAdapter<T>(
    private val fragment: T
) : ListAdapter<Video, VideoListAdapter.ViewHolder>(BasicComparator<Video>())
        where T : LifecycleOwner,
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

                        currentCheckBox.forEach { (id, box, selectable) ->
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
        holder.itemView.tag = data.id
        holder.apply {
            val state = data.stateData
            val context = itemView.context

            when (state) {
                is Video.State.Importing -> if (state.state is Video.WorkerState.Failed) {
                    title.text = context.getString(R.string.item_video_list_import_failed)
                    domainOrSize.text = context.getString(R.string.item_video_list_click_to_retry)
                    thumbnail.setImageResource(R.drawable.ic_error)
                    val url = state.state.url
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
                                        .filter { it.videos.contains(data.id) }
                                        .apply {
                                            val edited = map { playlist ->
                                                playlist.copy(
                                                    videos = playlist.videos.filterNot {
                                                        it == data.id
                                                    }
                                                )
                                            }
                                            fragment.playlistViewModel.update(edited).join()
                                        }
                                        .map { it.id }
                                        .toLongArray()
                                    VideoInfoDownloadWorker.registerWorker(
                                        view.context,
                                        url,
                                        targetPlaylists
                                    )
                                    fragment.videoViewModel.delete(data)
                                }
                            }.setNegativeButton(clearButtonMessage) { _, _ ->
                                fragment.lifecycleScope.launch {
                                    val targetPlaylists = fragment.playlistViewModel
                                        .allPlaylistsAsync
                                        .await()
                                        .filter { it.videos.contains(data.id) }
                                    val edited = targetPlaylists.map { playlist ->
                                        playlist.copy(
                                            videos = playlist.videos.filterNot { it == data.id }
                                        )
                                    }
                                    fragment.playlistViewModel.update(edited)
                                    fragment.videoViewModel.delete(data)
                                }
                            }.show()
                        selectable = false
                    }
                } else {
                    title.text = data.title
                    domainOrSize.visibility = View.GONE
                    thumbnail.setImageResource(R.drawable.ic_download)
                }
                is Video.State.Downloading -> if (state.state is Video.WorkerState.Failed) {
                    selectable = false
                    TODO("implement in #65")
                }
                else -> {
                    title.text = data.title
                    domainOrSize.text = if (state is Video.State.Downloaded) {
                        context.getString(
                            R.string.item_video_list_data_size,
                            state.fileSize / 1024f / 1024f
                        )
                    } else {
                        data.domain
                    }
                    Glide.with(itemView).load(data.thumbnailUrl).into(thumbnail)
                    itemView.setOnClickListener {
                        val navController = it.findFragment<Fragment>().findNavController()

                        val args = FragmentVideoPlayerArgs(data.videoId).toBundle()
                        navController.navigate(R.id.nav_graph_video_player, args)
                    }
                }
            }
            tracker?.let {
                var isSelected = it.isSelected(data.id)
                if (isSelected && !selectable) {
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        it.deselect(data.id)
                    }
                }
                isSelected = isSelected && selectable
                itemView.isActivated = isSelected
                checkBox.isChecked = isSelected
                checkBox.visibility = if (it.hasSelection() && selectable) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            currentCheckBox.add(ViewContainer(data.id, checkBox, selectable))
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        currentCheckBox.remove(
            ViewContainer(
                holder.itemView.tag as Long,
                holder.checkBox,
                holder.selectable
            )
        )
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_video_list_title)
        val domainOrSize: TextView = view.findViewById(R.id.item_video_domain_or_size)
        val thumbnail: ImageView = view.findViewById(R.id.item_video_list_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_video_checkbox)

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

    private data class ViewContainer(val id: Long, val checkBox: CheckBox, val selectable: Boolean)
}