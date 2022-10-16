package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.BasicComparator

class VideoListAdapter : ListAdapter<Video, VideoListAdapter.ViewHolder>(BasicComparator<Video>()) {
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
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_video_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.itemView.tag = data.id
        holder.apply {
            currentCheckBox.add(data.id to checkBox)
            title.text = data.title
            val state = data.stateData
            domainOrSize.text = if (state is Video.State.Downloaded) {
                itemView.context.getString(
                    R.string.item_video_list_data_size,
                    state.fileSize / 1024f / 1024f
                )
            } else {
                data.domain
            }
            Glide.with(itemView).load(data.thumbnailUrl).into(thumbnail)
            tracker?.let {
                val isSelected = it.isSelected(data.id)
                itemView.isActivated = isSelected
                checkBox.isChecked = isSelected
                checkBox.visibility = if (it.hasSelection()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            if (state !is Video.State.Importing && state !is Video.State.Downloading) {
                itemView.setOnClickListener {
                    val navController = it.findFragment<Fragment>().findNavController()

                    val args = FragmentVideoPlayerArgs(data.videoId).toBundle()
                    navController.navigate(R.id.nav_graph_video_player, args)
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        currentCheckBox.remove(holder.itemView.tag as Long to holder.checkBox)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_video_list_title)
        val domainOrSize: TextView = view.findViewById(R.id.item_video_domain_or_size)
        val thumbnail: ImageView = view.findViewById(R.id.item_video_list_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_video_checkbox)

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
}