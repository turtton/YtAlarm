package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.BasicComparator

class VideoListAdapter : ListAdapter<Video, VideoListAdapter.ViewHolder>(BasicComparator<Video>()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_video_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.apply {
            title.text = data.title
            val isLocal = data.internalLink.startsWith("http")
            domainOrSize.text = if (isLocal) {
                data.domain
            } else {
                itemView.context.getString(R.string.item_video_list_data_size, data.fileSize / 1024f / 1024f)
            }
            Glide.with(itemView).load(data.thumbnailUrl).into(thumbnail)

            itemView.setOnClickListener {
                val navController = it.findFragment<Fragment>().findNavController()

                val args = FragmentVideoPlayerArgs(data.id).toBundle()
                navController.navigate(R.id.nav_graph_video_player, args)
            }
            itemView.setOnLongClickListener {
                // TODO create choice mode
                false
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_video_list_title)
        val domainOrSize: TextView = view.findViewById(R.id.item_video_domain_or_size)
        val thumbnail: ImageView = view.findViewById(R.id.item_video_list_thumbnail)
    }
}