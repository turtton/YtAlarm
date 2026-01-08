package net.turtton.ytalarm.ui.compose.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.components.ClickableSettingItem
import net.turtton.ytalarm.ui.compose.components.SliderSettingItem
import net.turtton.ytalarm.ui.compose.components.SwitchSettingItem
import net.turtton.ytalarm.ui.compose.dialogs.AlarmDatePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.DayOfWeekPickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.DisplayDataThumbnail
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeDialog
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeSelection
import net.turtton.ytalarm.ui.compose.dialogs.SnoozeMinutePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.TimePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.VibrationWarningDialog
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.ui.compose.theme.Dimensions
import net.turtton.ytalarm.util.AlarmScheduleError
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.util.updateAlarmSchedule
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import java.util.Calendar
import java.util.Date

/**
 * AlarmSettingsScreenContent
 *
 * プレビュー可能なAlarm設定画面のコンテンツ部分。
 * ViewModelに依存せず、すべてのデータと操作をパラメータとして受け取る。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreenContent(
    alarm: Alarm,
    playlistTitle: String,
    snackbarHostState: SnackbarHostState,
    onAlarmChange: (Alarm) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    onPlaylistSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pre-fetch string resources for use in lambdas
    val errorPastDate = stringResource(R.string.snackbar_error_target_is_the_past_date)

    // ダイアログ表示状態
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showRepeatTypeDialog by remember { mutableStateOf(false) }
    var showDayOfWeekDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showVibrationWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (alarm.id == 0L) {
                            stringResource(R.string.nav_new_alarm)
                        } else {
                            stringResource(R.string.nav_edit_alarm)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = Dimensions.fabContentPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 0. 有効/無効トグル
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_enabled),
                    description = null,
                    checked = alarm.isEnable,
                    onCheckedChange = { onAlarmChange(alarm.copy(isEnable = it)) }
                )
            }

            // 1. 時刻設定
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_time),
                    description = "%02d:%02d".format(alarm.hour, alarm.minute),
                    onClick = { showTimePickerDialog = true }
                )
            }

            // 2. 繰り返し設定
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_repeat),
                    description = alarm.repeatType.getDisplay(context),
                    onClick = { showRepeatTypeDialog = true }
                )
            }

            // 3. プレイリスト選択
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_playlist),
                    description = playlistTitle.ifEmpty {
                        stringResource(
                            R.string.setting_playlist_empty
                        )
                    },
                    onClick = onPlaylistSelect
                )
            }

            // 4. ループ
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_loop),
                    description = null,
                    checked = alarm.shouldLoop,
                    onCheckedChange = { onAlarmChange(alarm.copy(shouldLoop = it)) }
                )
            }

            // 5. シャッフル
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_shuffle),
                    description = null,
                    checked = alarm.shouldShuffle,
                    onCheckedChange = { onAlarmChange(alarm.copy(shouldShuffle = it)) }
                )
            }

            // 6. 音量
            item {
                SliderSettingItem(
                    title = stringResource(R.string.setting_volume),
                    description = "${alarm.volume.volume}%",
                    value = alarm.volume.volume.toFloat(),
                    valueRange = 0f..100f,
                    onValueChange = {
                        onAlarmChange(alarm.copy(volume = Alarm.Volume(it.toInt())))
                    }
                )
            }

            // 7. スヌーズ
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_snooze),
                    description = pluralStringResource(
                        R.plurals.setting_snooze_time,
                        alarm.snoozeMinute,
                        alarm.snoozeMinute
                    ),
                    onClick = { showSnoozeDialog = true }
                )
            }

            // 8. バイブレーション
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_vibration),
                    description = null,
                    checked = alarm.shouldVibrate,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            showVibrationWarning = true
                        }
                        onAlarmChange(alarm.copy(shouldVibrate = isEnabled))
                    }
                )
            }
        }
    }

    // ダイアログ表示ロジック

    // 時刻選択
    if (showTimePickerDialog) {
        TimePickerDialog(
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            onConfirm = { hour, minute ->
                onAlarmChange(alarm.copy(hour = hour, minute = minute))
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    // 繰り返しタイプ選択
    if (showRepeatTypeDialog) {
        val currentType = when (alarm.repeatType) {
            is Alarm.RepeatType.Once -> RepeatTypeSelection.ONCE

            is Alarm.RepeatType.Everyday -> RepeatTypeSelection.EVERYDAY

            is Alarm.RepeatType.Days -> RepeatTypeSelection.DAYS

            is Alarm.RepeatType.Date -> RepeatTypeSelection.DATE

            is Alarm.RepeatType.Snooze -> {
                // Snoozeは設定画面では扱わない
                RepeatTypeSelection.ONCE
            }
        }

        RepeatTypeDialog(
            currentRepeatType = currentType,
            onTypeSelected = { type ->
                showRepeatTypeDialog = false
                when (type) {
                    RepeatTypeSelection.ONCE -> {
                        onAlarmChange(alarm.copy(repeatType = Alarm.RepeatType.Once))
                    }

                    RepeatTypeSelection.EVERYDAY -> {
                        onAlarmChange(alarm.copy(repeatType = Alarm.RepeatType.Everyday))
                    }

                    RepeatTypeSelection.DAYS -> {
                        showDayOfWeekDialog = true
                    }

                    RepeatTypeSelection.DATE -> {
                        showDatePickerDialog = true
                    }
                }
            },
            onDismiss = { showRepeatTypeDialog = false }
        )
    }

    // 曜日選択
    if (showDayOfWeekDialog) {
        val initialDays = (alarm.repeatType as? Alarm.RepeatType.Days)?.days ?: emptyList()
        DayOfWeekPickerDialog(
            initialSelectedDays = initialDays,
            onConfirm = { days ->
                val newRepeatType = when {
                    days.size == DayOfWeekCompat.entries.size -> Alarm.RepeatType.Everyday
                    else -> Alarm.RepeatType.Days(days)
                }
                onAlarmChange(alarm.copy(repeatType = newRepeatType))
                showDayOfWeekDialog = false
            },
            onDismiss = { showDayOfWeekDialog = false }
        )
    }

    // 日付選択
    if (showDatePickerDialog) {
        val initialMillis = (alarm.repeatType as? Alarm.RepeatType.Date)?.targetDate?.time
        val scope = rememberCoroutineScope()
        AlarmDatePickerDialog(
            initialDateMillis = initialMillis,
            onConfirm = { millis ->
                val selectedDate = Date(millis)
                if (selectedDate.before(Date())) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorPastDate)
                    }
                } else {
                    onAlarmChange(alarm.copy(repeatType = Alarm.RepeatType.Date(selectedDate)))
                }
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // スヌーズ分数選択
    if (showSnoozeDialog) {
        SnoozeMinutePickerDialog(
            initialMinute = alarm.snoozeMinute,
            onConfirm = { minute ->
                onAlarmChange(alarm.copy(snoozeMinute = minute))
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false }
        )
    }

    // バイブレーション警告
    if (showVibrationWarning) {
        VibrationWarningDialog(
            onOpenIssue = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://github.com/turtton/YtAlarm/issues/117".toUri()
                }
                context.startActivity(intent)
            },
            onDismiss = { showVibrationWarning = false }
        )
    }
}

/**
 * AlarmSettingsScreen
 *
 * ViewModel連携版のAlarm設定画面。
 * alarmIdを受け取り、新規作成(-1)または既存編集を行う。
 */
@Suppress("ThrowsCount") // CancellationException rethrows for proper coroutine handling
@Composable
fun AlarmSettingsScreen(
    alarmId: Long,
    onNavigateBack: () -> Unit,
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
    val errorFailedToGetAlarm = stringResource(R.string.snackbar_error_failed_to_get_alarm)
    val errorPlaylistIsNull = stringResource(R.string.snackbar_error_playlistid_is_null)
    val errorPastDate = stringResource(R.string.snackbar_error_target_is_the_past_date)
    val errorFailedToSchedule = stringResource(R.string.snackbar_error_failed_to_schedule_alarm)
    val errorFailedToSave = stringResource(R.string.snackbar_error_failed_to_save_alarm)

    // Alarm取得とローカル状態管理
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(alarmId) {
        alarm = if (alarmId == -1L) {
            Alarm(
                hour = 7,
                minute = 0,
                repeatType = Alarm.RepeatType.Once,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            )
        } else {
            withContext(Dispatchers.IO) {
                try {
                    alarmViewModel.getFromIdAsync(alarmId).await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: IllegalStateException) {
                    android.util.Log.e("AlarmSettingsScreen", "Failed to get alarm: $alarmId", e)
                    null
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e("AlarmSettingsScreen", "Invalid alarm id: $alarmId", e)
                    null
                }
            }
        }

        if (alarm == null && alarmId != -1L) {
            // Alarm取得失敗
            snackbarHostState.showSnackbar(errorFailedToGetAlarm)
            onNavigateBack()
        }

        editingAlarm = alarm
    }

    // プレイリスト情報取得
    var playlistTitle by remember { mutableStateOf("") }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editingAlarm?.playListId) {
        editingAlarm?.playListId?.let { ids ->
            if (ids.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val playlists = playlistViewModel.getFromIdsAsync(ids).await()
                        playlistTitle = playlists.joinToString(", ") { it.title }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: IllegalStateException) {
                        android.util.Log.e("AlarmSettingsScreen", "Failed to get playlists", e)
                        playlistTitle = ""
                    }
                }
            } else {
                playlistTitle = ""
            }
        }
    }

    // 保存処理
    fun saveAlarm() {
        val currentAlarm = editingAlarm ?: return

        // バリデーション: プレイリスト選択必須
        if (currentAlarm.playListId.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(errorPlaylistIsNull)
            }
            return
        }

        // バリデーション: 過去日付チェック（Date型の場合）
        if (currentAlarm.repeatType is Alarm.RepeatType.Date) {
            val targetDate = currentAlarm.repeatType.targetDate
            if (targetDate.before(Date())) {
                scope.launch {
                    snackbarHostState.showSnackbar(errorPastDate)
                }
                return
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                // insertSync/updateSyncで完了を待ってからスケジュール更新
                if (currentAlarm.id == 0L) {
                    alarmViewModel.insert(currentAlarm)
                } else {
                    alarmViewModel.update(currentAlarm)
                }

                // AlarmManagerにアラームを登録
                val allAlarms = alarmViewModel.getAllAlarmsAsync().await()
                    .filter { it.isEnable }
                val scheduleResult = updateAlarmSchedule(context, allAlarms)

                when (scheduleResult) {
                    is arrow.core.Either.Left -> {
                        val error = scheduleResult.value
                        // NoEnabledAlarmはエラーではなく正常なケース（無効アラームの保存時など）
                        if (error != AlarmScheduleError.NoEnabledAlarm) {
                            android.util.Log.e(
                                "AlarmSettingsScreen",
                                "Failed to schedule alarm: $error"
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if (error != AlarmScheduleError.NoEnabledAlarm) {
                                snackbarHostState.showSnackbar(errorFailedToSchedule)
                            }
                            onNavigateBack()
                        }
                    }

                    is arrow.core.Either.Right -> {
                        withContext(Dispatchers.Main) {
                            onNavigateBack()
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: android.database.sqlite.SQLiteException) {
                android.util.Log.e("AlarmSettingsScreen", "Database error saving alarm", e)
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(errorFailedToSave)
                }
            } catch (e: IllegalStateException) {
                android.util.Log.e("AlarmSettingsScreen", "Failed to save alarm", e)
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(errorFailedToSave)
                }
            }
        }
    }

    // プレイリストリストの取得（showPlaylistDialogの状態に依存）
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    LaunchedEffect(showPlaylistDialog) {
        if (showPlaylistDialog) {
            withContext(Dispatchers.IO) {
                try {
                    allPlaylists = playlistViewModel.allPlaylistsAsync.await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: IllegalStateException) {
                    android.util.Log.e("AlarmSettingsScreen", "Failed to get all playlists", e)
                    allPlaylists = emptyList()
                }
            }
        }
    }

    // UI表示
    Box(modifier = modifier) {
        // AlarmSettingsScreenContent呼び出し
        editingAlarm?.let { currentAlarm ->
            AlarmSettingsScreenContent(
                alarm = currentAlarm,
                playlistTitle = playlistTitle,
                snackbarHostState = snackbarHostState,
                onAlarmChange = { updatedAlarm -> editingAlarm = updatedAlarm },
                onSave = ::saveAlarm,
                onNavigateBack = onNavigateBack,
                onPlaylistSelect = { showPlaylistDialog = true }
            )
        }

        // プレイリスト選択ダイアログ
        if (showPlaylistDialog && allPlaylists.isNotEmpty()) {
            // PlaylistをDisplayDataに変換（非同期でサムネイル取得）
            var displayDataList by remember {
                mutableStateOf<List<net.turtton.ytalarm.ui.compose.dialogs.DisplayData<Long>>>(
                    emptyList()
                )
            }

            LaunchedEffect(allPlaylists) {
                displayDataList = withContext(Dispatchers.IO) {
                    allPlaylists.map { playlist ->
                        val thumbnailUrl = when (val thumbnail = playlist.thumbnail) {
                            is Playlist.Thumbnail.Video -> {
                                // Video thumbnailの場合、VideoからURLを取得
                                try {
                                    val video = videoViewModel.getFromIdAsync(thumbnail.id).await()
                                    video?.thumbnailUrl?.let {
                                        DisplayDataThumbnail.Url(
                                            it
                                        )
                                    } ?: DisplayDataThumbnail.Drawable(
                                        R.drawable.ic_no_image
                                    )
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: IllegalStateException) {
                                    android.util.Log.w(
                                        "AlarmSettingsScreen",
                                        "Failed to get video thumbnail: ${thumbnail.id}",
                                        e
                                    )
                                    DisplayDataThumbnail.Drawable(
                                        R.drawable.ic_no_image
                                    )
                                }
                            }

                            is Playlist.Thumbnail.Drawable -> {
                                DisplayDataThumbnail.Drawable(
                                    thumbnail.id
                                )
                            }
                        }
                        net.turtton.ytalarm.ui.compose.dialogs.DisplayData(
                            id = playlist.id,
                            title = playlist.title,
                            thumbnailUrl = thumbnailUrl
                        )
                    }
                }
            }

            if (displayDataList.isNotEmpty()) {
                net.turtton.ytalarm.ui.compose.dialogs.MultiChoiceVideoDialog(
                    displayDataList = displayDataList,
                    initialSelectedIds = editingAlarm?.playListId?.toSet() ?: emptySet(),
                    onConfirm = { selectedIds ->
                        editingAlarm = editingAlarm?.copy(playListId = selectedIds.toList())
                        showPlaylistDialog = false
                    },
                    onDismiss = {
                        showPlaylistDialog = false
                    }
                )
            }
        }
    }
}

// プレビュー

@Preview(showBackground = true)
@Composable
fun AlarmSettingsScreenContentPreview() {
    AppTheme {
        AlarmSettingsScreenContent(
            alarm = Alarm(
                id = 1L,
                hour = 7,
                minute = 30,
                repeatType = Alarm.RepeatType.Days(
                    listOf(
                        DayOfWeekCompat.MONDAY,
                        DayOfWeekCompat.WEDNESDAY,
                        DayOfWeekCompat.FRIDAY
                    )
                ),
                playListId = listOf(1L),
                shouldLoop = true,
                shouldShuffle = false,
                volume = Alarm.Volume(75),
                snoozeMinute = 15,
                shouldVibrate = true,
                isEnable = false,
                creationDate = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance()
            ),
            playlistTitle = "Morning Playlist",
            snackbarHostState = SnackbarHostState(),
            onAlarmChange = {},
            onSave = {},
            onNavigateBack = {},
            onPlaylistSelect = {}
        )
    }
}