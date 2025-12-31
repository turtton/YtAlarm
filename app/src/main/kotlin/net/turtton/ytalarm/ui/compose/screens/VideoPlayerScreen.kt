package net.turtton.ytalarm.ui.compose.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.DigitalClock
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.BuildConfig
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.hourOfDay
import net.turtton.ytalarm.util.extensions.minute
import net.turtton.ytalarm.util.extensions.plusAssign
import net.turtton.ytalarm.util.updateAlarmSchedule
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.UpdateSnoozeNotifyWorker
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * 動画プレーヤー画面
 *
 * @param videoId 動画ID（非アラームモード）またはアラームID（アラームモード）
 * @param isAlarmMode アラームモードかどうか
 * @param onDismiss 終了時のコールバック
 * @param snackbarHostState Snackbar表示用のホスト状態
 */
@Suppress("LongMethod", "LongParameterList")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    isAlarmMode: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val application = context.applicationContext as net.turtton.ytalarm.YtApplication
    val videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(application.repository)
    )
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(application.repository)
    )
    val alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(application.repository)
    )

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var currentVolume by remember { mutableStateOf<Int?>(null) }
    var vibrator by remember { mutableStateOf<Vibrator?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // ExoPlayerの作成（AudioAttributes設定込み）
    val exoPlayer = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(if (isAlarmMode) C.USAGE_ALARM else C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    // Player.Listenerの状態管理用（State型で競合状態を回避）
    val onPlaybackEndedState = remember { mutableStateOf<(() -> Unit)?>(null) }

    // 統合されたPlayer.Listener
    val playerListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onRenderedFirstFrame() {
                // ビデオトラックがある場合のみ背景をクリア
                val hasVideoTrack = exoPlayer.currentTracks.groups.any { group ->
                    group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                }
                if (hasVideoTrack) {
                    playerView?.background = null
                }
                isLoading = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEndedState.value?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(LOG_TAG, "ExoPlayer error: ${error.message}", error)
                isLoading = false
                hasError = true
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.snackbar_error_failed_to_import_video)
                    )
                }
            }
        }
    }

    // ExoPlayerのライフサイクル管理
    DisposableEffect(exoPlayer, playerListener) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
        }
    }

    // フルスクリーンモードとリソースのクリーンアップ
    DisposableEffect(Unit) {
        enableFullScreenMode(view)
        onDispose {
            // ExoPlayerを解放
            exoPlayer.release()

            // 音量を復元
            currentVolume?.let {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    it,
                    AudioManager.FLAG_PLAY_SOUND
                )
            }

            // フルスクリーンモードを解除
            disableFullScreenMode(view)

            // バイブレーションを停止
            vibrator?.cancel()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                // XMLからインフレートしてTextureViewとtransparent shutterを適用
                @SuppressLint("InflateParams")
                (
                    LayoutInflater.from(ctx)
                        .inflate(R.layout.player_view, null) as PlayerView
                    ).also { pv ->
                    playerView = pv
                    pv.player = exoPlayer
                }
            },
            update = { pv ->
                // ExoPlayerの参照が変わった場合に再バインド
                if (pv.player != exoPlayer) {
                    pv.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // アラームモード時の時刻表示
        if (isAlarmMode) {
            AndroidView(
                factory = { ctx ->
                    @Suppress("DEPRECATION")
                    DigitalClock(ctx).apply {
                        textSize = 96f
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // ローディング表示
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }

        // エラー表示
        if (hasError) {
            Text(
                text = stringResource(R.string.fragment_video_player_text_error),
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 100.dp)
            )
        }

        // ボタン
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // スヌーズボタン（アラームモード時のみ）
                if (isAlarmMode) {
                    Button(
                        onClick = {
                            scope.launch {
                                val alarmId = videoId.toLongOrNull() ?: -1L
                                if (alarmId != -1L) {
                                    val alarm = alarmViewModel.getFromIdAsync(alarmId).await()
                                    if (alarm != null) {
                                        val now = Calendar.getInstance()
                                        now += alarm.snoozeMinute.minutes
                                        val snoozeAlarm = alarm.copy(
                                            id = 0,
                                            hour = now.hourOfDay,
                                            minute = now.minute,
                                            repeatType = Alarm.RepeatType.Snooze
                                        )
                                        alarmViewModel.insert(snoozeAlarm)
                                        UpdateSnoozeNotifyWorker.registerWorker(context)
                                        withContext(Dispatchers.Main) {
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.fragment_video_player_button_snooze))
                    }
                }

                // ディスミス/停止ボタン
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    val buttonText = if (isAlarmMode) {
                        stringResource(R.string.fragment_video_player_button_dismiss)
                    } else {
                        stringResource(R.string.fragment_video_player_button_stop)
                    }
                    Text(buttonText)
                }
            }
        }
    }

    // 動画再生の初期化処理
    LaunchedEffect(videoId, isAlarmMode) {
        // playerViewの初期化を待機
        val currentPlayerView = snapshotFlow { playerView }
            .filterNotNull()
            .first()

        if (isAlarmMode) {
            val alarmId = videoId.toLongOrNull() ?: -1L
            if (alarmId == -1L) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_error_failed_to_get_alarm)
                )
                Log.e(LOG_TAG, "Alarm id is -1")
                hasError = true
                return@LaunchedEffect
            }

            // アラーム一覧を更新
            val alarmList = alarmViewModel.getAllAlarmsAsync().await().filter { it.isEnable }
            updateAlarmSchedule(context, alarmList)

            // アラーム情報を取得
            val alarm = alarmViewModel.getFromIdAsync(alarmId).await()
            if (alarm == null) {
                hasError = true
                Log.e(LOG_TAG, "Failed to get alarm. TargetId: $alarmId")
                return@LaunchedEffect
            }

            // アラームの更新
            updateAlarm(alarm, alarmViewModel)

            // バイブレーション開始
            if (alarm.shouldVibrate) {
                vibrator = startVibration(context)
            }

            // プレイリストから動画リストを取得
            val playlist = playlistViewModel.getFromIdsAsync(alarm.playListId).await()
            val videos = playlist.flatMap { it.videos }
                .distinct()
                .let { videoViewModel.getFromIdsAsync(it).await() }
                .filter { it.stateData is Video.State.Information }

            if (videos.isEmpty()) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_error_empty_video)
                )
                Log.e(LOG_TAG, "Could not start alarm due to empty videos")
                return@LaunchedEffect
            }

            // 音量設定
            currentVolume = setVolume(audioManager, alarm.volume.volume)

            // 動画キューの準備
            var queue = if (alarm.shouldShuffle) {
                videos.shuffled().iterator()
            } else {
                videos.iterator()
            }

            // 再生完了時のコールバックを設定
            onPlaybackEndedState.value = playbackEnded@{
                if (!queue.hasNext()) {
                    if (alarm.shouldLoop) {
                        queue = videos.iterator()
                    } else {
                        onDismiss()
                        return@playbackEnded
                    }
                }
                scope.launch {
                    playVideo(
                        exoPlayer = exoPlayer,
                        playerView = currentPlayerView,
                        video = queue.next(),
                        onLoading = { isLoading = it },
                        onError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        R.string.snackbar_error_failed_to_import_video
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // 最初の動画を再生
            playVideo(
                exoPlayer = exoPlayer,
                playerView = currentPlayerView,
                video = queue.next(),
                onLoading = { isLoading = it },
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.snackbar_error_failed_to_import_video)
                        )
                    }
                }
            )
        } else {
            // 非アラームモード
            val video = videoViewModel.getFromVideoIdAsync(videoId).await()
            if (video == null) {
                Log.e(LOG_TAG, "Failed to get video. VideoId: $videoId")
                hasError = true
                snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_error_failed_to_get_video)
                )
                return@LaunchedEffect
            }
            playVideo(
                exoPlayer = exoPlayer,
                playerView = currentPlayerView,
                video = video,
                onLoading = { isLoading = it },
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.snackbar_error_failed_to_import_video)
                        )
                    }
                }
            )
        }
    }
}

/**
 * フルスクリーンモードを有効化
 */
private fun enableFullScreenMode(view: View) {
    val window = view.context.findActivity()?.window ?: return
    val insetsController = WindowCompat.getInsetsController(window, view)
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
}

/**
 * フルスクリーンモードを解除
 */
private fun disableFullScreenMode(view: View) {
    val window = view.context.findActivity()?.window ?: return
    val insetsController = WindowCompat.getInsetsController(window, view)
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.show(WindowInsetsCompat.Type.systemBars())
}

/**
 * 音量を設定
 */
private fun setVolume(audioManager: AudioManager, alarmVolume: Int): Int {
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val volumeRate = alarmVolume / Alarm.Volume.MAX_VOLUME.toFloat()
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val volume = (maxVolume * volumeRate).roundToInt()
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND)
    return currentVolume
}

/**
 * アラームを更新
 */
private fun updateAlarm(alarm: Alarm, alarmViewModel: AlarmViewModel) {
    var repeatType = alarm.repeatType
    if (repeatType is Alarm.RepeatType.Date) {
        repeatType = Alarm.RepeatType.Once
    }
    when (repeatType) {
        is Alarm.RepeatType.Once -> {
            alarmViewModel.update(alarm.copy(repeatType = repeatType, isEnable = false))
        }

        is Alarm.RepeatType.Everyday, is Alarm.RepeatType.Days -> {
            alarmViewModel.update(alarm)
        }

        is Alarm.RepeatType.Snooze -> {
            alarmViewModel.delete(alarm)
        }

        else -> {}
    }
}

/**
 * バイブレーションを開始
 */
@Suppress("DEPRECATION")
private fun startVibration(context: Context): Vibrator? {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
        manager?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    } ?: run {
        Log.e(LOG_TAG, "Failed to get vibrator service. Null")
        return null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val wave = VibrationEffect.createWaveform(
            VIBRATION_TIMINGS,
            VIBRATION_AMPLITUDES,
            VIBRATION_REPEAT_POS
        )
        vibrator.vibrate(wave)
    } else {
        vibrator.vibrate(VIBRATION_TIMINGS, VIBRATION_REPEAT_POS)
    }

    return vibrator
}

/**
 * 動画を再生
 */
private suspend fun playVideo(
    exoPlayer: ExoPlayer,
    playerView: PlayerView,
    video: Video,
    onLoading: (Boolean) -> Unit,
    onError: () -> Unit
) {
    withContext(Dispatchers.Main) {
        onLoading(true)
    }

    // サムネイルを背景に設定
    val url = video.videoUrl
    if (BuildConfig.DEBUG) {
        Log.d(LOG_TAG, "Loading thumbnail: ${video.thumbnailUrl}")
    }

    // 前のGlideリクエストをキャンセル（viewにバインドしてライフサイクル管理）
    withContext(Dispatchers.Main) {
        Glide.with(playerView).clear(playerView)
    }

    Glide.with(playerView).load(video.thumbnailUrl.toUri())
        .into(object : CustomViewTarget<View, Drawable>(playerView) {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "Thumbnail loaded successfully")
                }
                playerView.background = resource
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                Log.e(LOG_TAG, "Failed to load thumbnail: ${video.thumbnailUrl}")
            }
            override fun onResourceCleared(placeholder: Drawable?) {}
        })

    // 動画情報を取得
    val infoResult = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url)
        request.addOption("-f", "best")
        runCatching {
            YoutubeDL.getInstance().getInfo(request)
        }
    }

    withContext(Dispatchers.Main) {
        infoResult.onSuccess { info ->
            val videoUrl = info.url
            if (videoUrl.isNullOrEmpty()) {
                Log.e(LOG_TAG, "failed to get stream url")
                onError()
            } else {
                onLoading(false)
                val mediaItem = MediaItem.fromUri(videoUrl.toUri())
                // 前の動画をクリアしてから新しい動画を設定
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }.onFailure { error ->
            Log.e(LOG_TAG, "failed to get stream info", error)
            onError()
        }
    }
}

/**
 * ContextからActivityを取得
 */
private fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private val VIBRATION_MILLIS = 1.5.seconds.toLong(DurationUnit.MILLISECONDS)
private const val VIBRATION_STRENGTH = 255
private val VIBRATION_TIMINGS = longArrayOf(VIBRATION_MILLIS, VIBRATION_MILLIS)
private val VIBRATION_AMPLITUDES = intArrayOf(0, VIBRATION_STRENGTH)
private const val VIBRATION_REPEAT_POS = 0
private const val LOG_TAG = "VideoPlayerScreen"

@Preview(showBackground = true)
@Composable
private fun VideoPlayerScreenPreview() {
    AppTheme {
        VideoPlayerScreen(
            videoId = "test",
            isAlarmMode = false,
            onDismiss = {}
        )
    }
}