package net.turtton.ytalarm.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.fragment.FragmentAlarmSettings
import net.turtton.ytalarm.structure.AlarmSettingData
import net.turtton.ytalarm.util.OnSeekBarChangeListenerBuilder

class AlarmSettingsAdapter(
    private val fragment: FragmentAlarmSettings,
    playlistName: String? = null
) : RecyclerView.Adapter<AlarmSettingsAdapter.ViewHolder>() {

    private val dataSet: Array<AlarmSettingData>

    init {
        val alarmState = fragment.alarmData
        val alarm = alarmState.value
        val timeSelector =
            AlarmSettingData.NormalData(R.string.setting_time, alarm.time) { _, description ->
                SettingTimePickerFragment(alarm.time) { _, hourOfDay, minute ->
                    val newTime = String.format("%02d:%02d", hourOfDay, minute)
                    alarmState.update {
                        it.copy(time = newTime)
                    }
                    description.text = newTime
                }.show(fragment.parentFragmentManager, "settingTimePicker")
            }
        val plName = playlistName ?: "Nothing"
        val playlistSelector =
            AlarmSettingData.NormalData(R.string.setting_playlist, plName) { _, description ->
                val async = fragment.playlistViewModel.allPlaylistsAsync
                fragment.lifecycleScope.launch {
                    val playlists = async.await()
                    launch(Dispatchers.Main) {
                        AlertDialog.Builder(fragment.activity)
                            .setTitle(R.string.dialog_playlist_choice_title)
                            .setItems(playlists.map { it.title }.toTypedArray()) { _, index ->
                                val playlist = playlists[index]
                                alarmState.update {
                                    it.copy(playListId = playlist.id)
                                }
                                description.text = playlist.title
                            }
                            .show()
                    }
                }
            }
        val loopToggle =
            AlarmSettingData.ToggleData(R.string.setting_loop, alarm.loop) { _, value ->
                alarmState.update {
                    it.copy(loop = value)
                }
            }
        val volumeProgress =
            AlarmSettingData.PercentData(R.string.setting_volume, alarm.volume, 100) {
                onProgressChanged = { _, progress, isUser ->
                    if (isUser) {
                        alarmState.update {
                            it.copy(volume = progress)
                        }
                    }
                }
            }

        dataSet = arrayOf(timeSelector, playlistSelector, loopToggle, volumeProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_aram_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataSet[position]
        holder.apply {
            itemView.isClickable = false
            title.text = itemView.context.getText(data.nameResourceId)
            when (data) {
                is AlarmSettingData.NormalData -> {
                    description.text = data.value
                    switch.visibility = View.GONE
                    seekBar.visibility = View.GONE
                    if (data.onClick != null) {
                        itemView.setOnClickListener { view ->
                            data.onClick?.let { it(view, description) }
                        }
                    }
                }
                is AlarmSettingData.ToggleData -> {
                    if (data.descriptionKeyId != null) {
                        description.text = itemView.context.getText(data.descriptionKeyId)
                    } else {
                        description.visibility = View.GONE
                    }
                    switch.isChecked = data.value
                    seekBar.visibility = View.GONE

                    switch.setOnCheckedChangeListener(data.onCheckedChanged)
                }
                is AlarmSettingData.PercentData -> {
                    description.visibility = View.GONE
                    switch.visibility = View.GONE
                    seekBar.progress = data.value
                    seekBar.max = data.max
                    val listener = OnSeekBarChangeListenerBuilder()
                        .apply(data.builder)
                        .build()
                    seekBar.setOnSeekBarChangeListener(listener)
                }
            }
        }
    }

    override fun getItemCount(): Int = dataSet.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_aram_setting_name)
        val description: TextView = view.findViewById(R.id.item_aram_setiing_description)
        val switch: SwitchMaterial = view.findViewById(R.id.item_aram_setting_switch)
        val seekBar: SeekBar = view.findViewById(R.id.item_aram_setting_seekbar)
    }

    class SettingTimePickerFragment(
        private val current: String,
        val onTimeSetListener: (TimePicker?, Int, Int) -> Unit
    ) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val (hour, minute) = current.split(":").map { it.toInt() }
            return TimePickerDialog(
                activity,
                this,
                hour,
                minute,
                DateFormat.is24HourFormat(activity)
            )
        }

        override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
            onTimeSetListener(view, hourOfDay, minute)
        }
    }
}