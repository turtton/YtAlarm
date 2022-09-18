package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.AlarmSettingsAdapter
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory

class FragmentAlarmSettings : FragmentAbstractList() {
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
            alarmViewModel.getFromId(alarmId).observe(viewLifecycleOwner) { uncheckedAlarm ->
                uncheckedAlarm?.let { alarm ->
                    binding.recyclerList.adapter = AlarmSettingsAdapter(alarm, this)
                    fab.setOnClickListener {
                        alarmViewModel.update(alarm)
                        findNavController().navigate(
                            R.id.action_AlarmSettingFragment_to_AlarmListFragment
                        )
                    }
                    fab.visibility = View.VISIBLE
                }
            }
        } else {
            val alarm = Alarm()
            binding.recyclerList.adapter = AlarmSettingsAdapter(alarm, this)
            fab.setOnClickListener {
                alarmViewModel.insert(alarm)
                findNavController().navigate(R.id.action_AlarmSettingFragment_to_AlarmListFragment)
            }
            fab.visibility = View.VISIBLE
        }
    }
}