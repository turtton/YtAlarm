package net.turtton.ytalarm.adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import net.turtton.ytalarm.AlarmActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentAlarmList
import net.turtton.ytalarm.fragment.FragmentAlarmListDirections
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.util.BasicComparator
import java.util.Calendar

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
            val context = itemView.context
            alarmType.text = data.repeatType.getDisplay(context)
            alarmSwitch.isChecked = data.enable
            alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val intent = Intent(context, AlarmActivity::class.java)
                    intent.putExtra(AlarmActivity.EXTRA_ALARM_ID, data.id!!)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    val calendar = Calendar.getInstance()
                    val (hour, minute) = data.time.split(':').map { it.toInt() }
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    val clockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
                    if (isChecked) {
                        alarmManager.setAlarmClock(clockInfo, pendingIntent)
                    } else {
                        alarmManager.cancel(pendingIntent)
                    }
                }
            }

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