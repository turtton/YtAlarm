package net.turtton.ytalarm

import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotClickable
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.matcher.isExtended
import net.turtton.ytalarm.matcher.isNotDisplayed
import net.turtton.ytalarm.matcher.isNotExtended
import net.turtton.ytalarm.matcher.withDrawerLockMode
import net.turtton.ytalarm.ui.adapter.VideoListAdapter
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TestMainActivity {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun fabTest() {
        checkAlarmListFragment()

        // AlarmList -> AlarmSettings
        onFabView().perform(click())

        checkAlarmSettingsFragment()

        // AlarmSettings -> AlarmList
        pressBack()

        checkAlarmListFragment()

        // AlarmList -> Playlist
        onDrawerView().perform(DrawerActions.open()).check(matches(isOpen()))
        onNavView().perform(navigateTo(R.id.nav_graph_playlist))

        checkPlaylistFragment()

        // Playlist -> VideoList
        onFabView().perform(click())

        checkVideoListFragment()

        // VideoList -> Playlist
        pressBack()

        checkPlaylistFragment()

        // Playlist -> AllVideoList
        onDrawerView().perform(DrawerActions.open()).check(matches(isOpen()))
        onNavView().perform(navigateTo(R.id.nav_graph_video_list))

        checkAllVideoListFragment()

        // AllVideoList -> VideoPlayer
        onView(withId(R.id.recycler_list))
            .perform(actionOnItemAtPosition<VideoListAdapter.ViewHolder>(0, click()))

        checkVideoPlayerFragment()

        // VideoPlayer -> AllVideoList
        checkAllVideoListFragment()

        // AllVideoList -> About
        onDrawerView().perform(DrawerActions.open())
        onNavView().perform(navigateTo(R.id.nav_graph_aboutpage))

        checkAboutPageFragment()
    }

    companion object {
        private fun onFabView(): ViewInteraction = onView(withId(R.id.fab))

        private fun onFabAddFromLinkView(): ViewInteraction =
            onView(withId(R.id.fab_add_video_from_link))

        private fun onFabAddFromVideoView(): ViewInteraction =
            onView(withId(R.id.fab_add_video_from_video))

        private fun onFabAddVideoView(): ViewInteraction = onView(withId(R.id.fab_add_video))

        private fun onDrawerView(): ViewInteraction = onView(withId(R.id.drawer_layout))

        private fun onNavView(): ViewInteraction = onView(withId(R.id.nav_view))

        private fun checkAlarmListFragment() {
            onFabView().check(matches(allOf(isDisplayed(), isNotExtended())))
            onFabAddVideoView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)))
        }

        private fun checkAlarmSettingsFragment() {
            onFabView().check(matches(allOf(isDisplayed(), isExtended())))
            onFabAddVideoView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)))
        }

        private fun checkPlaylistFragment() {
            onFabView().check(matches(allOf(isDisplayed(), isNotExtended())))
            onFabAddVideoView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)))
        }

        private fun checkVideoListFragment() {
            onFabView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)))

            onFabAddVideoView().check(matches(isDisplayed()))
                // TODO: suspend while processing concurrently(use idling system
                .check(matches(withTagValue(equalTo(R.drawable.ic_add_video))))
                .perform(click())
                .check(matches(withTagValue(equalTo(R.drawable.ic_add))))
            onFabAddFromLinkView().check(matches(isClickable()))
            onFabAddFromVideoView().check(matches(isClickable()))

            onFabAddVideoView().perform(click())
                .check(matches(withTagValue(equalTo(R.drawable.ic_add_video))))
            onFabAddFromLinkView().check(matches(isNotClickable()))
            onFabAddFromVideoView().check(matches(isNotClickable()))
        }

        private fun checkAllVideoListFragment() {
            onFabView().check(matches(allOf(isDisplayed(), isNotExtended())))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)))
        }

        private fun checkVideoPlayerFragment() {
            onFabView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
            onDrawerView().check(matches(withDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)))
        }

        private fun checkAboutPageFragment() {
            onFabView().check(matches(isNotDisplayed()))
            onFabAddFromLinkView().check(matches(isNotDisplayed()))
            onFabAddFromVideoView().check(matches(isNotDisplayed()))
        }
    }
}