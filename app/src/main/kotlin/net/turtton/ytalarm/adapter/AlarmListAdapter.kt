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
import net.turtton.ytalarm.util.extensions.getDisplayTime
import net.turtton.ytalarm.util.extensions.joinStringWithSlash

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
            alarmTime.text = data.getDisplayTime()
            val context = itemView.context
            alarmType.text = data.repeatType.getDisplay(context)
            alarmSwitch.isChecked = data.isEnable
            alarmSwitch.setOnCheckedChangeListener { button, isChecked ->
                val async = parentFragment.alarmViewModel.getFromIdAsync(data.id!!)
                button.isClickable = false
                parentFragment.lifecycleScope.launch {
                    val alarm = async.await().copy(isEnable = isChecked)
                    parentFragment.alarmViewModel.update(alarm).join()
                    launch(Dispatchers.Main) {
                        button.isClickable = true
                    }
                }
            }

            val async = parentFragment.playlistViewModel.getFromIdsAsync(data.playListId)
            parentFragment.lifecycleScope.launch {
                async.await().let { list ->
                    launch(Dispatchers.Main) {
                        playlistName.text = list.map { it.title }.joinStringWithSlash()
                        Glide.with(itemView).load(list.first().thumbnailUrl).into(alarmThumbnail)
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