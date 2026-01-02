package net.turtton.ytalarm.util

import net.turtton.ytalarm.database.dao.AlarmDao
import net.turtton.ytalarm.database.dao.PlaylistDao
import net.turtton.ytalarm.database.dao.VideoDao
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import java.util.UUID

/**
 * スクリーンショットテスト用のテストデータヘルパー
 *
 * Bandcamp "Hyped_Garage 01" プレイリストから抽出したデータを使用
 */
object TestDataHelper {

    // Bandcamp Hyped_Garage 01 のトラック情報
    private val BANDCAMP_TRACKS = listOf(
        TrackInfo(
            videoId = "2314350106",
            title = "Serina - Because I can relief you!",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/because-i-can-relief-you"
        ),
        TrackInfo(
            videoId = "3022656617",
            title = "deadbeak - the light through the display",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/the-light-through-the-display"
        ),
        TrackInfo(
            videoId = "1216220770",
            title = "ralker - I reached wisp planet now",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/i-reached-wisp-planet-now"
        ),
        TrackInfo(
            videoId = "3619514216",
            title = "MiaplaK - dreams that defy calculation",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/dreams-that-defy-calculation"
        )
    )

    private data class TrackInfo(
        val videoId: String,
        val title: String,
        val thumbnailUrl: String,
        val videoUrl: String
    )

    /**
     * テスト用のVideoデータをDBに挿入
     *
     * @return 挿入されたVideoのIDリスト
     */
    suspend fun insertTestVideos(videoDao: VideoDao): List<Long> {
        val videoIds = mutableListOf<Long>()
        BANDCAMP_TRACKS.forEach { track ->
            val videoId = videoDao.insert(
                Video(
                    id = 0,
                    videoId = track.videoId,
                    title = track.title,
                    thumbnailUrl = track.thumbnailUrl,
                    videoUrl = track.videoUrl,
                    domain = "memoryscape.bandcamp.com",
                    stateData = Video.State.Information()
                )
            )
            videoIds.add(videoId)
        }
        return videoIds
    }

    /**
     * テスト用のPlaylistデータをDBに挿入
     *
     * @param videoIds プレイリストに含めるVideoのIDリスト
     * @return 挿入されたPlaylistのID
     */
    suspend fun insertTestPlaylist(playlistDao: PlaylistDao, videoIds: List<Long>): Long =
        playlistDao.insert(
            Playlist(
                id = 0,
                title = "Hyped_Garage 01",
                thumbnail = if (videoIds.isNotEmpty()) {
                    Playlist.Thumbnail.Video(videoIds.first())
                } else {
                    Playlist.Thumbnail.Drawable(net.turtton.ytalarm.R.drawable.ic_no_image)
                },
                videos = videoIds,
                type = Playlist.Type.CloudPlaylist(
                    url = "https://memoryscape.bandcamp.com/album/hyped-garage-01",
                    workerId = UUID.randomUUID(),
                    syncRule = Playlist.SyncRule.ALWAYS_ADD
                )
            )
        )

    /**
     * テスト用のAlarmデータをDBに挿入
     *
     * @param playlistIds アラームに設定するPlaylistのIDリスト
     * @return 挿入された最初のアラームのID
     */
    suspend fun insertTestAlarms(alarmDao: AlarmDao, playlistIds: List<Long>): Long {
        // 追加のアラーム1: 7:30 有効
        val firstAlarmId = alarmDao.insert(
            Alarm(
                id = 0,
                hour = 7,
                minute = 30,
                repeatType = Alarm.RepeatType.Days(
                    listOf(
                        DayOfWeekCompat.MONDAY,
                        DayOfWeekCompat.TUESDAY,
                        DayOfWeekCompat.WEDNESDAY,
                        DayOfWeekCompat.THURSDAY,
                        DayOfWeekCompat.FRIDAY
                    )
                ),
                playListId = if (playlistIds.isNotEmpty()) {
                    listOf(
                        playlistIds.first()
                    )
                } else {
                    emptyList()
                },
                isEnable = true
            )
        )

        // 追加のアラーム2: 18:45 無効
        alarmDao.insert(
            Alarm(
                id = 0,
                hour = 18,
                minute = 45,
                repeatType = Alarm.RepeatType.Everyday,
                playListId = if (playlistIds.size > 1) listOf(playlistIds[1]) else playlistIds,
                isEnable = false
            )
        )

        return firstAlarmId
    }

    /**
     * 全てのテストデータを一括挿入
     * 既存のデータをクリアしてからテストデータを挿入する
     *
     * @return 挿入された最初のアラームのID
     */
    suspend fun insertAllTestData(
        videoDao: VideoDao,
        playlistDao: PlaylistDao,
        alarmDao: AlarmDao
    ): Long {
        // 既存データをクリア（順序重要：外部キー制約を考慮）
        alarmDao.deleteAll()
        playlistDao.deleteAll()
        videoDao.deleteAll()

        val videoIds = insertTestVideos(videoDao)
        val playlistId = insertTestPlaylist(playlistDao, videoIds)
        return insertTestAlarms(alarmDao, listOf(playlistId))
    }
}