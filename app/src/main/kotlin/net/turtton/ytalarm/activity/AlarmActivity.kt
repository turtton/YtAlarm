package net.turtton.ytalarm.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.turtton.ytalarm.R
import net.turtton.ytalarm.databinding.ActivityAlarmBinding
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceController
import net.turtton.ytalarm.ui.fragment.FragmentVideoPlayerArgs
import net.turtton.ytalarm.util.initYtDL

class AlarmActivity :
    AppCompatActivity(),
    VideoPlayerLoadingResourceContainer {
    private lateinit var navController: NavController
    lateinit var binding: ActivityAlarmBinding

    override val videoPlayerLoadingResourceController = VideoPlayerLoadingResourceController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        initYtDL(binding.root.rootView)

        setContentView(binding.root)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) {
            Log.e("AlarmActivity", "Failed to get alarm id")
        }
        val args = FragmentVideoPlayerArgs(alarmId.toString(), true)

        val alarmNav = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_alarm)
        navController = alarmNav!!.findNavController()
        navController.setGraph(R.navigation.video_player, args.toBundle())
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    companion object {
        const val EXTRA_ALARM_ID = "ALARM_ID"
    }
}