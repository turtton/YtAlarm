package net.turtton.ytalarm

import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.ui.fragment.FragmentPlaylistDirections
import net.turtton.ytalarm.ui.fragment.FragmentVideoPlayerArgs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class TestMainActivity {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun basicFabTest() {
        activityRule.scenario.onActivity {
            val navController = it.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main)!!
                .findNavController()

            it.lifecycleScope.launch {
                // alarmListFragment -> alarmSettingsFragment -> alarmListFragment
                launchMain {
                    it.checkAlarmListFab()
                }.join()

                navController.navigate(R.id.action_AlarmListFragment_to_AlarmSettingsFragment)
                launchMain {
                    it.checkAlarmSettingFab()
                }.join()

                navController.navigateUp()
                launchMain {
                    it.checkAlarmListFab()
                }.join()

                // playlistFragment -> videoListFragment -> playlistFragment
                navController.navigate(R.id.nav_graph_playlist)
                launchMain {
                    it.checkPlaylistFab()
                }.join()

                navController.navigate(
                    FragmentPlaylistDirections.actionPlaylistFragmentToVideoListFragment()
                )
                it.checkVideoListFab(this)

                navController.navigateUp()
                launchMain {
                    it.checkPlaylistFab()
                }.join()

                // allVideoListFragment -> videoPlayerFragment -> allPlaylistFragment
                navController.navigate(R.id.nav_graph_video_list)
                launchMain {
                    it.checkAllVideoListFab()
                }.join()

                val args = FragmentVideoPlayerArgs("0").toBundle()
                navController.navigate(R.id.nav_graph_video_player, args)
                launchMain {
                    checkVideoPlayerFab(it)
                }.join()

                navController.navigateUp()
                launchMain {
                    it.checkAllVideoListFab()
                }.join()
            }
        }
    }

    companion object {
        private const val visible = View.VISIBLE
        private const val invisible = View.INVISIBLE
        private const val gone = View.GONE

        private fun MainActivity.checkAlarmListFab() {
            binding.fab.visibility shouldBe visible
            binding.fab.isExtended shouldBe false
            binding.fabAddVideoFromLink.visibility shouldBe gone
            binding.fabAddVideoFromVideo.visibility shouldBe gone
            drawerLayout.getDrawerLockMode(Gravity.LEFT) shouldBe DrawerLayout.LOCK_MODE_UNLOCKED
        }

        private fun MainActivity.checkAlarmSettingFab() {
            binding.fab.visibility shouldBe visible
            binding.fab.isExtended shouldBe true
            binding.fabAddVideoFromLink.visibility shouldBe gone
            binding.fabAddVideoFromVideo.visibility shouldBe gone
            val drawerLockMode = drawerLayout.getDrawerLockMode(Gravity.LEFT)
            drawerLockMode shouldBe DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        }

        private fun MainActivity.checkPlaylistFab() {
            binding.fab.visibility shouldBe visible
            binding.fab.isExtended shouldBe false
            binding.fabAddVideoFromLink.visibility shouldBe gone
            binding.fabAddVideoFromVideo.visibility shouldBe gone
            drawerLayout.getDrawerLockMode(Gravity.LEFT) shouldBe DrawerLayout.LOCK_MODE_UNLOCKED
        }

        /**
         * This method is not good for testing, but I could not find good way to launching multiple coroutine.
         * Please avoid including CoroutineScope field in arguments to make stack trace information easy to read.
         */
        private suspend fun MainActivity.checkVideoListFab(scope: CoroutineScope) {
            scope.launchMain {
                binding.fab.visibility shouldBe gone
                binding.fabAddVideoFromLink.visibility shouldBe invisible
                binding.fabAddVideoFromVideo.visibility shouldBe invisible
                val drawerLockMode = drawerLayout.getDrawerLockMode(Gravity.LEFT)
                drawerLockMode shouldBe DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            }.join()

            scope.launch {
                while (binding.fabAddVideo.visibility == gone) {
                    delay(16.milliseconds)
                }
            }.join()

            scope.launchMain {
                binding.fabAddVideo.tag?.let { tag ->
                    tag shouldBe R.drawable.ic_add_video
                }

                binding.fabAddVideo.performClick()
                binding.fabAddVideo.tag shouldBe R.drawable.ic_add
                binding.fabAddVideoFromLink.isClickable shouldBe true
                binding.fabAddVideoFromVideo.isClickable shouldBe true

                binding.fabAddVideo.performClick()
                binding.fabAddVideo.tag shouldBe R.drawable.ic_add_video
                binding.fabAddVideoFromLink.isClickable shouldBe false
                binding.fabAddVideoFromVideo.isClickable shouldBe false
            }.join()
        }

        private fun MainActivity.checkAllVideoListFab() {
            binding.fab.visibility shouldBe visible
            binding.fab.isExtended shouldBe false
            binding.fabAddVideoFromLink.visibility shouldBe gone
            binding.fabAddVideoFromVideo.visibility shouldBe gone

            val drawerLockMode = drawerLayout.getDrawerLockMode(Gravity.LEFT)
            drawerLockMode shouldBe DrawerLayout.LOCK_MODE_UNLOCKED
        }

        private fun checkVideoPlayerFab(activity: MainActivity) {
            activity.binding.fab.visibility shouldBe gone
            activity.binding.fabAddVideoFromLink.visibility shouldBe gone
            activity.binding.fabAddVideoFromVideo.visibility shouldBe gone

            val drawerLockMode = activity.drawerLayout.getDrawerLockMode(Gravity.LEFT)
            drawerLockMode shouldBe DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        }

        private fun CoroutineScope.launchMain(block: suspend CoroutineScope.() -> Unit) =
            launch(Dispatchers.Main, block = block)
    }
}