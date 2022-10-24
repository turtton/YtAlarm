package net.turtton.ytalarm.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.databinding.ActivityMainBinding
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.addNewPlaylist
import net.turtton.ytalarm.ui.adapter.MultiChoiceVideoListAdapter.DisplayData.Companion.toDisplayData
import net.turtton.ytalarm.ui.dialog.DialogMultiChoiceVideo
import net.turtton.ytalarm.util.initYtDL
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.SNOOZE_NOTIFICATION
import net.turtton.ytalarm.worker.VIDEO_DOWNLOAD_NOTIFICATION
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding
    lateinit var drawerLayout: DrawerLayout

    val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(application.repository)
    }

    val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(application.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        initYtDL(binding.root.rootView)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val mainNav = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        navController = mainNav!!.findNavController()
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)
        drawerLayout = findViewById(R.id.drawer_layout)
        val topLevelFragments = setOf(
            R.id.aram_list_fragment,
            R.id.playlist_fragment,
            R.id.all_video_list_fragment,
            R.id.aboutpage_fragment
        )
        appBarConfiguration = AppBarConfiguration(
            topLevelFragments,
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.shrink()

        createNotificationChannel()
        requestPermission()
        checkUrlShare(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkUrlShare(intent)
    }

    override fun onRestart() {
        super.onRestart()
        requestPermission()
    }

    private fun requestPermission() {
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
                            Snackbar.make(
                                binding.root.rootView,
                                R.string.snackbar_failed_to_check_restriction,
                                Snackbar.LENGTH_LONG
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
                        AlertDialog.Builder(applicationContext)
                            .setTitle(R.string.dialog_shared_text_should_be_url_title)
                            .setMessage(R.string.dialog_shared_text_should_be_url_description)
                            .setPositiveButton(R.string.dialog_shared_text_should_be_url_ok, null)
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val APP_TAG = "YtAram"
    }
}