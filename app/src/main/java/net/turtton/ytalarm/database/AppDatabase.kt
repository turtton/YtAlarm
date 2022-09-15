package net.turtton.ytalarm.database

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.turtton.ytalarm.database.dao.AlarmDao
import net.turtton.ytalarm.database.dao.PlaylistDao
import net.turtton.ytalarm.database.dao.VideoDao
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.converter.StringListConverter

@Database(entities = [Alarm::class, Video::class, Playlist::class], version = 1)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    companion object {
        @VisibleForTesting
        const val DATABASE_NAME = "yt-aram-db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDataBase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }

    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            INSTANCE?.let {
                scope.launch {
                    populateAlarmDatabase(it.alarmDao())
                    populatePlaylistDatabase(it.playlistDao())
                    populateVideoDatabase(it.videoDao())
                }
            }
        }

        suspend fun populateAlarmDatabase(dao: AlarmDao) {
            dao.deleteAll()
            dao.insert(Alarm())
        }

        suspend fun populatePlaylistDatabase(dao: PlaylistDao) {
            dao.insert(
                Playlist(
                    title = "ExamplePlaylist",
                    thumbnailUrl = "https://i.ytimg.com/vi/xn4qndSd3vs/maxresdefault.jpg",
                    videos = listOf("xn4qndSd3vs")
                )
            )
        }

        suspend fun populateVideoDatabase(dao: VideoDao) {
            dao.insert(
                Video(
                    "xn4qndSd3vs",
                    "稲葉曇『シンクタンク』Vo. 裏命",
                    "https://i.ytimg.com/vi/xn4qndSd3vs/maxresdefault.jpg",
                    "https://www.youtube.com/watch?v=xn4qndSd3vs",
                    "https://rr4---sn-3v2upjvh-puhe.googlevideo.com/videoplayback?expire=1663161599&ei=n4AhY-6iKNPv2roPoYmY4AI&ip=2001%3Ace8%3A132%3A184e%3Ae7ce%3A1851%3A9f74%3A2b81&id=o-AIawH6_p2wKTXmX-iSRP7p-h4HCJzw7D0Rk0me48afgq&itag=22&source=youtube&requiressl=yes&mh=Sp&mm=31%2C29&mn=sn-3v2upjvh-puhe%2Csn-3v2upjvh-3pm6&ms=au%2Crdu&mv=m&mvi=4&pl=47&initcwndbps=1458750&spc=yR2vpz7EfqmisxaMFbw6_4rLyTX-HJ4&vprv=1&svpuc=1&mime=video%2Fmp4&cnr=14&ratebypass=yes&dur=222.679&lmt=1661452562636863&mt=1663139584&fvip=7&fexp=24001373%2C24007246&c=ANDROID&rbqsm=fr&txp=5532434&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AOq0QJ8wRQIhAIfXdgYB8CiY1Y89GkgMuGyItCeRoRoTUmk_ZxK_zg_PAiAwdk_HtP2LyZQ3B3MmlV_1Z6M0tJQkoAXccyq8OfQSGQ%3D%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRQIhALBfBqM0Go8g8r6E7ZDUsT2_-Z0uJlYyT2FyBVYOAPM7AiBDcN9D-HZXT49AOaJXASA0jOSSVhoBg-FmS8cMUhU-qQ%3D%3D",
                    "youtube.com",
                    26955482
                )
            )
        }
    }
}