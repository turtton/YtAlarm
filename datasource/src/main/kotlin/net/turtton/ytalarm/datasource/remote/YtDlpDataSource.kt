package net.turtton.ytalarm.datasource.remote

import net.turtton.ytalarm.kernel.di.DataSource

/**
 * [YtDlpExecutor] を生成する [DataSource] 実装。
 *
 * yt-dlp (youtubedl-android) へのアクセスを提供する Executor を生成する。
 */
class YtDlpDataSource : DataSource<YtDlpExecutor> {
    override fun createExecutor(): YtDlpExecutor = YtDlpExecutor()
}