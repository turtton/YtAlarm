package net.turtton.ytalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.turtton.ytalarm.activity.AlarmActivity

/**
 * アラーム発動時に呼ばれるBroadcastReceiver
 *
 * AlarmManagerからのブロードキャストを受け取り、AlarmActivityを起動する。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmActivity.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) {
            return
        }

        launchAlarmActivity(context, alarmId)
    }

    /**
     * アラームActivityを起動
     */
    private fun launchAlarmActivity(context: Context, alarmId: Long) {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
        }

        context.startActivity(alarmIntent)
    }
}