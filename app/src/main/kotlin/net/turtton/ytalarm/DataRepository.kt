package net.turtton.ytalarm

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
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

    @WorkerThread
    suspend fun getAllPlaylistsSync(): List<Playlist> = database.playlistDao().getAllSync()

    fun getPlaylistFromId(id: Long): Flow<Playlist> {
        return database.playlistDao().getFromId(id)
    }

    @WorkerThread
    suspend fun getPlaylistFromIdSync(id: Long): Playlist {
        return database.playlistDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun getPlaylistFromIdsSync(ids: List<Long>): List<Playlist> {
        return database.playlistDao().getFromIdsSync(ids)
    }

    @WorkerThread
    suspend fun getPlaylistContainsVideoIds(ids: List<String>): List<Playlist> {
        val selectQuery = "SELECT * FROM playlists"
        val searchQuery = ids.joinToString(prefix = " WHERE ", separator = " OR ") {
            "videos LIKE '%$it%'"
        }
        val query = SimpleSQLiteQuery("$selectQuery$searchQuery")
        return database.playlistDao().getFromVideoIdsSync(query)
    }

    @WorkerThread
    suspend fun update(playlist: Playlist) {
        database.playlistDao().update(playlist)
    }

    @WorkerThread
    suspend fun update(playlists: List<Playlist>) {
        database.playlistDao().update(playlists)
    }

    @WorkerThread
    suspend fun insert(playlist: Playlist): Long {
        return database.playlistDao().insert(playlist)
    }

    @WorkerThread
    suspend fun delete(playlist: Playlist) {
        database.playlistDao().delete(playlist)
    }

    @WorkerThread
    suspend fun deletePlaylists(playlists: List<Playlist>) {
        database.playlistDao().delete(playlists)
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
    suspend fun getVideoFromIdsSync(ids: List<String>): List<Video> {
        return database.videoDao().getFromIdsSync(ids)
    }

    @WorkerThread
    suspend fun getVideoExceptIdsSync(ids: List<String>): List<Video> {
        return database.videoDao().getExceptIdsSync(ids)
    }

    @WorkerThread
    suspend fun insert(video: Video) {
        database.videoDao().insert(video)
    }

    @WorkerThread
    suspend fun deleteVideoLists(videos: List<Video>) {
        database.videoDao().delete(videos)
    }
}