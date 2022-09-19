package net.turtton.ytalarm.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.adapter.AlarmListAdapter
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory

class FragmentAlarmList : FragmentAbstractList() {

    val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(view.context)
        val adapter = AlarmListAdapter(this)
        binding.recyclerList.layoutManager = layoutManager
        binding.recyclerList.adapter = adapter
        alarmViewModel.allAlarms.observe(requireActivity()) { list ->
            list?.let {
                adapter.submitList(it)
            }
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            val action = FragmentAlarmListDirections
                .actionAlarmListFragmentToAlarmSettingsFragment(-1)
            findNavController().navigate(action)
        }
    }
}