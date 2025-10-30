package net.turtton.ytalarm.ui.fragment

import android.app.AlertDialog
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.ui.adapter.AlarmListComposeAdapter
import net.turtton.ytalarm.ui.menu.MenuProviderContainer
import net.turtton.ytalarm.util.extensions.alarmOrderRule
import net.turtton.ytalarm.util.extensions.alarmOrderUp
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.showMessageIfNull
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.util.order.AlarmOrder
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentAlarmList : FragmentAbstractList(), MenuProviderContainer {
    private lateinit var adapter: ListAdapter<Alarm, *>

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
    override var menuProvider: MenuProvider? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        activity.binding.fab.show()

        val layoutManager = LinearLayoutManager(view.context)
        // Compose版のAdapterを使用してテスト
        adapter = AlarmListComposeAdapter(this)
        binding.recyclerList.layoutManager = layoutManager
        binding.recyclerList.adapter = adapter
        val allAlarms = alarmViewModel.allAlarms
        updateObserver(view)
        allAlarms.observeAlarm(viewLifecycleOwner, requireContext())

        AlarmListMenuProvider(this, view).also {
            menuProvider = it
            activity.addMenuProvider(it, viewLifecycleOwner)
        }

        val fab = (requireActivity() as MainActivity).binding.fab
        fab.shrink()
        fab.setOnClickListener {
            val action = FragmentAlarmListDirections
                .actionAlarmListFragmentToAlarmSettingsFragment(-1)
            findNavController().navigate(action)
        }
    }

    private fun updateObserver(view: View) {
        alarmViewModel.allAlarms.observe(viewLifecycleOwner) { alarmList ->
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
            alarmList.filter {
                it.repeatType !is Alarm.RepeatType.Snooze
            }.let { immutable ->
                val targetList = immutable.toMutableList()
                val preferences = activity?.privatePreferences ?: kotlin.run {
                    val message =
                        R.string.snackbar_error_failed_to_access_settings_data
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@let
                }
                when (preferences.alarmOrderRule) {
                    AlarmOrder.TIME -> targetList.sortBy { "${it.hour}${it.minute}".toInt() }
                    AlarmOrder.CREATION_DATE -> targetList.sortBy { it.creationDate }
                    AlarmOrder.LAST_UPDATED -> targetList.sortBy { it.lastUpdated }
                }
                if (!preferences.alarmOrderUp) {
                    targetList.reverse()
                }
                adapter.submitList(targetList)
            }
        }
    }

    private class AlarmListMenuProvider(
        private val fragment: FragmentAlarmList,
        private val view: View
    ) : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_alarm_option, menu)
            menu.forEach {
                val icon = it.icon ?: return@forEach
                icon.mutate()
                val colorRes = fragment.resources.getColor(R.color.white, null)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    icon.colorFilter = BlendModeColorFilter(colorRes, BlendMode.SRC_ATOP)
                } else {
                    icon.colorFilter = PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_ATOP)
                }

                if (it.itemId == R.id.menu_alarm_option_order) {
                    val preferences = fragment.activity?.privatePreferences ?: kotlin.run {
                        val message = R.string.snackbar_error_failed_to_access_settings_data
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                        fragment.findNavController().navigateUp()
                        return
                    }
                    val drawable = if (preferences.alarmOrderUp) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    it.setIcon(drawable)
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_alarm_option_order -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val next = !preferences.alarmOrderUp
                    preferences.alarmOrderUp = next
                    val drawable = if (next) {
                        R.drawable.ic_arrow_upward
                    } else {
                        R.drawable.ic_arrow_downward
                    }
                    menuItem.setIcon(drawable)
                    fragment.updateObserver(view)
                    true
                }
                R.id.menu_alarm_option_sortrule -> {
                    val preferences = fragment.activity?.privatePreferences.showMessageIfNull(view)
                        ?: kotlin.run {
                            fragment.findNavController().navigateUp()
                            return false
                        }
                    val current = preferences.alarmOrderRule
                    val selectionString = R.array.dialog_alarm_order
                    AlertDialog.Builder(view.context)
                        .setSingleChoiceItems(
                            selectionString,
                            current.ordinal
                        ) { dialog, selected ->
                            preferences.alarmOrderRule = AlarmOrder.values()[selected]
                            dialog.dismiss()
                            fragment.updateObserver(view)
                        }.show()
                    true
                }
                else -> false
            }
        }
    }
}