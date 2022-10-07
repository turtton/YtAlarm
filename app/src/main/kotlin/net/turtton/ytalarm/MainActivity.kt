package net.turtton.ytalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import net.turtton.ytalarm.databinding.ActivityMainBinding
import net.turtton.ytalarm.util.SNOOZE_NOTIFICATION
import net.turtton.ytalarm.util.initYtDL
import net.turtton.ytalarm.worker.VIDEO_DOWNLOAD_NOTIFICATION

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding
    lateinit var drawerLayout: DrawerLayout

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
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.aram_list_fragment, R.id.playlist_fragment, R.id.all_video_list_fragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.shrink()

        createNotificationChannel()
        requestPermission()
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