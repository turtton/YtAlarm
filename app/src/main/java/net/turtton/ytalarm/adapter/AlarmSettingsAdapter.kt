package net.turtton.ytalarm.adapter

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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import net.turtton.ytalarm.R
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.structure.AramSettingData
import net.turtton.ytalarm.util.OnSeekBarChangeListenerBuilder

class AlarmSettingsAdapter(private val alarm: Alarm, private val parentFragment: Fragment) :
    RecyclerView.Adapter<AlarmSettingsAdapter.ViewHolder>() {

    private val dataSet: Array<AramSettingData>

    init {
        val timeSettingData = AramSettingData.NormalData(R.string.setting_time, alarm.time) {
            SettingTimePickerFragment(alarm.time) { _, hourOfDay, minute ->
                val newTime = String.format("%02d:%02d", hourOfDay, minute)
                alarm.time = newTime
                it.findViewById<TextView>(R.id.item_aram_setiing_description).text = newTime
            }.show(parentFragment.parentFragmentManager, "settingTimePicker")
        }
        val loopSettingData =
            AramSettingData.ToggleData(R.string.setting_loop, alarm.loop) { _, value ->
                alarm.loop = value
            }
        val volumeSettingData =
            AramSettingData.PercentData(R.string.setting_volume, alarm.volume, 100) {
                onProgressChanged = { _, progress, isUser ->
                    if (isUser) {
                        alarm.volume = progress
                    }
                }
            }

        dataSet = arrayOf(timeSettingData, loopSettingData, volumeSettingData)
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
                is AramSettingData.NormalData -> {
                    description.text = data.value
                    switch.visibility = View.GONE
                    seekBar.visibility = View.GONE
                    if (data.onClick != null) {
                        itemView.setOnClickListener(data.onClick)
                    }
                }
                is AramSettingData.ToggleData -> {
                    if (data.descriptionKeyId != null) {
                        description.text = itemView.context.getText(data.descriptionKeyId)
                    } else {
                        description.visibility = View.GONE
                    }
                    switch.isChecked = data.value
                    seekBar.visibility = View.GONE

                    switch.setOnCheckedChangeListener(data.onCheckedChanged)
                }
                is AramSettingData.PercentData -> {
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