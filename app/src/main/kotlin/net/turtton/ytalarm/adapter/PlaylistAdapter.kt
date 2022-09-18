package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentPlaylistDirections
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.util.BasicComparator

class PlaylistAdapter : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(
    BasicComparator<Playlist>()
) {
    private val currentCheckBox = hashSetOf<Pair<Long, CheckBox>>()

    var tracker: SelectionTracker<Long>? = null
        set(value) {
            value?.let {
                it.addObserver(object : SelectionObserver<Long>() {
                    override fun onSelectionChanged() {
                        currentCheckBox.forEach { (id, box) ->
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.itemView.tag = data.id!!.toLong()
        holder.apply {
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
            data.thumbnailUrl.also {
                Glide.with(itemView).load(it).into(thumbnail)
            }
            tracker?.let {
                val isSelected = it.isSelected(data.id.toLong())
                itemView.isActivated = isSelected
                checkBox.isChecked = isSelected
                checkBox.visibility = if (it.hasSelection()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            itemView.setOnClickListener {
                val action = FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment(
                    data.id
                )
                itemView.findNavController().navigate(action)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        currentCheckBox.remove(holder.itemView.tag as Long to holder.checkBox)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_playlist_title)
        val videoCount: TextView = view.findViewById(R.id.item_playlist_video_count)
        val thumbnail: ImageView = view.findViewById(R.id.item_playlist_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_playlist_checkbox)

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
}