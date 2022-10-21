package net.turtton.ytalarm.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.ui.adapter.AlarmSettingsAdapter
import net.turtton.ytalarm.util.extensions.joinStringWithSlash
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewContainer
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAlarmSettings : FragmentAbstractList(), PlaylistViewContainer {
    val alarmData = MutableStateFlow(Alarm())

    private val args by navArgs<FragmentAlarmSettingsArgs>()

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    override val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainActivity = requireActivity() as MainActivity
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val fab = mainActivity.binding.fab
        fab.extend()
        fab.visibility = View.GONE

        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerList.layoutManager = layoutManager

        alarmViewModel.allAlarms.observeAlarm(viewLifecycleOwner, view.context)

        val alarmId = args.alarmId
        if (alarmId != -1L) {
            val async = alarmViewModel.getFromIdAsync(alarmId)
            lifecycleScope.launch {
                val alarm = async.await()
                alarmData.update { alarm }
                val plTitle = playlistViewModel.getFromIdsAsync(alarm.playListId)
                    .await()
                    .map { it.title }
                    .joinStringWithSlash()
                launch(Dispatchers.Main) {
                    val adapter = AlarmSettingsAdapter(this@FragmentAlarmSettings, plTitle)
                    binding.recyclerList.adapter = adapter
                    fab.visibility = View.VISIBLE
                }
            }
        } else {
            binding.recyclerList.adapter = AlarmSettingsAdapter(this)
            fab.visibility = View.VISIBLE
        }
        fab.setOnClickListener {
            val currentData = alarmData.value
            if (currentData.playListId.isEmpty()) {
                Snackbar.make(view, R.string.snackbar_error_playlistid_is_null, 300).show()
                return@setOnClickListener
            }
            if (currentData.id == 0L) {
                alarmViewModel.insert(currentData)
            } else {
                alarmViewModel.update(currentData)
            }
            findNavController().navigate(R.id.action_AlarmSettingFragment_to_AlarmListFragment)
        }
    }
}