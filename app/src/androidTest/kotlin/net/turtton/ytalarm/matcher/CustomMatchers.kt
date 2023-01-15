package net.turtton.ytalarm.matcher

import android.view.Gravity
import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not

fun isNotDisplayed(): Matcher<View> = not(isDisplayed())

fun isExtended(): Matcher<View> = withExtendedState(equalTo(true))

fun isNotExtended(): Matcher<View> = withExtendedState(equalTo(false))

private fun withExtendedState(extendStateMatcher: Matcher<Boolean>): Matcher<View> =
    ExtendedFloatingActionButtonMatcher(extendStateMatcher)

fun withDrawerLockMode(lockMode: Int, gravity: Int = Gravity.LEFT): Matcher<View> =
    DrawerLockModeMatcher(lockMode, gravity)