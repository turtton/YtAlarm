package net.turtton.ytalarm.ui.compose.components

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.dialogs.AlarmDatePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.DayOfWeekPickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.DisplayData
import net.turtton.ytalarm.ui.compose.dialogs.DisplayDataThumbnail
import net.turtton.ytalarm.ui.compose.dialogs.MultiChoiceVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeDialog
import net.turtton.ytalarm.ui.compose.dialogs.RepeatTypeSelection
import net.turtton.ytalarm.ui.compose.dialogs.SnoozeMinutePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.TimePickerDialog
import net.turtton.ytalarm.ui.compose.dialogs.VibrationWarningDialog
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModel
import java.util.Date

/**
 * アラーム編集用ボトムシート
 *
 * @param alarm 編集対象のアラーム（新規作成時はid=0L）
 * @param isNewAlarm 新規作成かどうか
 * @param sheetState ボトムシートの状態
 * @param playlistViewModel プレイリストViewModel
 * @param videoViewModel 動画ViewModel
 * @param onAlarmChange アラームが変更されたときのコールバック
 * @param onSaveRequest 保存リクエスト時のコールバック（バリデーション通過後に呼ばれる）
 * @param onDelete 削除ボタンがクリックされたときのコールバック
 * @param onDismiss ボトムシートを閉じるときのコールバック
 * @param saveErrorMessage 保存エラーメッセージ（外部からのエラー表示用）
 * @param onSaveErrorShown エラー表示後のコールバック（エラーメッセージをクリアするため）
 * @param modifier Modifier
 */
@Suppress("ThrowsCount") // CancellationException rethrows for proper coroutine handling
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditBottomSheet(
    alarm: Alarm,
    isNewAlarm: Boolean,
    sheetState: SheetState,
    playlistViewModel: PlaylistViewModel,
    videoViewModel: VideoViewModel,
    onAlarmChange: (Alarm) -> Unit,
    onSaveRequest: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    saveErrorMessage: String? = null,
    onSaveErrorShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 内部でSnackbarHostStateを管理
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-fetch string resources for use in lambdas
    val errorPastDate = stringResource(R.string.snackbar_error_target_is_the_past_date)
    val errorPlaylistIsNull = stringResource(R.string.snackbar_error_playlistid_is_null)

    // ダイアログ表示状態
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showRepeatTypeDialog by remember { mutableStateOf(false) }
    var showDayOfWeekDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showVibrationWarning by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // ダイアログ表示中はボトムシートのドラッグを無効化
    val isDialogShowing by remember {
        derivedStateOf {
            showTimePickerDialog ||
                showRepeatTypeDialog ||
                showDayOfWeekDialog ||
                showDatePickerDialog ||
                showSnoozeDialog ||
                showVibrationWarning ||
                showPlaylistDialog
        }
    }

    // 外部からのエラーメッセージ表示
    LaunchedEffect(saveErrorMessage) {
        if (saveErrorMessage != null) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            onSaveErrorShown()
        }
    }

    // プレイリスト情報取得
    var playlistTitle by remember { mutableStateOf("") }

    LaunchedEffect(alarm.playListId) {
        if (alarm.playListId.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val playlists = playlistViewModel.getFromIdsAsync(alarm.playListId).await()
                    playlistTitle = playlists.joinToString(", ") { it.title }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Log.w("AlarmEditBottomSheet", "Failed to get playlist titles", e)
                    playlistTitle = ""
                }
            }
        } else {
            playlistTitle = ""
        }
    }

    // プレイリストリストの取得
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    LaunchedEffect(showPlaylistDialog) {
        if (showPlaylistDialog) {
            withContext(Dispatchers.IO) {
                try {
                    allPlaylists = playlistViewModel.allPlaylistsAsync.await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Log.w("AlarmEditBottomSheet", "Failed to get all playlists", e)
                    allPlaylists = emptyList()
                }
            }
        }
    }

    // バリデーション付き保存処理
    val onSaveWithValidation: () -> Unit = {
        // バリデーション: プレイリスト選択必須
        if (alarm.playListId.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(errorPlaylistIsNull)
            }
        } else if (alarm.repeatType is Alarm.RepeatType.Date) {
            // バリデーション: 過去日付チェック
            val targetDate = alarm.repeatType.targetDate
            if (targetDate.before(Date())) {
                scope.launch {
                    snackbarHostState.showSnackbar(errorPastDate)
                }
            } else {
                onSaveRequest()
            }
        } else {
            onSaveRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDialogShowing) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AlarmEditBottomSheetContent(
                alarm = alarm,
                isNewAlarm = isNewAlarm,
                playlistTitle = playlistTitle,
                onAlarmChange = onAlarmChange,
                onSave = onSaveWithValidation,
                onDelete = onDelete,
                onTimeClick = { showTimePickerDialog = true },
                onRepeatClick = { showRepeatTypeDialog = true },
                onPlaylistClick = { showPlaylistDialog = true },
                onSnoozeClick = { showSnoozeDialog = true },
                onVibrationChange = { isEnabled ->
                    if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        showVibrationWarning = true
                    }
                    onAlarmChange(alarm.copy(shouldVibrate = isEnabled))
                }
            )

            // SnackbarHostをボトムシート内に配置
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // アクションボタンの上に表示
            )
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
            is Alarm.RepeatType.Snooze -> RepeatTypeSelection.ONCE
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

    // プレイリスト選択ダイアログ
    if (showPlaylistDialog && allPlaylists.isNotEmpty()) {
        var displayDataList by remember {
            mutableStateOf<List<DisplayData<Long>>>(emptyList())
        }

        LaunchedEffect(allPlaylists) {
            displayDataList = withContext(Dispatchers.IO) {
                allPlaylists.map { playlist ->
                    val thumbnailUrl = when (val thumbnail = playlist.thumbnail) {
                        is Playlist.Thumbnail.Video -> {
                            try {
                                val video = videoViewModel.getFromIdAsync(thumbnail.id).await()
                                video?.thumbnailUrl?.let {
                                    DisplayDataThumbnail.Url(it)
                                } ?: DisplayDataThumbnail.Drawable(R.drawable.ic_no_image)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                Log.w(
                                    "AlarmEditBottomSheet",
                                    "Failed to get video thumbnail: ${thumbnail.id}",
                                    e
                                )
                                DisplayDataThumbnail.Drawable(R.drawable.ic_no_image)
                            }
                        }

                        is Playlist.Thumbnail.Drawable -> {
                            DisplayDataThumbnail.Drawable(thumbnail.id)
                        }
                    }
                    DisplayData(
                        id = playlist.id,
                        title = playlist.title,
                        thumbnailUrl = thumbnailUrl
                    )
                }
            }
        }

        if (displayDataList.isNotEmpty()) {
            MultiChoiceVideoDialog(
                displayDataList = displayDataList,
                initialSelectedIds = alarm.playListId.toSet(),
                onConfirm = { selectedIds ->
                    onAlarmChange(alarm.copy(playListId = selectedIds.toList()))
                    showPlaylistDialog = false
                },
                onDismiss = { showPlaylistDialog = false }
            )
        }
    }
}

/**
 * ボトムシート内のコンテンツ
 */
@Composable
private fun AlarmEditBottomSheetContent(
    alarm: Alarm,
    isNewAlarm: Boolean,
    playlistTitle: String,
    onAlarmChange: (Alarm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTimeClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        // 時刻表示
        AlarmTimeDisplay(
            hour = alarm.hour,
            minute = alarm.minute,
            onTimeClick = onTimeClick
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 設定項目
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
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

            // 1. 繰り返し設定
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_repeat),
                    description = alarm.repeatType.getDisplay(context),
                    onClick = onRepeatClick
                )
            }

            // 2. プレイリスト選択
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_playlist),
                    description = playlistTitle.ifEmpty {
                        stringResource(R.string.setting_playlist_empty)
                    },
                    onClick = onPlaylistClick
                )
            }

            // 3. ループ
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_loop),
                    description = null,
                    checked = alarm.shouldLoop,
                    onCheckedChange = { onAlarmChange(alarm.copy(shouldLoop = it)) }
                )
            }

            // 4. シャッフル
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_shuffle),
                    description = null,
                    checked = alarm.shouldShuffle,
                    onCheckedChange = { onAlarmChange(alarm.copy(shouldShuffle = it)) }
                )
            }

            // 5. 音量
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

            // 6. スヌーズ
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.setting_snooze),
                    description = pluralStringResource(
                        R.plurals.setting_snooze_time,
                        alarm.snoozeMinute,
                        alarm.snoozeMinute
                    ),
                    onClick = onSnoozeClick
                )
            }

            // 7. バイブレーション
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.setting_vibration),
                    description = null,
                    checked = alarm.shouldVibrate,
                    onCheckedChange = onVibrationChange
                )
            }
        }

        // 削除・保存ボタン
        AlarmBottomSheetActions(
            isNewAlarm = isNewAlarm,
            onDelete = onDelete,
            onSave = onSave
        )
    }
}