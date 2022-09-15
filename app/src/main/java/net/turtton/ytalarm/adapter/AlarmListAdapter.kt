package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentAlarmList
import net.turtton.ytalarm.fragment.FragmentAlarmListDirections
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.util.BasicComparator

class AlarmListAdapter(
    private val parentFragment: FragmentAlarmList
) : ListAdapter<Alarm, AlarmListAdapter.ViewHolder>(BasicComparator<Alarm>()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aram, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.apply {
            aramTime.text = data.time
            aramType.text = data.repeatType.name
            aramSwitch.isChecked = data.enable
            parentFragment.playlistViewModel
                .getFromId(data.playListId)
                .observe(parentFragment.requireActivity()) { playlist ->
                    playlist?.also {
                        playlistName.text = it.title
                        Glide.with(itemView).load(it.thumbnailUrl).into(aramThumbnail)
                    }
                }

            itemView.setOnClickListener {
                @Suppress("ktlint:argument-list-wrapping")
                val action = FragmentAlarmListDirections
                    .actionAlarmListFragmentToAlarmSettingsFragment(data.id!!)
                parentFragment.findNavController().navigate(action)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val aramTime: TextView = view.findViewById(R.id.item_playlist_title)
        val aramType: TextView = view.findViewById(R.id.item_aram_type)
        val playlistName: TextView = view.findViewById(R.id.item_aram_playlist_name)
        val aramThumbnail: ImageView = view.findViewById(R.id.item_playlist_thumbnail)
        val aramSwitch: SwitchCompat = view.findViewById(R.id.item_aram_switch)
    }
}