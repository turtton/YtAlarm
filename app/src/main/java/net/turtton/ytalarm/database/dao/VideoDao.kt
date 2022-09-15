package net.turtton.ytalarm.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.structure.Video

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getAll(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    fun getFromIds(ids: List<String>): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getFromIdSync(id: String): Video?

    @Query("SELECT * FROM videos WHERE id NOT IN (:ids)")
    suspend fun getExceptIds(ids: List<String>): List<Video>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: Video)

    @Delete
    suspend fun delete(video: Video)
}