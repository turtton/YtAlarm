package net.turtton.ytalarm.datasource.remote

import com.yausername.youtubedl_android.YoutubeDL

/**
 * yt-dlp (youtubedl-android) へのアクセスを表す Executor。
 *
 * [YoutubeDL] のシングルトンインスタンスをラップする。
 * Executor パターンにより、テスト時にモックと差し替えが可能。
 */
class YtDlpExecutor {
    val instance: YoutubeDL get() = YoutubeDL.getInstance()
}