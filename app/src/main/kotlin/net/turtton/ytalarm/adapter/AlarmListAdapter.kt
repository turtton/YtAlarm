package net.turtton.ytalarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            alarmTime.text = data.time
            alarmType.text = data.repeatType.getDisplay(itemView.context)
            alarmSwitch.isChecked = data.enable

            val async = parentFragment.playlistViewModel.getFromIdAsync(data.playListId!!)
            parentFragment.lifecycleScope.launch {
                async.await()?.let {
                    launch(Dispatchers.Main) {
                        playlistName.text = it.title
                        Glide.with(itemView).load(it.thumbnailUrl).into(alarmThumbnail)
                    }
                }
            }

            itemView.setOnClickListener {
                val action = FragmentAlarmListDirections
                    .actionAlarmListFragmentToAlarmSettingsFragment(data.id!!)
                parentFragment.findNavController().navigate(action)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val alarmTime: TextView = view.findViewById(R.id.item_playlist_title)
        val alarmType: TextView = view.findViewById(R.id.item_aram_type)
        val playlistName: TextView = view.findViewById(R.id.item_aram_playlist_name)
        val alarmThumbnail: ImageView = view.findViewById(R.id.item_playlist_thumbnail)
        val alarmSwitch: SwitchCompat = view.findViewById(R.id.item_aram_switch)
    }
}