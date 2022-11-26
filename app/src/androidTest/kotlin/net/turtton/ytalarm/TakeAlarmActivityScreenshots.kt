package net.turtton.ytalarm

import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.turtton.ytalarm.activity.AlarmActivity
import net.turtton.ytalarm.idling.VideoPlayerLoadingResource
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
class TakeAlarmActivityScreenshots {
    private var idlingResource: VideoPlayerLoadingResource? = null

    @get:Rule
    var activityRule = ActivityScenarioRule(AlarmActivity::class.java)

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
            it.intent.putExtra(AlarmActivity.EXTRA_ALARM_ID, 1)
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
        // Wait for video player start.
        // I do not know why, but videoPlayerLoadingResourceController does not work in this test.
        Thread.sleep(15000)
        Screengrab.screenshot("00-alarm")
    }
}