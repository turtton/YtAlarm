package net.turtton.ytalarm.util.extensions

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.order.AlarmOrder
import net.turtton.ytalarm.util.order.PlaylistOrder
import net.turtton.ytalarm.util.order.VideoOrder

private const val APP_SETTINGS_PREF_NAME = "app_settings"

/**
 * アプリ設定用のSharedPreferencesを取得
 */
val Context.appSettings: SharedPreferences
    get() = getSharedPreferences(APP_SETTINGS_PREF_NAME, Context.MODE_PRIVATE)

fun SharedPreferences?.showMessageIfNull(view: View): SharedPreferences? = also {
    if (it == null) {
        val message = R.string.snackbar_error_failed_to_access_settings_data
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}

private const val ALARM_ORDER = "AlarmOrder"
var SharedPreferences.alarmOrderUp: Boolean
    get() = getBoolean(ALARM_ORDER, true)
    set(value) = edit { putBoolean(ALARM_ORDER, value) }

private const val VIDEO_ORDER = "VideoOrder"
var SharedPreferences.videoOrderUp: Boolean
    get() = getBoolean(VIDEO_ORDER, true)
    set(value) = edit { putBoolean(VIDEO_ORDER, value) }

private const val PLAYLIST_ORDER = "PlaylistOrder"
var SharedPreferences.playlistOrderUp: Boolean
    get() = getBoolean(PLAYLIST_ORDER, true)
    set(value) = edit { putBoolean(PLAYLIST_ORDER, value) }

private const val ALARM_ORDER_RULE = "AlarmOrderRule"
var SharedPreferences.alarmOrderRule: AlarmOrder
    get() = AlarmOrder.valueOf(getString(ALARM_ORDER_RULE, AlarmOrder.TIME.name)!!)
    set(value) = edit { putString(ALARM_ORDER_RULE, value.name) }

private const val VIDEO_ORDER_RULE = "VideoOrderRule"
var SharedPreferences.videoOrderRule: VideoOrder
    get() = VideoOrder.valueOf(getString(VIDEO_ORDER_RULE, VideoOrder.TITLE.name)!!)
    set(value) = edit { putString(VIDEO_ORDER_RULE, value.name) }

private const val PLAYLIST_ORDER_RULE = "PlaylistOrderRule"
var SharedPreferences.playlistOrderRule: PlaylistOrder
    get() = PlaylistOrder.valueOf(getString(PLAYLIST_ORDER_RULE, PlaylistOrder.TITLE.name)!!)
    set(value) = edit { putString(PLAYLIST_ORDER_RULE, value.name) }

private const val YTDLP_UPDATE_CHANNEL = "YtDlpUpdateChannel"
var SharedPreferences.ytDlpUpdateChannel: String
    get() = getString(YTDLP_UPDATE_CHANNEL, "STABLE") ?: "STABLE"
    set(value) = edit { putString(YTDLP_UPDATE_CHANNEL, value) }