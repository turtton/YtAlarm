package net.turtton.ytalarm.ui.compose.screens

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.components.AlarmItem
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.AlarmScheduleError
import net.turtton.ytalarm.util.extensions.alarmOrderRule
import net.turtton.ytalarm.util.extensions.alarmOrderUp
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.order.AlarmOrder
import net.turtton.ytalarm.util.updateAlarmSchedule
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import kotlin.coroutines.cancellation.CancellationException

/**
 * アラーム一覧画面のコンテンツ（プレビュー可能）
 *
 * ViewModelに依存せず、すべてのデータと関数を引数として受け取る純粋なComposable。
 * これにより、@Previewアノテーションでプレビュー可能になる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreenContent(
    alarms: List<Alarm>,
    orderRule: AlarmOrder,
    orderUp: Boolean,
    snackbarHostState: SnackbarHostState,
    onAlarmToggle: (Alarm, Boolean) -> Unit,
    onAlarmClick: (Long) -> Unit,
    onAlarmLongClick: (Alarm) -> Unit,
    onOpenDrawer: () -> Unit,
    onSortRuleChange: (AlarmOrder) -> Unit,
    onOrderUpToggle: () -> Unit,
    onCreateAlarm: () -> Unit,
    modifier: Modifier = Modifier,
    playlistViewModel: PlaylistViewModel? = null,
    videoViewModel: VideoViewModel? = null
) {
    val scope = rememberCoroutineScope()
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_alarm_list)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // 並び替えボタン
                    IconButton(onClick = onOrderUpToggle) {
                        Icon(
                            imageVector = if (orderUp) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (orderUp) {
                                "Sort ascending"
                            } else {
                                "Sort descending"
                            }
                        )
                    }
                    // ソートルール選択ボタン
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort rule")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateAlarm
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add alarm")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (alarms.isEmpty()) {
                Text(
                    text = stringResource(R.string.alarm_list_empty_message),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = alarms,
                        key = { it.id }
                    ) { alarm ->
                        // プレイリスト名とサムネイルを非同期で取得
                        val playlists = remember(alarm.playListId) {
                            mutableStateOf<List<Playlist>>(
                                emptyList()
                            )
                        }

                        LaunchedEffect(alarm.playListId) {
                            playlistViewModel?.getFromIdsAsync(alarm.playListId)?.let { deferred ->
                                playlists.value = withContext(Dispatchers.IO) {
                                    deferred.await()
                                }
                            }
                        }

                        val playlistTitle = playlists.value.firstOrNull()?.title ?: ""

                        // サムネイル取得
                        val thumbnailUrl = remember(playlists.value.firstOrNull()?.thumbnail) {
                            mutableStateOf<Any?>(null)
                        }

                        playlists.value.firstOrNull()?.thumbnail?.let { thumbnail ->
                            when (thumbnail) {
                                is Playlist.Thumbnail.Video -> {
                                    // Video thumbnailの場合、VideoからURLを取得
                                    LaunchedEffect(thumbnail.id) {
                                        videoViewModel?.getFromIdAsync(
                                            thumbnail.id
                                        )?.let { deferred ->
                                            val video = withContext(Dispatchers.IO) {
                                                deferred.await()
                                            }
                                            thumbnailUrl.value = video?.thumbnailUrl
                                        }
                                    }
                                }

                                is Playlist.Thumbnail.Drawable -> {
                                    thumbnailUrl.value = thumbnail.id
                                }
                            }
                        }

                        AlarmItem(
                            alarm = alarm,
                            playlistTitle = playlistTitle,
                            thumbnailUrl = thumbnailUrl.value,
                            onToggle = { isEnabled ->
                                onAlarmToggle(alarm, isEnabled)
                            },
                            onClick = {
                                onAlarmClick(alarm.id)
                            },
                            onLongClick = {
                                onAlarmLongClick(alarm)
                            }
                        )
                    }
                }
            }
        }
    }

    // ソートルール選択ダイアログ
    if (showSortDialog) {
        val sortOptions = stringArrayResource(R.array.dialog_alarm_order)
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.menu_alarm_option_sortrule)) },
            text = {
                Column {
                    sortOptions.forEachIndexed { index, option ->
                        RadioButton(
                            selected = orderRule.ordinal == index,
                            onClick = {
                                onSortRuleChange(AlarmOrder.values()[index])
                                showSortDialog = false
                            }
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * アラーム一覧画面（Compose版）
 *
 * 機能:
 * - アラーム一覧表示（LazyColumn）
 * - ソート機能（時刻、作成日、最終更新日）
 * - 並び替え（昇順/降順）
 * - アラームのON/OFF切り替え
 * - 新規アラーム作成（FAB）
 * - アラーム設定画面へのナビゲーション
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onNavigateToAlarmSettings: (alarmId: Long) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    ),
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-fetch string resources for use in lambdas
    val errorNoAlarmPermission = stringResource(R.string.snackbar_error_no_alarm_permission)
    val errorNoAlarmManager = stringResource(R.string.error_no_alarm_manager)
    val msgAlarmDeleted = stringResource(R.string.message_alarm_deleted)
    val errorDeleteFailed = stringResource(R.string.error_delete_alarm_failed)

    // 削除対象のアラーム
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }

    val allAlarms by alarmViewModel.allAlarms.observeAsState(emptyList())

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.alarmOrderRule
    val orderUp = preferences.alarmOrderUp

    // スヌーズアラームを除外してソート
    val sortedAlarms = remember(allAlarms, orderRule, orderUp) {
        val filtered: List<Alarm> = allAlarms.filter { it.repeatType !is Alarm.RepeatType.Snooze }
        val mutableList: MutableList<Alarm> = filtered.toMutableList()
        when (orderRule) {
            AlarmOrder.TIME -> mutableList.sortBy { "${it.hour}${it.minute}".toInt() }
            AlarmOrder.CREATION_DATE -> mutableList.sortBy { it.creationDate }
            AlarmOrder.LAST_UPDATED -> mutableList.sortBy { it.lastUpdated }
        }
        if (!orderUp) {
            mutableList.reverse()
        }
        mutableList
    }

    // AlarmListScreenContentを呼び出す
    AlarmListScreenContent(
        alarms = sortedAlarms,
        orderRule = orderRule,
        orderUp = orderUp,
        snackbarHostState = snackbarHostState,
        onAlarmToggle = { alarm, isEnabled ->
            scope.launch(Dispatchers.IO) {
                // 有効化時のみ権限チェック
                if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(errorNoAlarmPermission)
                        }
                        return@launch // トグルを更新しない
                    }
                }

                val updatedAlarm = alarm.copy(isEnable = isEnabled)
                alarmViewModel.update(updatedAlarm)

                // 全アラームを取得して、同一IDのデータを更新済みのものに差し替え
                val allAlarms = alarmViewModel.getAllAlarmsAsync().await()
                val updatedList = allAlarms.map {
                    if (it.id == alarm.id) updatedAlarm else it
                }.filter { it.isEnable }

                updateAlarmSchedule(context, updatedList).onLeft { error ->
                    val message = when (error) {
                        is AlarmScheduleError.PermissionDenied -> error.message
                        AlarmScheduleError.NoAlarmManager -> errorNoAlarmManager
                        AlarmScheduleError.NoEnabledAlarm -> null
                    }
                    message?.let {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        },
        onAlarmClick = onNavigateToAlarmSettings,
        onAlarmLongClick = { alarm ->
            alarmToDelete = alarm
        },
        onOpenDrawer = onOpenDrawer,
        onSortRuleChange = { rule ->
            preferences.alarmOrderRule = rule
        },
        onOrderUpToggle = {
            preferences.alarmOrderUp = !orderUp
        },
        onCreateAlarm = {
            onNavigateToAlarmSettings(-1)
        },
        modifier = modifier,
        playlistViewModel = playlistViewModel,
        videoViewModel = videoViewModel
    )

    // 削除確認ダイアログ
    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_alarm_title)) },
            text = {
                Text(stringResource(R.string.dialog_delete_alarm_message, alarm.hour, alarm.minute))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                ensureActive()
                                // DB削除（完了を待つ）
                                alarmViewModel.delete(alarm).join()
                                ensureActive()

                                // 残りの有効なアラームでスケジュール更新
                                val remainingAlarms = alarmViewModel.getAllAlarmsAsync().await()
                                    .filter { it.isEnable }
                                ensureActive()

                                updateAlarmSchedule(context, remainingAlarms).onLeft { error ->
                                    val message = when (error) {
                                        is AlarmScheduleError.PermissionDenied -> error.message
                                        AlarmScheduleError.NoAlarmManager -> errorNoAlarmManager
                                        AlarmScheduleError.NoEnabledAlarm -> null
                                    }
                                    message?.let { msg ->
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    alarmToDelete = null
                                    snackbarHostState.showSnackbar(msgAlarmDeleted)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                Log.e("AlarmListScreen", "Failed to delete alarm", e)
                                withContext(Dispatchers.Main) {
                                    alarmToDelete = null
                                    snackbarHostState.showSnackbar(errorDeleteFailed)
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_remove_video_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text(stringResource(R.string.dialog_remove_video_negative))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmListScreenPreview() {
    AppTheme {
        // ダミーデータを作成
        val dummyAlarms = listOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 30,
                repeatType = Alarm.RepeatType.Days(
                    listOf(
                        net.turtton.ytalarm.util.DayOfWeekCompat.MONDAY,
                        net.turtton.ytalarm.util.DayOfWeekCompat.TUESDAY,
                        net.turtton.ytalarm.util.DayOfWeekCompat.WEDNESDAY,
                        net.turtton.ytalarm.util.DayOfWeekCompat.THURSDAY,
                        net.turtton.ytalarm.util.DayOfWeekCompat.FRIDAY
                    )
                ),
                playListId = listOf(1L),
                isEnable = true,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            ),
            Alarm(
                id = 2L,
                hour = 9,
                minute = 0,
                repeatType = Alarm.RepeatType.Everyday,
                playListId = listOf(2L),
                isEnable = false,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            ),
            Alarm(
                id = 3L,
                hour = 18,
                minute = 45,
                repeatType = Alarm.RepeatType.Once,
                playListId = listOf(3L),
                isEnable = true,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            )
        )

        AlarmListScreenContent(
            alarms = dummyAlarms,
            orderRule = AlarmOrder.TIME,
            orderUp = true,
            snackbarHostState = remember { SnackbarHostState() },
            onAlarmToggle = { _, _ -> },
            onAlarmClick = { },
            onAlarmLongClick = { },
            onOpenDrawer = { },
            onSortRuleChange = { },
            onOrderUpToggle = { },
            onCreateAlarm = { }
        )
    }
}