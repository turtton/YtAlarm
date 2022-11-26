package net.turtton.ytalarm

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.idling.VideoPlayerLoadingResource
import net.turtton.ytalarm.ui.adapter.AlarmListAdapter
import net.turtton.ytalarm.ui.adapter.PlaylistAdapter
import net.turtton.ytalarm.ui.adapter.VideoListAdapter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.cleanstatusbar.MobileDataType
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
@RunWith(AndroidJUnit4::class)
class TakeMainActivityScreenshots {
    private var idlingResource: VideoPlayerLoadingResource? = null

    @get:Rule
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    @Before
    fun beforeAll() {
        CleanStatusBar()
            .setClock("0000")
            .setMobileNetworkDataType(MobileDataType.FOURG)
            .enable()
        activityRule.scenario.onActivity {
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
            idlingResource = it.videoPlayerLoadingResourceController
                .registerVideoPlayerLoadingResource()
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    @After
    fun afterAll() {
        CleanStatusBar.disable()
        idlingResource?.also {
            IdlingRegistry.getInstance().unregister(it)
        }
    }

    @Test
    fun testTakeScreenshot() {
        // Wait until loading thumbnail
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close())

        Screengrab.screenshot("01-alarms")

        onView(withId(R.id.recycler_list))
            .perform(actionOnItemAtPosition<AlarmListAdapter.ViewHolder>(0, click()))

        Screengrab.screenshot("06-alarmSettings")

        pressBack()
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

        Screengrab.screenshot("07-drawer")

        onView(withId(R.id.nav_graph_playlist)).perform(click())

        Screengrab.screenshot("02-playlist")

        onView(withId(R.id.recycler_list))
            .perform(actionOnItemAtPosition<PlaylistAdapter.ViewHolder>(0, click()))

        Screengrab.screenshot("03-videos-origin")

        pressBack()
        onView(withId(R.id.recycler_list))
            .perform(actionOnItemAtPosition<PlaylistAdapter.ViewHolder>(1, click()))

        Screengrab.screenshot("04-videos-playlist")

        pressBack()
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_graph_video_list)).perform(click())

        onView(withId(R.id.recycler_list)).perform(swipeUp())

        Screengrab.screenshot("05-allvideos")

        onView(withId(R.id.recycler_list))
            .perform(actionOnItemAtPosition<VideoListAdapter.ViewHolder>(0, click()))

        Screengrab.screenshot("08-videoplayer")

        pressBack()
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_graph_aboutpage)).perform(click())

        Screengrab.screenshot("09-aboutpage")
    }
}