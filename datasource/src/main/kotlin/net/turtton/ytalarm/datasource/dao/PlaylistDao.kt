package net.turtton.ytalarm.datasource.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.datasource.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getFromId(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getFromIdSync(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id IN (:ids)")
    suspend fun getFromIdsSync(ids: List<Long>): List<PlaylistEntity>

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Update
    suspend fun update(playlists: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlists: List<PlaylistEntity>)

    @Transaction
    @Query("DELETE FROM playlists")
    suspend fun deleteAll()
}