package net.turtton.ytalarm.util.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

val Activity.privatePreferences: SharedPreferences
    get() = getPreferences(Context.MODE_PRIVATE)

/**
 * Find the closest Activity from a Context.
 * This is useful in Composables where we only have access to Context.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}