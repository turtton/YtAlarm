package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.viewmodel.VideoViewModel

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

    data class DisplayData<T>(val id: T, val title: String, val thumbnailUrl: Thumbnail?) {
        companion object {
            fun Video.toDisplayData() = DisplayData(id, title, Thumbnail.Url(thumbnailUrl))
            suspend fun Playlist.toDisplayData(videoViewModel: VideoViewModel) = when (thumbnail) {
                is Playlist.Thumbnail.Video ->
                    videoViewModel.getFromIdAsync(thumbnail.id).await()?.thumbnailUrl.let {
                        Thumbnail.Url(it)
                    }
                is Playlist.Thumbnail.Drawable -> Thumbnail.Drawable(thumbnail.id)
            }.let {
                DisplayData(id, title, it)
            }
        }

        sealed interface Thumbnail {
            @JvmInline
            value class Url(val url: String?) : Thumbnail

            @JvmInline
            value class Drawable(@DrawableRes val id: Int) : Thumbnail
        }
    }
}