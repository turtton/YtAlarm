package net.turtton.ytalarm.util.extensions

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

val Activity.privatePreferences: SharedPreferences
    get() = getPreferences(Context.MODE_PRIVATE)