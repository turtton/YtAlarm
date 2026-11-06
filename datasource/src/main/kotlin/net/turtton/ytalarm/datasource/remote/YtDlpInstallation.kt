package net.turtton.ytalarm.datasource.remote

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Depends on youtubedl-android 0.18.1 internals, checked against tag 0.18.1:
 * YoutubeDL.kt, YoutubeDLUpdater.kt and common/SharedPrefsHelper.kt.
 * Recheck paths, raw resource and BOTH version keys when upgrading that dependency.
 */
internal class YtDlpInstallation(
    private val context: Context,
    private val initializeRuntime: () -> Unit = { YoutubeDL.getInstance().init(context) },
    private val verify: () -> Unit = ::verifyYtDlpVersion
) {
    private val base = File(context.noBackupFilesDir, "youtubedl-android")
    private val binary = File(base, "yt-dlp/yt-dlp")
    val pending = File(context.noBackupFilesDir, "yt-dlp-update-pending")
    val transaction = YtDlpUpdateTransaction(
        markPending = ::markPending,
        clearPending = { check(!pending.exists() || pending.delete()) },
        restoreBundled = ::restoreBundled,
        verify = verify
    )

    fun isUsable(): Boolean = !pending.exists()

    fun initialize() {
        // init() is a no-op after its first call; it cannot repair a partially copied binary.
        val needsRecovery = pending.exists() || !binary.isFile || binary.length() == 0L
        if (needsRecovery) {
            // A failed startup probe must be retried, not accepted as a healthy nonempty file.
            markPending()
            restoreBundled()
        }
        initializeRuntime()
        if (needsRecovery) {
            verify()
            check(!pending.exists() || pending.delete())
        }
    }

    private fun markPending() {
        FileOutputStream(pending).use {
            it.write(1)
            it.fd.sync()
        }
    }

    private fun restoreBundled() {
        check(base.exists() || base.mkdirs())
        val staging = File(base, "bundled-yt-dlp.tmp")
        context.resources.openRawResource(
            com.yausername.youtubedl_android.R.raw.ytdlp
        ).use { input ->
            FileOutputStream(staging).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        check(binary.parentFile!!.exists() || binary.parentFile!!.mkdirs())
        check(staging.renameTo(binary)) { "Cannot restore bundled yt-dlp" }
        // Keep pythonLibVersion intact. init_ytdlp() does not clear these keys itself.
        @Suppress("ApplySharedPref")
        check(
            context.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
                .edit().remove("dlpVersion").remove("dlpVersionName").commit()
        ) { "Cannot clear yt-dlp version metadata" }
    }
}

private const val VERIFY_TIMEOUT_SECONDS = 10L

private fun verifyYtDlpVersion() {
    // A coroutine timeout alone cannot stop this blocking API. Interrupting execute()
    // makes 0.18.1 destroy its child process. Join before releasing the update gate.
    val task = FutureTask {
        val request = YoutubeDLRequest(emptyList<String>()).addOption("--version")
        val response = YoutubeDL.getInstance().execute(request)
        check(response.exitCode == 0 && response.out.trim().isNotEmpty()) {
            "yt-dlp version probe failed"
        }
    }
    val thread = Thread(task, "yt-dlp-version-probe")
    thread.start()
    try {
        task.get(VERIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (error: TimeoutException) {
        throw IllegalStateException("yt-dlp version probe timed out", error)
    } catch (error: ExecutionException) {
        throw IllegalStateException("yt-dlp version probe failed", error)
    } finally {
        task.cancel(true)
        thread.join()
    }
}