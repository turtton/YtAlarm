package net.turtton.ytalarm.datasource.remote

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.mapper.VideoInfo
import java.io.File

/** All production yt-dlp entry points share this gate, even across Executor instances. */
class YtDlpExecutor(private val isAlarmRinging: () -> Boolean = { false }) {
    suspend fun getInfo(request: YoutubeDLRequest): VideoInfo = gate.execute(
        shortWait = isAlarmRinging()
    ) {
        checkReady()
        YoutubeDL.getInstance().getInfo(request)
    }

    suspend fun execute(
        request: YoutubeDLRequest,
        callback: ((Float, Long, String) -> Unit)? = null
    ): YoutubeDLResponse = gate.execute {
        checkReady()
        YoutubeDL.getInstance().execute(request, callback = callback)
    }

    companion object {
        private val gate = YtDlpGate()

        @Volatile private var installationMarker: File? = null

        fun initialize(context: Context) = gate.initialize {
            if (installationMarker == null) {
                val candidate = YtDlpInstallation(context.applicationContext)
                candidate.initialize()
                installationMarker = candidate.pending
            }
        }

        /** null means busy or deferred; shouldDefer is rechecked after acquiring the gate. */
        fun update(
            context: Context,
            channel: YoutubeDL.UpdateChannel,
            shouldDefer: () -> Boolean
        ): YoutubeDL.UpdateStatus? = gate.tryUpdate {
            check(installationMarker != null) { "yt-dlp is not initialized" }
            val current = YtDlpInstallation(context.applicationContext)
            if (shouldDefer()) return@tryUpdate null
            // A failed rollback is fail-closed for readers, but may be retried here.
            if (!current.isUsable()) current.transaction.recover()
            current.transaction.update {
                YoutubeDL.getInstance().updateYoutubeDL(context, channel)
            }
        }

        private fun checkReady() {
            check(installationMarker?.exists() == false) { "yt-dlp installation is not ready" }
        }
    }
}