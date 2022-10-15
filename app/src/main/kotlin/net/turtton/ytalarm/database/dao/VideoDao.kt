package net.turtton.ytalarm.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.structure.Video

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getAll(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getFromIdSync(id: Long): Video?

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    suspend fun getFromIdsSync(ids: List<Long>): List<Video>

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    fun getFromIds(ids: List<Long>): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id NOT IN (:ids)")
    suspend fun getExceptIdsSync(ids: List<Long>): List<Video>

    @Query("SELECT * FROM videos WHERE video_id IN (:ids)")
    fun getFromVideoIds(ids: List<String>): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE video_id = :id")
    suspend fun getFromVideoIdSync(id: String): Video?

    @Query("SELECT * FROM videos WHERE video_id IN (:ids)")
    suspend fun getFromVideoIdsSync(ids: List<String>): List<Video>

    @Query("SELECT * FROM videos WHERE video_id NOT IN (:ids)")
    suspend fun getExceptVideoIdsSync(ids: List<String>): List<Video>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: Video): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(videos: List<Video>)

    @Delete
    suspend fun delete(videos: List<Video>)
}