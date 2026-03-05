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
 * 複数のプレイリストからテストデータを使用:
 * - Mixed Tracks: SoundCloud/YouTube から抽出したデータ
 * - Bandcamp "Hyped_Garage 01" プレイリストから抽出したデータ
 */
object TestDataHelper {

    // Mixed Tracks: SoundCloud/YouTube からの混合プレイリスト（4トラック）
    private val MIXED_TRACKS = listOf(
        TrackInfo(
            videoId = "1789054948",
            title = "dancinwithsomebawdy",
            thumbnailUrl = "https://i1.sndcdn.com/artworks-5qgAgVY4upyMy8uY-NYXH5w-original.jpg",
            videoUrl = "https://soundcloud.com/lookatcurren/dancinwithsomebawdy",
            domain = "soundcloud.com"
        ),
        TrackInfo(
            videoId = "2231400578",
            title = "B4LLooN - ⠀",
            thumbnailUrl = "https://i1.sndcdn.com/artworks-idsxjqvK6cjedLNx-pad9OA-original.png",
            videoUrl = "https://soundcloud.com/noah_b4lloon/c4b08f0d-936b-4ec0-a47a-16e3d3b4c69a",
            domain = "soundcloud.com"
        ),
        TrackInfo(
            videoId = "2200041203",
            title = "XxKOYUKIxX - ☆。.:＊・゜ @koy #jc",
            thumbnailUrl = "https://i1.sndcdn.com/artworks-4j1WxaU8yrZNklBR-yYFOQw-original.png",
            videoUrl = "https://soundcloud.com/koyuki214/koy-jc",
            domain = "soundcloud.com"
        ),
        TrackInfo(
            videoId = "2140727487",
            title = "AИBY - DON'T YOU WAИT ICECREAM???",
            thumbnailUrl = "https://i1.sndcdn.com/artworks-zVhzHLHiKd4IIhyO-5FDqKg-original.png",
            videoUrl = "https://soundcloud.com/anbyflip/dont-you-want-icecream",
            domain = "soundcloud.com"
        ),
        TrackInfo(
            videoId = "3UJZ8CndI8Y",
            title = "ariiol - typing（ft.歌愛ユキ）",
            thumbnailUrl = "https://i.ytimg.com/vi/3UJZ8CndI8Y/sddefault.jpg",
            videoUrl = "https://www.youtube.com/watch?v=3UJZ8CndI8Y",
            domain = "www.youtube.com"
        )
    )

    // Bandcamp Hyped_Garage 01 のトラック情報（全16トラック）
    private val BANDCAMP_TRACKS = listOf(
        TrackInfo(
            videoId = "2314350106",
            title = "𝚂𝚎𝚛𝚒𝚗𝚊 - Because I can relief you!",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/because-i-can-relief-you",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3022656617",
            title = "deadbeak - the light through the display",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/the-light-through-the-display",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "1216220770",
            title = "ralker - I reached wisp planet now",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/i-reached-wisp-planet-now",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3619514216",
            title = "MiaplaK - dreams that defy calculation",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/dreams-that-defy-calculation",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3361629336",
            title = "DJ藤田ことね - i just wanna dance like this!",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/i-just-wanna-dance-like-this",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3936001422",
            title = "Yutori(Natsu) - ⋆˙⟡HÊŁP✧_｡",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/h-p",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "4176697816",
            title = "FTN-NKNK - A dance that never ends.",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/a-dance-that-never-ends",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "2977873267",
            title = "~𝓦𝓸𝓷𝓭𝓮𝓻 𝓐𝓬𝓾𝓽𝓮~ - Ain't nobody that can do it like you",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/aint-nobody-that-can-do-it-like-you",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "2573770918",
            title = "P1PER - NOTHING BEATS A JET2 HOLIDAY",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/nothing-beats-a-jet2-holiday",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "1228386713",
            title = "vivlos - no end to what we wish for",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/no-end-to-what-we-wish-for",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "1696560815",
            title = "XxKOYUKIxX - DJwon'tyou(come)again",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/djwontyou-come-again",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3785876337",
            title = "<* ))><< - you are in my -",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/you-are-in-my",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "2921878164",
            title = "ウおおああ - v^~^v",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/v-v",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "3935226314",
            title = "Chiffon - I'm warning you, don't do that.",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/im-warning-you-dont-do-that",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "2964798734",
            title = "FAILchan - Don't you want NRG？",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/dont-you-want-nrg",
            domain = "memoryscape.bandcamp.com"
        ),
        TrackInfo(
            videoId = "2514688677",
            title = "Temenough - Urban Groove flip",
            thumbnailUrl = "https://f4.bcbits.com/img/a1553711738_5.jpg",
            videoUrl = "https://memoryscape.bandcamp.com/track/urban-groove-flip",
            domain = "memoryscape.bandcamp.com"
        )
    )

    private data class TrackInfo(
        val videoId: String,
        val title: String,
        val thumbnailUrl: String,
        val videoUrl: String,
        val domain: String
    )

    /**
     * トラックリストからVideoデータをDBに挿入するヘルパー
     */
    private suspend fun insertVideosFromTracks(
        videoDao: VideoDao,
        tracks: List<TrackInfo>
    ): List<Long> {
        val videoIds = mutableListOf<Long>()
        tracks.forEach { track ->
            val videoId = videoDao.insert(
                Video(
                    id = 0,
                    videoId = track.videoId,
                    title = track.title,
                    thumbnailUrl = track.thumbnailUrl,
                    videoUrl = track.videoUrl,
                    domain = track.domain,
                    stateData = Video.State.Information()
                )
            )
            videoIds.add(videoId)
        }
        return videoIds
    }

    /**
     * Mixed TracksのVideoデータをDBに挿入
     *
     * @return 挿入されたVideoのIDリスト
     */
    suspend fun insertMixedVideos(videoDao: VideoDao): List<Long> =
        insertVideosFromTracks(videoDao, MIXED_TRACKS)

    /**
     * Bandcamp TracksのVideoデータをDBに挿入
     *
     * @return 挿入されたVideoのIDリスト
     */
    suspend fun insertBandcampVideos(videoDao: VideoDao): List<Long> =
        insertVideosFromTracks(videoDao, BANDCAMP_TRACKS)

    /**
     * Mixed Tracksプレイリストを挿入
     *
     * @param videoIds プレイリストに含めるVideoのIDリスト
     * @return 挿入されたPlaylistのID
     */
    suspend fun insertMixedPlaylist(playlistDao: PlaylistDao, videoIds: List<Long>): Long =
        playlistDao.insert(
            Playlist(
                id = 0,
                title = "Mixed Tracks",
                thumbnail = if (videoIds.isNotEmpty()) {
                    Playlist.Thumbnail.Video(videoIds.first())
                } else {
                    Playlist.Thumbnail.Drawable(net.turtton.ytalarm.R.drawable.ic_no_image)
                },
                videos = videoIds,
                type = Playlist.Type.Original
            )
        )

    /**
     * Bandcampプレイリストを挿入
     *
     * @param videoIds プレイリストに含めるVideoのIDリスト
     * @return 挿入されたPlaylistのID
     */
    suspend fun insertBandcampPlaylist(playlistDao: PlaylistDao, videoIds: List<Long>): Long =
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
                isEnabled = true
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
                isEnabled = false
            )
        )

        return firstAlarmId
    }

    /**
     * 全てのテストデータを一括挿入
     * 既存のデータをクリアしてからテストデータを挿入する
     * プレイリスト順序: Mixed Tracks -> Hyped_Garage 01
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

        // Mixed Tracksを先に挿入
        val mixedVideoIds = insertMixedVideos(videoDao)
        val mixedPlaylistId = insertMixedPlaylist(playlistDao, mixedVideoIds)

        // Bandcamp Tracksを後に挿入
        val bandcampVideoIds = insertBandcampVideos(videoDao)
        val bandcampPlaylistId = insertBandcampPlaylist(playlistDao, bandcampVideoIds)

        return insertTestAlarms(alarmDao, listOf(mixedPlaylistId, bandcampPlaylistId))
    }
}