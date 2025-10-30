package net.turtton.ytalarm.ui.adapter

import android.util.Log
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.components.AlarmItem
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.ui.fragment.FragmentAlarmList
import net.turtton.ytalarm.ui.fragment.FragmentAlarmListDirections
import net.turtton.ytalarm.util.extensions.joinStringWithSlash

/**
 * Compose版のAlarmListAdapter
 * テスト用にComposeViewを使用してAlarmItemを表示
 */
class AlarmListComposeAdapter(
    private val parentFragment: FragmentAlarmList
) : ListAdapter<Alarm, AlarmListComposeAdapter.ComposeViewHolder>(BasicComparator<Alarm>()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
        val composeView = ComposeView(parent.context)
        return ComposeViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        val alarm = getItem(position)
        holder.bind(alarm, parentFragment)
    }

    class ComposeViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {

        fun bind(alarm: Alarm, parentFragment: FragmentAlarmList) {
            // プレイリスト情報とサムネイルを取得
            val async = parentFragment.playlistViewModel.getFromIdsAsync(alarm.playListId)

            parentFragment.lifecycleScope.launch {
                val playlists = async.await()
                val playlistTitle = playlists.map { it.title }.joinStringWithSlash()

                // サムネイルURLを取得
                val thumbnailUrl = if (playlists.isNotEmpty()) {
                    when (val thumbnail = playlists.first().thumbnail) {
                        is Playlist.Thumbnail.Video -> {
                            parentFragment.videoViewModel
                                .getFromIdAsync(thumbnail.id)
                                .await()
                                ?.thumbnailUrl
                        }
                        is Playlist.Thumbnail.Drawable -> {
                            thumbnail.id // Drawableリソースはそのまま渡す
                        }
                    }
                } else {
                    null
                }

                launch(Dispatchers.Main) {
                    composeView.setContent {
                        AppTheme {
                            AlarmItem(
                                alarm = alarm,
                                playlistTitle = playlistTitle,
                                thumbnailUrl = thumbnailUrl,
                                onToggle = { isChecked ->
                                    handleToggle(alarm, isChecked, parentFragment)
                                },
                                onClick = {
                                    val action = FragmentAlarmListDirections
                                        .actionAlarmListFragmentToAlarmSettingsFragment(alarm.id)
                                    parentFragment.findNavController().navigate(action)
                                }
                            )
                        }
                    }
                }
            }
        }

        private fun handleToggle(
            alarm: Alarm,
            isChecked: Boolean,
            parentFragment: FragmentAlarmList
        ) {
            val async = parentFragment.alarmViewModel.getFromIdAsync(alarm.id)
            parentFragment.lifecycleScope.launch {
                async.await()?.copy(isEnable = isChecked)?.let {
                    parentFragment.alarmViewModel.update(it).join()
                } ?: run {
                    val message = R.string.snackbar_error_failed_to_get_alarm
                    Snackbar.make(composeView, message, Snackbar.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to get alarm. Id: ${alarm.id}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "AlarmListComposeAdapter"
    }
}