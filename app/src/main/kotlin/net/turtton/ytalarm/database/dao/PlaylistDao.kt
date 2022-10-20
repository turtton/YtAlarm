package net.turtton.ytalarm.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.structure.Playlist

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllSync(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getFromId(id: Long): Flow<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getFromIdSync(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE id IN (:ids)")
    suspend fun getFromIdsSync(ids: List<Long>): List<Playlist>

    @Update
    suspend fun update(playlist: Playlist)

    @Update
    suspend fun update(playlists: List<Playlist>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Delete
    suspend fun delete(playlist: Playlist)

    @Delete
    suspend fun delete(playlists: List<Playlist>)
}