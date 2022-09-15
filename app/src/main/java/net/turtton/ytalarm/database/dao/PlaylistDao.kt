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

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getFromId(id: Int): Flow<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getFromIdSync(id: Int): Playlist

    @Update
    suspend fun update(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)
}