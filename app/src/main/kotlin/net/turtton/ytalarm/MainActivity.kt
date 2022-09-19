package net.turtton.ytalarm

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
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

        // TODO add description dialog
        requestPermission()
    }

    private fun requestPermission() {
        val hasPerm = { Settings.canDrawOverlays(this) }
        if (!hasPerm()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (!hasPerm()) {
                    requestPermission()
                }
            }.launch(intent)
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