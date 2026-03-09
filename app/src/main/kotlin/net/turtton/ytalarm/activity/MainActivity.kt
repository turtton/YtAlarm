package net.turtton.ytalarm.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.dataContainerProvider
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceController
import net.turtton.ytalarm.ui.MainScreen
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.SNOOZE_NOTIFICATION
import net.turtton.ytalarm.worker.VIDEO_DOWNLOAD_NOTIFICATION
import net.turtton.ytalarm.worker.VIDEO_FILE_DOWNLOAD_NOTIFICATION
import net.turtton.ytalarm.worker.YTDLP_UPDATE_NOTIFICATION
import net.turtton.ytalarm.worker.YtDlpUpdateWorker

class MainActivity :
    AppCompatActivity(),
    VideoPlayerLoadingResourceContainer {

    override val videoPlayerLoadingResourceController = VideoPlayerLoadingResourceController()

    private val sharedUrlState = mutableStateOf<String?>(null)

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(application.dataContainerProvider.getUseCaseContainer())
    }

    val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(application.dataContainerProvider.getUseCaseContainer())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Composeで画面を設定
        setContent {
            MainScreen(
                playlistViewModel = playlistViewModel,
                videoViewModel = videoViewModel,
                videoPlayerResourceContainer = this,
                sharedUrl = sharedUrlState.value,
                onSharedUrlConsumed = { sharedUrlState.value = null }
            )
        }

        // 既存の初期化処理
        initYtDL()
        createNotificationChannel()
        extractSharedUrl(intent)
    }

    /**
     * YoutubeDLライブラリの初期化
     *
     * YT-DL初期化をバックグラウンドで実行し、エラー時にToastを表示する。
     * 初期化成功後にWorkerを登録してyt-dlpの更新を試みる。
     */
    private fun initYtDL() = lifecycleScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(applicationContext)
            }
        }.onSuccess {
            YtDlpUpdateWorker.registerWorker(applicationContext)
        }.onFailure {
            Toast.makeText(
                this@MainActivity,
                "Internal error occurred.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(APP_TAG, "YtDL initialization failed", it)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSharedUrl(intent)
    }

    private fun extractSharedUrl(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                sharedUrlState.value = url
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = getString(R.string.notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val snoozeDescription = getString(R.string.notification_snooze_channel_description)
            val snoozeChannel = NotificationChannel(SNOOZE_NOTIFICATION, name, importance)
            snoozeChannel.description = snoozeDescription
            notificationManager.createNotificationChannel(snoozeChannel)

            val videoInfoDescription =
                getString(R.string.notification_download_video_info_channel_description)
            val videoInfoChannel =
                NotificationChannel(VIDEO_DOWNLOAD_NOTIFICATION, name, importance)
            videoInfoChannel.description = videoInfoDescription
            notificationManager.createNotificationChannel(videoInfoChannel)

            val videoFileDescription =
                getString(R.string.notification_download_video_file_channel_description)
            val videoFileChannel = NotificationChannel(
                VIDEO_FILE_DOWNLOAD_NOTIFICATION,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            videoFileChannel.description = videoFileDescription
            notificationManager.createNotificationChannel(videoFileChannel)

            // yt-dlp更新通知チャンネル
            val ytdlpUpdateDescription =
                getString(R.string.notification_ytdlp_update_channel_description)
            val ytdlpUpdateChannel = NotificationChannel(
                YTDLP_UPDATE_NOTIFICATION,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            ytdlpUpdateChannel.description = ytdlpUpdateDescription
            notificationManager.createNotificationChannel(ytdlpUpdateChannel)
        }
    }

    companion object {
        const val APP_TAG = "YtAram"
    }
}