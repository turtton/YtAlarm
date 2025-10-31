package net.turtton.ytalarm.ui.compose.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeDialog
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeSelection
import net.turtton.ytalarm.ui.compose.dialogs.SnoozeMinutePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.TimePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.VibrationWarningDialog
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                    description = playlistTitle.ifEmpty { stringResource(R.string.setting_playlist_empty) },
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
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.snackbar_error_target_is_the_past_date)
                        )
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
                    data = Uri.parse("https://github.com/turtton/YtAlarm/issues/117")
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
@Composable
fun AlarmSettingsScreen(
    alarmId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                } catch (e: Exception) {
                    null
                }
            }
        }

        if (alarm == null && alarmId != -1L) {
            // Alarm取得失敗
            snackbarHostState.showSnackbar(
                context.getString(R.string.snackbar_error_failed_to_get_alarm)
            )
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
                    } catch (e: Exception) {
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
                snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_error_playlistid_is_null)
                )
            }
            return
        }

        // バリデーション: 過去日付チェック（Date型の場合）
        if (currentAlarm.repeatType is Alarm.RepeatType.Date) {
            val targetDate = (currentAlarm.repeatType as Alarm.RepeatType.Date).targetDate
            if (targetDate.before(Date())) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.snackbar_error_target_is_the_past_date)
                    )
                }
                return
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                if (currentAlarm.id == 0L) {
                    alarmViewModel.insert(currentAlarm)
                } else {
                    alarmViewModel.update(currentAlarm)
                }
                withContext(Dispatchers.Main) {
                    onNavigateBack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.snackbar_error_failed_to_save_alarm)
                    )
                }
            }
        }
    }

    // プレイリスト選択ダイアログ
    if (showPlaylistDialog) {
        var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    allPlaylists = playlistViewModel.allPlaylistsAsync.await()
                } catch (e: Exception) {
                    allPlaylists = emptyList()
                }
            }
        }

        // TODO: MultiChoiceVideoDialogの統合
        // 現時点ではプレイリスト選択ダイアログは実装を保留
        // 既存のMultiChoiceVideoDialogを使用する必要があるが、
        // DisplayDataの変換ロジックが必要
        showPlaylistDialog = false
    }

    // AlarmSettingsScreenContent呼び出し
    editingAlarm?.let { currentAlarm ->
        AlarmSettingsScreenContent(
            alarm = currentAlarm,
            playlistTitle = playlistTitle,
            snackbarHostState = snackbarHostState,
            onAlarmChange = { updatedAlarm -> editingAlarm = updatedAlarm },
            onSave = ::saveAlarm,
            onNavigateBack = onNavigateBack,
            onPlaylistSelect = { showPlaylistDialog = true },
            modifier = modifier
        )
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
