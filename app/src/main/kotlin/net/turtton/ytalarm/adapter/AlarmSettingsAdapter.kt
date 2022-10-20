package net.turtton.ytalarm.adapter

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.fragment.FragmentAlarmSettings
import net.turtton.ytalarm.fragment.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.structure.AlarmSettingData
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.util.OnSeekBarChangeListenerBuilder
import net.turtton.ytalarm.util.RepeatType
import net.turtton.ytalarm.util.extensions.getDisplayTime
import net.turtton.ytalarm.util.extensions.joinStringWithSlash
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class AlarmSettingsAdapter(
    private val fragment: FragmentAlarmSettings,
    playlistName: String? = null
) : RecyclerView.Adapter<AlarmSettingsAdapter.ViewHolder>() {

    private val dataSet: Array<AlarmSettingData>

    init {
        val alarmState = fragment.alarmData
        val alarm = alarmState.value
        val displayTime = alarm.getDisplayTime()
        val timeSelector =
            AlarmSettingData.NormalData(R.string.setting_time, displayTime) { _, description ->
                SettingTimePickerFragment(displayTime) { _, hourOfDay, minute ->
                    description.text = alarmState.updateAndGet {
                        it.copy(hour = hourOfDay, minute = minute)
                    }.getDisplayTime()
                }.show(fragment.parentFragmentManager, "settingTimePicker")
            }

        val context = fragment.requireContext()
        val repeatDisplay = alarm.repeatType.getDisplay(context)
        val repeatTypeSelector =
            AlarmSettingData.NormalData(R.string.setting_repeat, repeatDisplay) { _, description ->
                RepeatTypePickFragment(fragment, alarmState, description)
                    .show(fragment.childFragmentManager, "RepeatTypePicker")
            }

        val plName = playlistName ?: "Nothing"
        val playlistSelector =
            AlarmSettingData.NormalData(R.string.setting_playlist, plName) { _, description ->
                val async = fragment.playlistViewModel.allPlaylistsAsync
                fragment.lifecycleScope.launch {
                    val playlists = async.await()
                    launch(Dispatchers.Main) {
                        val displayData = playlists.map {
                            it.toDisplayData(fragment.videoViewModel)
                        }
                        val chosenList = playlists.map {
                            alarmState.value.playListId.contains(it.id)
                        }
                        DialogMultiChoiceVideo(displayData, chosenList) { _, ids ->
                            alarmState.update {
                                it.copy(playListId = ids.toList())
                            }
                            val currentId = alarmState.value.playListId
                            description.text = playlists.filter { currentId.contains(it.id) }
                                .map { it.title }
                                .joinStringWithSlash()
                        }.show(fragment.childFragmentManager, "MultiChoicePlaylist")
                    }
                }
            }

        val loopToggle =
            AlarmSettingData.ToggleData(R.string.setting_loop, alarm.shouldLoop) { _, value ->
                alarmState.update {
                    it.copy(shouldLoop = value)
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
        val getSnoozeMinute = { minute: Int ->
            context.resources.getQuantityString(R.plurals.setting_snooze_time, minute, minute)
        }
        val snoozeTitle = R.string.setting_snooze
        val snoozeTime =
            AlarmSettingData.NormalData(
                snoozeTitle,
                getSnoozeMinute(alarm.snoozeMinute)
            ) { _, description ->
                val numberPicker = NumberPicker(context)
                numberPicker.value = alarmState.value.snoozeMinute
                numberPicker.maxValue = 60
                numberPicker.minValue = 1

                AlertDialog.Builder(context)
                    .setTitle(snoozeTitle)
                    .setView(numberPicker)
                    .setPositiveButton(R.string.dialog_snooze_time_input_ok) { _, _ ->
                        numberPicker.clearFocus()
                        val minute = numberPicker.value
                        alarmState.update {
                            it.copy(snoozeMinute = minute)
                        }
                        description.text = getSnoozeMinute(minute)
                    }.setNegativeButton(R.string.dialog_snooze_time_input_cancel) { _, _ -> }
                    .show()
            }

        dataSet = arrayOf(
            timeSelector,
            repeatTypeSelector,
            playlistSelector,
            loopToggle,
            volumeProgress,
            snoozeTime
        )
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

    class RepeatTypePickFragment(
        private val fragment: Fragment,
        private val alarmState: MutableStateFlow<Alarm>,
        private val description: TextView
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = requireContext()
            return AlertDialog.Builder(fragment.activity)
                .setTitle(R.string.dialog_repeat_choice_title)
                .setItems(R.array.dialog_repeat_type_choice) { _, index ->
                    when (index) {
                        // ONCE
                        0 -> {
                            alarmState.update {
                                it.copy(repeatType = RepeatType.Once)
                            }
                            description.text = alarmState.value.repeatType.getDisplay(context)
                        }
                        // EVERYDAY
                        1 -> {
                            alarmState.update {
                                it.copy(repeatType = RepeatType.Everyday)
                            }
                            description.text = alarmState.value.repeatType.getDisplay(context)
                        }
                        // DAYS
                        2 -> {
                            val current = alarmState.value.repeatType as? RepeatType.Days
                            DayChoiceFragment(current?.days) { dayOfWeekCompats ->
                                if (dayOfWeekCompats.size == 7) {
                                    alarmState.update {
                                        it.copy(repeatType = RepeatType.Everyday)
                                    }
                                } else {
                                    alarmState.update {
                                        it.copy(repeatType = RepeatType.Days(dayOfWeekCompats))
                                    }
                                }
                                description.text = alarmState.value.repeatType.getDisplay(
                                    context
                                )
                            }.show(fragment.childFragmentManager, "DayChoice")
                        }
                        // DATE
                        3 -> {
                            val current = alarmState.value.repeatType as? RepeatType.Date
                            SettingDatePickerFragment(current?.targetDate) { _, year, month, day ->
                                val nowCalendar = Calendar.getInstance()
                                val nowYear = nowCalendar[Calendar.YEAR]
                                val nowDay = nowCalendar[Calendar.DAY_OF_YEAR]
                                val calendar = GregorianCalendar(year, month, day)
                                val dayOfYear = calendar[Calendar.DAY_OF_YEAR]
                                val isPastDate = nowYear == year && nowDay > dayOfYear
                                val newDate = if (nowYear > year || isPastDate) {
                                    val pastError = R.string.snackbar_error_target_is_the_past_date
                                    Snackbar.make(fragment.requireView(), pastError, 600)
                                        .show()
                                    Date(nowCalendar.timeInMillis)
                                } else {
                                    Date(calendar.timeInMillis)
                                }
                                alarmState.update {
                                    it.copy(repeatType = RepeatType.Date(newDate))
                                }
                                description.text = alarmState.value.repeatType.getDisplay(
                                    context
                                )
                            }.show(fragment.childFragmentManager, "DatePicker")
                        }
                        else -> {
                            Log.e(
                                "AlarmSettingAdapter",
                                "RepeatTypeSelector OutOfBoundsException index:$index"
                            )
                        }
                    }
                }.create()
        }
    }

    class DayChoiceFragment(
        current: List<DayOfWeekCompat>? = null,
        val onConfirm: (List<DayOfWeekCompat>) -> Unit
    ) : DialogFragment() {
        private val checkedItem = BooleanArray(7) { false }
        private val currentDay = arrayListOf<DayOfWeekCompat>()

        init {
            current?.let {
                it.forEach { day ->
                    when (day) {
                        DayOfWeekCompat.MONDAY -> checkedItem[0] = true
                        DayOfWeekCompat.TUESDAY -> checkedItem[1] = true
                        DayOfWeekCompat.WEDNESDAY -> checkedItem[2] = true
                        DayOfWeekCompat.THURSDAY -> checkedItem[3] = true
                        DayOfWeekCompat.FRIDAY -> checkedItem[4] = true
                        DayOfWeekCompat.SATURDAY -> checkedItem[5] = true
                        DayOfWeekCompat.SUNDAY -> checkedItem[6] = true
                    }
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_repeat_days_title)
                .setMultiChoiceItems(
                    R.array.dialog_repeat_days_choice,
                    checkedItem
                ) { _, index, isChecked ->
                    val addOrRemove: (DayOfWeekCompat) -> Unit = {
                        if (isChecked) currentDay += it else currentDay.remove(it)
                    }
                    when (index) {
                        0 -> addOrRemove(DayOfWeekCompat.MONDAY)
                        1 -> addOrRemove(DayOfWeekCompat.TUESDAY)
                        2 -> addOrRemove(DayOfWeekCompat.WEDNESDAY)
                        3 -> addOrRemove(DayOfWeekCompat.THURSDAY)
                        4 -> addOrRemove(DayOfWeekCompat.FRIDAY)
                        5 -> addOrRemove(DayOfWeekCompat.SATURDAY)
                        6 -> addOrRemove(DayOfWeekCompat.SUNDAY)
                    }
                }.setPositiveButton(R.string.dialog_repeat_days_ok) { _, _ ->
                    onConfirm(currentDay.toList())
                }.setNegativeButton(R.string.dialog_repeat_days_cancel) { _, _ -> }
                .create()
        }
    }

    class SettingDatePickerFragment(
        private val current: Date? = null,
        val onDateSetListener: (DatePicker?, year: Int, month: Int, dayOfMonth: Int) -> Unit
    ) : DialogFragment(), DatePickerDialog.OnDateSetListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val calendar = Calendar.getInstance()
            current?.let {
                calendar.time = it
            }
            return DatePickerDialog(
                requireActivity(),
                this,
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            )
        }

        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            onDateSetListener(view, year, month, dayOfMonth)
        }
    }
}