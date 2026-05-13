package net.turtton.ytalarm.activity

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.turtton.ytalarm.YtApplication.Companion.ytDlInitJob
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

        // 再生中は画面をオンに保つ
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        observeYtDlInit()

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

    private fun observeYtDlInit() = lifecycleScope.launch {
        application.ytDlInitJob.await().onFailure {
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