package net.turtton.ytalarm

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.database.AppDatabase
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video

class DataRepository(private val database: AppDatabase) {

    // Alarm
    val allAlarms: Flow<List<Alarm>> = database.alarmDao().getAll()

    @WorkerThread
    suspend fun getAllAlarmsSync(): List<Alarm> {
        return database.alarmDao().getAllSync()
    }

    @WorkerThread
    suspend fun getAlarmFromIdSync(id: Long): Alarm {
        return database.alarmDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun getMatchedAlarmSync(repeatType: Alarm.RepeatType): List<Alarm> {
        return database.alarmDao().getMatchedSync(repeatType)
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
    suspend fun getPlaylistFromIdSync(id: Long): Playlist? {
        return database.playlistDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun getPlaylistFromIdsSync(ids: List<Long>): List<Playlist> {
        return database.playlistDao().getFromIdsSync(ids)
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

    @WorkerThread
    suspend fun getVideoFromIdSync(id: Long): Video? {
        return database.videoDao().getFromIdSync(id)
    }

    @WorkerThread
    suspend fun getVideoFromIdsSync(ids: List<Long>): List<Video> {
        return database.videoDao().getFromIdsSync(ids)
    }

    fun getVideoFromIds(ids: List<Long>): Flow<List<Video>> {
        return database.videoDao().getFromIds(ids)
    }

    @WorkerThread
    suspend fun getVideoExceptIdsSync(ids: List<Long>): List<Video> {
        return database.videoDao().getExceptIdsSync(ids)
    }

    fun getVideoFromVideoIds(ids: List<String>): Flow<List<Video>> {
        return database.videoDao().getFromVideoIds(ids)
    }

    @WorkerThread
    suspend fun getVideoFromVideoIdSync(id: String): Video? {
        return database.videoDao().getFromVideoIdSync(id)
    }

    @WorkerThread
    suspend fun getVideoFromVideoIdsSync(ids: List<String>): List<Video> {
        return database.videoDao().getFromVideoIdsSync(ids)
    }

    @WorkerThread
    suspend fun getVideoExceptVideoIdsSync(ids: List<String>): List<Video> {
        return database.videoDao().getExceptVideoIdsSync(ids)
    }

    @WorkerThread
    suspend fun update(video: Video) {
        return database.videoDao().update(video)
    }

    @WorkerThread
    suspend fun insert(video: Video): Long {
        return database.videoDao().insert(video)
    }

    @WorkerThread
    suspend fun insert(videos: List<Video>): List<Long> {
        return database.videoDao().insert(videos)
    }

    @WorkerThread
    suspend fun delete(video: Video) {
        database.videoDao().delete(video)
    }

    @WorkerThread
    suspend fun deleteVideoLists(videos: List<Video>) {
        database.videoDao().delete(videos)
    }
}