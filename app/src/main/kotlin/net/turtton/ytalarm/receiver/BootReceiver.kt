package net.turtton.ytalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import net.turtton.ytalarm.worker.BootRescheduleWorker

/**
 * 端末再起動時にアラームを再登録するBroadcastReceiver
 *
 * BOOT_COMPLETEDブロードキャストを受け取り、WorkManager経由でアラームを再スケジュールする。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "Boot completed, scheduling alarm reschedule worker")
        BootRescheduleWorker.registerWorker(context)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}