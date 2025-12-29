package net.turtton.ytalarm.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.BuildConfig
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceController
import net.turtton.ytalarm.ui.MainScreen
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.dialog.DialogMultiChoiceVideo
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
        requestPermission()
        checkUrlShare(intent)
    }

    /**
     * YoutubeDLライブラリの初期化
     *
     * YT-DL初期化をバックグラウンドで実行し、エラー時にToastを表示する。
     */
    private fun initYtDL() = lifecycleScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(applicationContext)
            }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkUrlShare(intent)
    }

    override fun onRestart() {
        super.onRestart()
        requestPermission()
    }

    private fun requestPermission() {
        if (BuildConfig.DEBUG) return
        val hasDrawPerm = { Settings.canDrawOverlays(this) }
        val activity = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}
        if (!hasDrawPerm()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_require_permission)
                .setMessage(R.string.dialog_require_overlay_perm)
                .setPositiveButton(R.string.dialog_require_overlay_perm_ok) { _, _ ->
                    activity.launch(intent)
                }.show()
        } else {
            PackageManagerCompat.getUnusedAppRestrictionsStatus(this).let {
                it.addListener({
                    when (it.get()) {
                        UnusedAppRestrictionsConstants.API_30_BACKPORT,
                        UnusedAppRestrictionsConstants.API_30,
                        UnusedAppRestrictionsConstants.API_31 -> {
                            val intent = IntentCompat
                                .createManageUnusedAppRestrictionsIntent(this, packageName)
                            AlertDialog.Builder(this)
                                .setTitle(R.string.dialog_require_configuration)
                                .setMessage(R.string.dialog_require_disable_restrictions)
                                .setPositiveButton(
                                    R.string.dialog_require_disable_restrictions_ok
                                ) { _, _ ->
                                    activity.launch(intent)
                                }.show()
                        }

                        UnusedAppRestrictionsConstants.ERROR -> {
                            Toast.makeText(
                                this,
                                R.string.snackbar_failed_to_check_restriction,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }, ContextCompat.getMainExecutor(this))
            }
        }
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
        }
    }

    companion object {
        const val APP_TAG = "YtAram"
    }
}