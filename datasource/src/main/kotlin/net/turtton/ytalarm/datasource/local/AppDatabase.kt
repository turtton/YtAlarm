package net.turtton.ytalarm.datasource.local

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.turtton.ytalarm.datasource.converter.CalendarConverter
import net.turtton.ytalarm.datasource.converter.LongListConverter
import net.turtton.ytalarm.datasource.converter.PlaylistThumbnailConverter
import net.turtton.ytalarm.datasource.converter.PlaylistTypeConverter
import net.turtton.ytalarm.datasource.converter.RepeatTypeConverter
import net.turtton.ytalarm.datasource.converter.VideoStateConverter
import net.turtton.ytalarm.datasource.dao.AlarmDao
import net.turtton.ytalarm.datasource.dao.PlaylistDao
import net.turtton.ytalarm.datasource.dao.VideoDao
import net.turtton.ytalarm.datasource.entity.AlarmEntity
import net.turtton.ytalarm.datasource.entity.PlaylistEntity
import net.turtton.ytalarm.datasource.entity.VideoEntity
import net.turtton.ytalarm.datasource.local.migration.MigrationV1ToV2

@Database(
    entities = [AlarmEntity::class, VideoEntity::class, PlaylistEntity::class],
    version = 2
)
@TypeConverters(
    LongListConverter::class,
    CalendarConverter::class,
    RepeatTypeConverter::class,
    VideoStateConverter::class,
    PlaylistTypeConverter::class,
    PlaylistThumbnailConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    companion object {
        @VisibleForTesting
        const val DATABASE_NAME = "yt-aram-db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getDataBase(context: Context, scope: CoroutineScope): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MigrationV1ToV2)
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                    .also {
                        instance = it
                    }
            }
    }

    private class AppDatabaseCallback(private val scope: CoroutineScope) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            instance?.let {
                scope.launch {
                    val videoId = populateVideoDatabase(it.videoDao())
                    val playlistId = populatePlaylistDatabase(it.playlistDao(), videoId)
                    populateAlarmDatabase(it.alarmDao(), playlistId)
                }
            }
        }

        suspend fun populateAlarmDatabase(dao: AlarmDao, playlistId: Long) {
            dao.insert(AlarmEntity(playListId = listOf(playlistId)))
        }

        suspend fun populatePlaylistDatabase(dao: PlaylistDao, videoId: Long): Long = dao.insert(
            PlaylistEntity(
                id = 0,
                title = "ExamplePlaylist",
                thumbnail = PlaylistEntity.Thumbnail.Video(videoId),
                videos = listOf(videoId)
            )
        )

        suspend fun populateVideoDatabase(dao: VideoDao): Long {
            val thumbnailUrl =
                "https://i1.sndcdn.com/artworks-5qgAgVY4upyMy8uY-NYXH5w-original.jpg"
            return dao.insert(
                VideoEntity(
                    id = 0,
                    videoId = "1789054948",
                    title = "dancinwithsomebawdy",
                    thumbnailUrl = thumbnailUrl,
                    videoUrl = "https://soundcloud.com/lookatcurren/dancinwithsomebawdy",
                    domain = "soundcloud.com",
                    stateData = VideoEntity.State.Information()
                )
            )
        }
    }
}