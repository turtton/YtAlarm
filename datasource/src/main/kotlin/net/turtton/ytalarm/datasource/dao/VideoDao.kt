package net.turtton.ytalarm.datasource.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.datasource.entity.VideoEntity

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getFromIdSync(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    suspend fun getFromIdsSync(ids: List<Long>): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    fun getFromIds(ids: List<Long>): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id NOT IN (:ids)")
    suspend fun getExceptIdsSync(ids: List<Long>): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE video_id IN (:ids)")
    fun getFromVideoIds(ids: List<String>): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE video_id = :id")
    suspend fun getFromVideoIdSync(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE video_id IN (:ids)")
    suspend fun getFromVideoIdsSync(ids: List<String>): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE video_id NOT IN (:ids)")
    suspend fun getExceptVideoIdsSync(ids: List<String>): List<VideoEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(videos: List<VideoEntity>): List<Long>

    @Delete
    suspend fun delete(video: VideoEntity)

    @Delete
    suspend fun delete(videos: List<VideoEntity>)

    @Transaction
    @Query("DELETE FROM videos")
    suspend fun deleteAll()
}