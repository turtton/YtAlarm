package net.turtton.ytalarm.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceController
import net.turtton.ytalarm.ui.LocalVideoPlayerResourceContainer
import net.turtton.ytalarm.ui.compose.screens.VideoPlayerScreen
import net.turtton.ytalarm.ui.compose.theme.AppTheme

class AlarmActivity :
    AppCompatActivity(),
    VideoPlayerLoadingResourceContainer {

    override val videoPlayerLoadingResourceController = VideoPlayerLoadingResourceController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // YoutubeDLの初期化
        initYtDL()

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId <= 0L) {
            Log.e(LOG_TAG, "Invalid alarm id: $alarmId, using fallback alarm")
        }

        // Composeで画面を設定
        setContent {
            CompositionLocalProvider(
                LocalVideoPlayerResourceContainer provides this
            ) {
                AppTheme {
                    VideoPlayerScreen(
                        videoId = alarmId.toString(),
                        isAlarmMode = true,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    /**
     * YoutubeDLライブラリの初期化
     */
    private fun initYtDL() = lifecycleScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(applicationContext)
            }
        }.onFailure {
            Toast.makeText(
                this@AlarmActivity,
                "Internal error occurred.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(LOG_TAG, "YtDL initialization failed", it)
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "ALARM_ID"
        private const val LOG_TAG = "AlarmActivity"
    }
}