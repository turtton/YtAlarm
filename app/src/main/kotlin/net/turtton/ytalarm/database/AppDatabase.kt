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
import net.turtton.ytalarm.util.converter.LongListConverter
import net.turtton.ytalarm.util.converter.PlaylistTypeConverter
import net.turtton.ytalarm.util.converter.RepeatTypeConverter
import net.turtton.ytalarm.util.converter.StringListConverter
import net.turtton.ytalarm.util.converter.VideoStateConverter

@Database(entities = [Alarm::class, Video::class, Playlist::class], version = 1)
@TypeConverters(
    StringListConverter::class,
    LongListConverter::class,
    RepeatTypeConverter::class,
    VideoStateConverter::class,
    PlaylistTypeConverter::class
)
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
                    val videoId = populateVideoDatabase(it.videoDao())
                    val playlistId = populatePlaylistDatabase(it.playlistDao(), videoId)
                    populateAlarmDatabase(it.alarmDao(), playlistId)
                }
            }
        }

        suspend fun populateAlarmDatabase(dao: AlarmDao, playlistId: Long) {
            dao.deleteAll()
            dao.insert(Alarm(playListId = listOf(playlistId)))
        }

        suspend fun populatePlaylistDatabase(dao: PlaylistDao, videoId: Long): Long = dao.insert(
            Playlist(
                id = 0,
                title = "ExamplePlaylist",
                thumbnailUrl = "https://i.ytimg.com/vi_webp/aLexJOGZ_gw/maxresdefault.webp",
                videos = listOf(videoId)
            )
        )

        suspend fun populateVideoDatabase(dao: VideoDao): Long = dao.insert(
            Video(
                0,
                "aLexJOGZ_gw",
                "クッキー☆ボムラッシュ.SSBU",
                "https://i.ytimg.com/vi_webp/aLexJOGZ_gw/maxresdefault.webp",
                "https://www.youtube.com/watch?v=aLexJOGZ_gw",
                "youtube.com",
                Video.State.Information
            )
        )
    }
}