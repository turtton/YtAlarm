package net.turtton.ytalarm

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    YoutubeDL.getInstance().init(applicationContext)
                }
            }.onFailure {
                launch(Dispatchers.Main) {
                    Snackbar.make(binding.root.rootView, "Internal error occurred.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                    Log.e(APP_TAG, "YtDL initialization failed", it)
                }
            }
        }

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)!!.findNavController()
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.aram_list_fragment, R.id.playlist_fragment, R.id.all_video_list_fragment), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.shrink()
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