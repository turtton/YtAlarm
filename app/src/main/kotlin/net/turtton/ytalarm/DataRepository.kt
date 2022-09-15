package net.turtton.ytalarm

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.database.AppDatabase
import net.turtton.ytalarm.structure.Alarm
import net.turtton.ytalarm.structure.Playlist
import net.turtton.ytalarm.structure.Video

class DataRepository(private val database: AppDatabase) {

    // Alarm
    val allAlarms: Flow<List<Alarm>> = database.alarmDao().getAll()

    fun getAlarmFromId(id: Int): Flow<Alarm> {
        return database.alarmDao().getFromId(id)
    }

    @WorkerThread
    suspend fun update(alarm: Alarm) {
        database.alarmDao().update(alarm)
    }

    @WorkerThread
    suspend fun insert(alarm: Alarm) {
        database.alarmDao().insert(alarm)
    }

    @WorkerThread
    suspend fun delete(alarm: Alarm) {
        database.alarmDao().delete(alarm)
    }

    // Playlist
    val allPlaylists: Flow<List<Playlist>> = database.playlistDao().getAll()

    fun getPlaylistFromId(id: Int): Flow<Playlist> {
        return database.playlistDao().getFromId(id)
    }

    @WorkerThread
    suspend fun getPlaylistFromIdSync(id: Int): Playlist {
        return database.playlistDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun update(playlist: Playlist) {
        database.playlistDao().update(playlist)
    }

    @WorkerThread
    suspend fun insert(playlist: Playlist) {
        database.playlistDao().insert(playlist)
    }

    @WorkerThread
    suspend fun delete(playlist: Playlist) {
        database.playlistDao().delete(playlist)
    }

    // Video
    val allVideos: Flow<List<Video>> = database.videoDao().getAll()

    fun getVideoFromIds(ids: List<String>): Flow<List<Video>> {
        return database.videoDao().getFromIds(ids)
    }

    @WorkerThread
    suspend fun getVideoFromIdSync(id: String): Video? {
        return database.videoDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun getVideoExceptIdsSync(ids: List<String>): List<Video> {
        return database.videoDao().getExceptIds(ids)
    }

    @WorkerThread
    suspend fun insert(video: Video) {
        database.videoDao().insert(video)
    }

    @WorkerThread
    suspend fun delete(video: Video) {
        database.videoDao().delete(video)
    }
}