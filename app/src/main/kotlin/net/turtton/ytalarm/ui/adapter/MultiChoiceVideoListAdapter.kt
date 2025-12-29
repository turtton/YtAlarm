package net.turtton.ytalarm.ui.adapter

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
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.viewmodel.VideoViewModel

class MultiChoiceVideoListAdapter<T>(
    private val displayDataList: List<DisplayData<T>>,
    private val chosenTargets: List<Boolean>
) : RecyclerView.Adapter<MultiChoiceVideoListAdapter.ViewHolder>() {
    val selectedId = hashSetOf<T>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_choice_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = displayDataList[position]
        holder.title.text = data.title
        when (val thumbnail = data.thumbnailUrl) {
            is DisplayData.Thumbnail.Url -> Glide.with(holder.itemView)
                .load(thumbnail.url)
                .into(holder.thumbnail)

            is DisplayData.Thumbnail.Drawable -> holder.thumbnail.setImageResource(thumbnail.id)

            null -> holder.thumbnail.setImageResource(R.drawable.ic_no_image)
        }
        chosenTargets[position].also {
            holder.checkBox.isChecked = it
            if (it) {
                selectedId.add(data.id)
            }
        }

        holder.itemView.setOnClickListener {
            val current = holder.checkBox.isChecked
            holder.checkBox.isChecked = !current
        }
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedId.add(data.id)
            } else {
                selectedId.remove(data.id)
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

            fun <T : MutableCollection<DisplayData<Long>>> T.addNewPlaylist() = apply {
                val title = "Create New Playlist"
                val drawable = Thumbnail.Drawable(R.drawable.ic_add_playlist)
                add(DisplayData(0, title, drawable))
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