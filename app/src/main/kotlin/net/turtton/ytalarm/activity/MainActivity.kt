package net.turtton.ytalarm.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceController
import net.turtton.ytalarm.ui.MainScreen
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.ytDlpUpdateChannel
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.SNOOZE_NOTIFICATION
import net.turtton.ytalarm.worker.VIDEO_DOWNLOAD_NOTIFICATION
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

class MainActivity :
    AppCompatActivity(),
    VideoPlayerLoadingResourceContainer {

    override val videoPlayerLoadingResourceController = VideoPlayerLoadingResourceController()

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(application.repository)
    }

    val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(application.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Composeで画面を設定
        setContent {
            MainScreen(
                playlistViewModel = playlistViewModel,
                videoViewModel = videoViewModel,
                videoPlayerResourceContainer = this
            )
        }

        // 既存の初期化処理
        initYtDL()
        createNotificationChannel()
        checkUrlShare(intent)
    }

    /**
     * YoutubeDLライブラリの初期化
     *
     * YT-DL初期化をバックグラウンドで実行し、エラー時にToastを表示する。
     * 初期化成功後にyt-dlpの更新を試みる。
     */
    private fun initYtDL() = lifecycleScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(applicationContext)
            }
        }.onSuccess {
            // 初期化成功後に更新を実行
            updateYtDL()
        }.onFailure {
            // lifecycleScope内なので追加のlaunchは不要
            Toast.makeText(
                this@MainActivity,
                "Internal error occurred.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(APP_TAG, "YtDL initialization failed", it)
        }
    }

    /**
     * yt-dlpの更新
     *
     * SharedPreferencesから更新チャンネルを取得し、バックグラウンドで更新を実行する。
     * 更新失敗時のみ通知を表示する。
     */
    private fun updateYtDL() = lifecycleScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val channelName = applicationContext.appSettings.ytDlpUpdateChannel
                val channel = when (channelName) {
                    "NIGHTLY" -> UpdateChannel.NIGHTLY
                    else -> UpdateChannel.STABLE
                }
                YoutubeDL.getInstance().updateYoutubeDL(applicationContext, channel)
            }
        }.onSuccess { status ->
            Log.i(APP_TAG, "YtDL update status: $status")
        }.onFailure { error ->
            Log.w(APP_TAG, "YtDL update failed", error)
            showYtDlpUpdateFailedNotification()
        }
    }

    /**
     * yt-dlp更新失敗通知を表示
     */
    private fun showYtDlpUpdateFailedNotification() {
        // Android 13以上では通知権限が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, YTDLP_UPDATE_NOTIFICATION)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle(getString(R.string.notification_ytdlp_update_failed_title))
            .setContentText(getString(R.string.notification_ytdlp_update_failed_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(YTDLP_UPDATE_NOTIFICATION_ID, notification)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkUrlShare(intent)
    }

    private fun checkUrlShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                lifecycleScope.launch {
                    if (!url.startsWith("http")) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.dialog_shared_text_should_be_url_title)
                            .setMessage(R.string.dialog_shared_text_should_be_url_description)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    }

                    val playlists = playlistViewModel.allPlaylistsAsync.await()
                        .filter { it.type is Playlist.Type.Original }
                        .map { it.toDisplayData(videoViewModel) }
                        .toMutableList()
                        .addNewPlaylist()

                    DialogMultiChoiceVideo(playlists) { _, id ->
                        lifecycleScope.launch {
                            VideoInfoDownloadWorker.registerWorker(
                                applicationContext,
                                url,
                                id.toLongArray()
                            )
                        }
                    }.show(supportFragmentManager, "SelectTargetPlaylist")
                }
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
        const val YTDLP_UPDATE_NOTIFICATION = "net.turtton.ytalarm.YtDlpUpdateNotification"
        private const val YTDLP_UPDATE_NOTIFICATION_ID = 1001
    }
}