package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentPlaylistDirections
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.util.BasicComparator

class PlaylistAdapter : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(BasicComparator<Playlist>()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var playlist: Playlist

        private val title: TextView = view.findViewById(R.id.item_playlist_title)
        private val videoCount: TextView = view.findViewById(R.id.item_playlist_video_count)
        private val thumbnail: ImageView = view.findViewById(R.id.item_playlist_thumbnail)

        fun bindData(data: Playlist) {
            playlist = data
            title.text = data.title
            val size = data.videos.size
            if (data.videos.isNotEmpty()) {
                videoCount.text = itemView.context.resources.getQuantityString(R.plurals.playlist_item_video_count, size, size)
            } else {
                videoCount.text = itemView.context.getString(R.string.playlist_item_video_count_none)
            }
            Glide.with(itemView).load(data.thumbnailUrl).into(thumbnail)

            itemView.setOnClickListener {
                val action = FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment(data.id)
                itemView.findNavController().navigate(action)
            }
        }
    }
}