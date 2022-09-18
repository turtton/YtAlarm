package net.turtton.ytalarm.database

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

    private class AppDatabaseCallback(private val scope: CoroutineScope) : Callback() {
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
            dao.insert(Alarm(playListId = 0))
        }

        suspend fun populatePlaylistDatabase(dao: PlaylistDao) {
            dao.insert(
                Playlist(
                    id = 0,
                    title = "ExamplePlaylist",
                    thumbnailUrl = "https://i.ytimg.com/vi_webp/aLexJOGZ_gw/maxresdefault.webp",
                    videos = listOf("aLexJOGZ_gw")
                )
            )
        }

        suspend fun populateVideoDatabase(dao: VideoDao) {
            dao.insert(
                Video(
                    "aLexJOGZ_gw",
                    "クッキー☆ボムラッシュ.SSBU",
                    "https://i.ytimg.com/vi_webp/aLexJOGZ_gw/maxresdefault.webp",
                    "https://www.youtube.com/watch?v=aLexJOGZ_gw",
                    "https://...",
                    "youtube.com",
                    26955482
                )
            )
        }
    }
}