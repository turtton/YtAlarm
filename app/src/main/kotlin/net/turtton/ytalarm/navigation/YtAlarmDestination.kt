package net.turtton.ytalarm.navigation

/**
 * YtAlarmアプリの全ナビゲーション先を定義する
 *
 * 文字列ベースのルート定義を使用。
 * 将来的にNavigation Compose 2.8.0+への移行時に型安全版に更新予定。
 */
object YtAlarmDestination {
    const val ALARM_LIST = "alarm_list"
    const val ALARM_SETTINGS = "alarm_settings/{alarmId}"
    const val PLAYLIST = "playlist"
    const val VIDEO_LIST = "video_list/{playlistId}"
    const val ALL_VIDEOS = "all_videos"
    const val VIDEO_PLAYER = "video_player/{videoId}/{isAlarmMode}"
    const val ABOUT = "about"
    const val SETTINGS = "settings"

    /**
     * アラーム設定画面のルートを生成
     */
    fun alarmSettings(alarmId: Long): String = "alarm_settings/$alarmId"

    /**
     * 動画一覧画面のルートを生成
     */
    fun videoList(playlistId: Long): String = "video_list/$playlistId"

    /**
     * 動画プレーヤー画面のルートを生成
     */
    fun videoPlayer(videoId: String, isAlarmMode: Boolean = false): String =
        "video_player/$videoId/$isAlarmMode"
}