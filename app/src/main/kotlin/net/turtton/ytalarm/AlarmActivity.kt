package net.turtton.ytalarm

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import net.turtton.ytalarm.databinding.ActivityAlarmBinding
import net.turtton.ytalarm.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.util.initYtDL

class AlarmActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityAlarmBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        initYtDL(binding.root.rootView)

        setContentView(binding.root)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)

        val alarmNav = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_alarm)
        navController = alarmNav!!.findNavController()
        val args = FragmentVideoPlayerArgs(alarmId.toString(), true)
        navController.setGraph(R.navigation.video_player, args.toBundle())
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    companion object {
        const val EXTRA_ALARM_ID = "ALARM_ID"
    }
}