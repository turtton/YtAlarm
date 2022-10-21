package net.turtton.ytalarm.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.ui.adapter.AlarmListAdapter
import net.turtton.ytalarm.util.RepeatType
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAlarmList : FragmentAbstractList() {

    val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    private val prevList = MutableStateFlow(mapOf<Long, Boolean>())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        activity.binding.fab.show()

        val layoutManager = LinearLayoutManager(view.context)
        val adapter = AlarmListAdapter(this)
        binding.recyclerList.layoutManager = layoutManager
        binding.recyclerList.adapter = adapter
        val allAlarms = alarmViewModel.allAlarms
        allAlarms.observe(viewLifecycleOwner) { alarmList ->
            if (alarmList == null) return@observe
            val compList = alarmList.associate { it.id to it.isEnable }
            val currentState = prevList.value
            prevList.update { compList }
            if (alarmList.size == currentState.size) {
                val isEnableChanged = compList.any { (key, isEnable) ->
                    currentState.contains(key) && currentState[key] != isEnable
                }
                if (isEnableChanged) {
                    return@observe
                }
            }
            prevList.update { compList }
            alarmList.filter { it.repeatType !is RepeatType.Snooze }
                .let { adapter.submitList(it) }
        }
        allAlarms.observeAlarm(viewLifecycleOwner, requireContext())

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            val action = FragmentAlarmListDirections
                .actionAlarmListFragmentToAlarmSettingsFragment(-1)
            findNavController().navigate(action)
        }
    }
}