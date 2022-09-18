package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.AlarmSettingsAdapter
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory

class FragmentAlarmSettings : FragmentAbstractList() {
    val alarmData = MutableStateFlow(Alarm())

    private val args by navArgs<FragmentAlarmSettingsArgs>()

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fab = (requireActivity() as MainActivity).binding.fab
        fab.extend()

        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerList.layoutManager = layoutManager

        fab.visibility = View.GONE

        val alarmId = args.alarmId
        if (alarmId != -1L) {
            val async = alarmViewModel.getFromIdAsync(alarmId)
            lifecycleScope.launch {
                val alarm = async.await()
                alarmData.update { alarm }
                launch(Dispatchers.Main) {
                    binding.recyclerList.adapter = AlarmSettingsAdapter(this@FragmentAlarmSettings)
                    fab.visibility = View.VISIBLE
                }
            }
        } else {
            binding.recyclerList.adapter = AlarmSettingsAdapter(this)
            fab.visibility = View.VISIBLE
        }
        fab.setOnClickListener {
            val currentData = alarmData.value
            if (currentData.id == null) {
                alarmViewModel.insert(currentData)
            } else {
                alarmViewModel.update(currentData)
            }
            findNavController().navigate(R.id.action_AlarmSettingFragment_to_AlarmListFragment)
        }
    }
}