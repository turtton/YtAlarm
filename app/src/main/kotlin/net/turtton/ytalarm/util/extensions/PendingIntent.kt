package net.turtton.ytalarm.util.extensions

import android.app.PendingIntent
import android.os.Build

val compatPendingIntentFlag: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }