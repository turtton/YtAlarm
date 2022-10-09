package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.structure.Video

class MultiChoiceVideoListAdapter<T>(
    private val displayDataList: List<DisplayData<T>>,
    private val chosenTargets: List<Boolean>
) : RecyclerView.Adapter<MultiChoiceVideoListAdapter.ViewHolder>() {
    val selectedId = hashSetOf<T>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        @Suppress("ktlint:argument-list-wrapping")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_choice_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = displayDataList[position]
        holder.title.text = data.title
        data.thumbnailUrl?.also {
            Glide.with(holder.itemView).load(it).into(holder.thumbnail)
        }
        chosenTargets[position].also {
            holder.checkBox.isChecked = it
            if (it) {
                selectedId.add(data.id)
            }
        }
        holder.itemView.setOnClickListener {
            val checkBox = holder.checkBox
            if (checkBox.isChecked) {
                selectedId.remove(data.id)
                checkBox.isChecked = false
            } else {
                selectedId.add(data.id)
                checkBox.isChecked = true
            }
        }
    }

    override fun getItemCount(): Int = displayDataList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_dialog_choice_video_title)
        val thumbnail: ImageView = view.findViewById(R.id.item_dialog_choice_video_thumbnail)
        val checkBox: CheckBox = view.findViewById(R.id.item_dialog_choice_video_checkBox)
    }

    data class DisplayData<T>(val id: T, val title: String, val thumbnailUrl: String?) {
        companion object {
            fun Video.toDisplayData() = DisplayData(id, title, thumbnailUrl)
            fun Playlist.toDisplayData() = DisplayData(id!!, title, thumbnailUrl)
        }
    }
}