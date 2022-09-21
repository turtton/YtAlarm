package net.turtton.ytalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
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

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        initYtDL(binding.root.rootView)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val mainNav = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        navController = mainNav!!.findNavController()
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.aram_list_fragment, R.id.playlist_fragment, R.id.all_video_list_fragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.shrink()

        createNotificationChannel()
        requestPermission()
    }

    private fun requestPermission() {
        val hasPerm = { Settings.canDrawOverlays(applicationContext) }
        if (!hasPerm()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            val activity = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (!hasPerm()) {
                    requestPermission()
                }
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_require_overlay_perm)
                .setPositiveButton(R.string.dialog_require_overlay_perm_ok) { _, _ ->
                    activity.launch(intent)
                    finish()
                }.show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_snooze_channel_name)
            val description = getString(R.string.notification_snooze_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(SNOOZE_NOTIFICATION, name, importance)
            channel.description = description
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val APP_TAG = "YtAram"
    }
}