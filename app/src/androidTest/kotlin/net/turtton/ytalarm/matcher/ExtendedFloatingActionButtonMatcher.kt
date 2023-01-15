package net.turtton.ytalarm.matcher

import android.view.View
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import org.hamcrest.Description
import org.hamcrest.Matcher

class ExtendedFloatingActionButtonMatcher(
    private val extendStateMatcher: Matcher<Boolean>
) : BoundedDiagnosingMatcher<View, ExtendedFloatingActionButton>(
    ExtendedFloatingActionButton::class.java
) {
    override fun describeMoreTo(description: Description?) {
        description?.appendText("efab.isExtended() matching: ")
            ?.appendDescriptionOf(extendStateMatcher)
    }

    override fun matchesSafely(
        item: ExtendedFloatingActionButton,
        mismatchDescription: Description?
    ): Boolean {
        val isExtended = item.isExtended
        mismatchDescription?.appendText("efab.isExtended() was ")?.appendValue(isExtended)
        return extendStateMatcher.matches(isExtended)
    }
}