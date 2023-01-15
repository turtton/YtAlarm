package net.turtton.ytalarm.matcher

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import org.hamcrest.Description

class DrawerLockModeMatcher(
    private val lockMode: Int,
    private val gravity: Int
) : BoundedDiagnosingMatcher<View, DrawerLayout>(DrawerLayout::class.java) {
    override fun describeMoreTo(description: Description?) {
        description?.appendText("drawer.getDrawerLockMode($gravity) should be ")
            ?.appendValue(lockMode)
    }

    override fun matchesSafely(item: DrawerLayout?, mismatchDescription: Description?): Boolean {
        val drawerLockMode = item?.getDrawerLockMode(gravity)
        mismatchDescription?.appendText("drawer.getDrawerLockMode($gravity) was ")
            ?.appendValue(drawerLockMode)
        return drawerLockMode == lockMode
    }
}