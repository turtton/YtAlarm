package net.turtton.ytalarm.ui.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.fragment.FragmentPlaylistDirections
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.VideoViewContainer

class PlaylistAdapter<T>(
    private val fragment: T
) : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(
    BasicComparator<Playlist>()
) where T : VideoViewContainer, T : PlaylistViewContainer, T : Fragment {
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        val itemView = holder.itemView
        val title = holder.title
        val videoCount = holder.videoCount
        val thumbnail = holder.thumbnail
        val optionButton = holder.optionButton
        val checkBox = holder.checkBox

        itemView.tag = data.id

        title.text = data.title
        val size = data.videos.size
        if (data.videos.isNotEmpty()) {
            videoCount.text = itemView.context.resources.getQuantityString(
                R.plurals.playlist_item_video_count,
                size,
                size
            )
        } else {
            videoCount.text = itemView.context.getString(
                R.string.playlist_item_video_count_none
            )
        }

        fragment.lifecycleScope.launch {
            attachThumbnail(itemView, thumbnail, data)
        }

        if (data.type !is Playlist.Type.Importing) {
            setClickActions(itemView, optionButton, title.text.toString(), data.id)
        } else {
            title.text = itemView.context.getString(R.string.playlist_name_importing)
            videoCount.visibility = View.GONE
            optionButton.visibility = View.GONE
            holder.selectable = false
        }

        tracker?.applyTrackerState(itemView, checkBox, optionButton, data.id, holder.selectable)

        currentCheckBox += ViewContainer(data.id, checkBox, optionButton, holder.selectable)
    }

    private suspend fun attachThumbnail(view: View, thumbnail: ImageView, data: Playlist) {
        when (val thumbnailData = data.thumbnail) {
            is Playlist.Thumbnail.Video -> {
                val thumbnailUrl = fragment.videoViewModel
                    .getFromIdAsync(thumbnailData.id)
                    .await()
                    ?.thumbnailUrl
                    ?: data.videos.firstOrNull()?.let { video ->
                        fragment.videoViewModel
                            .getFromIdAsync(video)
                            .await()
                            ?.takeIf {
                                val thumbnailUrl = it.thumbnailUrl
                                thumbnailUrl.isNotEmpty() && thumbnailUrl.isNotBlank()
                            }
                    }?.also {
                        fragment.playlistViewModel.update(
                            data.copy(thumbnail = Playlist.Thumbnail.Video(it.id))
                        )
                    }?.thumbnailUrl
                Glide.with(view).load(thumbnailUrl).into(thumbnail)
            }
            is Playlist.Thumbnail.Drawable -> thumbnail.setImageResource(thumbnailData.id)
        }
    }

    private fun setClickActions(
        view: View,
        optionButton: ImageButton,
        title: String,
        playlistId: Long
    ) {
        view.setOnClickListener {
            val action =
                FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment(
                    playlistId
                )
            view.findNavController().navigate(action)
        }

        optionButton.setOnClickListener {
            val menu =
                PopupMenu(it.context, it.findViewById(R.id.item_playlist_option_button))
            menu.inflate(R.menu.menu_playlist_option)
            menu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_playlist_option_rename -> {
                        DialogNameInput(playlistId, fragment, title)
                            .show(fragment.childFragmentManager, "DialogNameInput")
                        true
                    }
                    else -> false
                }
            }
            menu.show()
        }
    }

    private fun SelectionTracker<Long>.applyTrackerState(
        itemView: View,
        checkBox: CheckBox,
        optionButton: ImageButton,
        playlistId: Long,
        selectable: Boolean
    ) {
        var isSelected = isSelected(playlistId)
        if (isSelected && !selectable) {
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                deselect(playlistId)
            }
        }
        isSelected = isSelected && selectable
        itemView.isActivated = isSelected
        checkBox.isChecked = isSelected
        checkBox.visibility = if (hasSelection() && selectable) {
            View.VISIBLE
        } else {
            View.GONE
        }
        optionButton.visibility = if (hasSelection() || !selectable) {
            View.GONE
        } else {
            View.VISIBLE
        }
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_playlist_title)
        val videoCount: TextView = view.findViewById(R.id.item_playlist_video_count)
        val thumbnail: ImageView = view.findViewById(R.id.item_playlist_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_playlist_checkbox)
        val optionButton: ImageButton = view.findViewById(R.id.item_playlist_option_button)

        var selectable: Boolean = true

        init {
            checkBox.visibility = View.GONE
        }

        fun toItemDetail(): ItemDetails<Long> = object : ItemDetails<Long>() {
            override fun getPosition(): Int = absoluteAdapterPosition
            override fun getSelectionKey(): Long? = itemView.tag as Long?
        }
    }

    class PlaylistDetailsLookup(val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            return recyclerView.findChildViewUnder(e.x, e.y)?.let {
                val viewHolder = recyclerView.getChildViewHolder(it)
                if (viewHolder is ViewHolder) {
                    viewHolder.toItemDetail()
                } else {
                    null
                }
            }
        }
    }

    class DialogNameInput(
        private val targetId: Long,
        private val playlistViewContainer: PlaylistViewContainer,
        private val currentName: String
    ) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val editText = EditText(context)
            editText.setText(currentName)

            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        editText.selectAll()
                    }
                }
            }

            return AlertDialog.Builder(context)
                .setView(editText)
                .setPositiveButton(R.string.dialog_playlist_name_input_ok) { _, _ ->
                    lifecycleScope.launch {
                        val viewModel = playlistViewContainer.playlistViewModel
                        viewModel.getFromIdAsync(targetId)
                            .await()
                            ?.let {
                                viewModel.update(it.copy(title = editText.text.toString()))
                            }
                    }
                }.setNegativeButton(R.string.dialog_playlist_name_input_cancel) { _, _ -> }
                .create()
        }
    }

    private data class ViewContainer(
        val id: Long,
        val checkBox: CheckBox,
        val optionButton: ImageButton,
        val selectable: Boolean
    )
}