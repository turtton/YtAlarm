package net.turtton.ytalarm.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.structure.Playlist

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllSync(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getFromId(id: Int): Flow<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getFromIdSync(id: Int): Playlist

    @Query("SELECT * FROM playlists WHERE id IN (:ids)")
    suspend fun getFromIdsSync(ids: List<Int>): List<Playlist>

    @RawQuery(observedEntities = [Playlist::class])
    suspend fun getFromVideoIdsSync(query: SupportSQLiteQuery): List<Playlist>

    @Update
    suspend fun update(playlist: Playlist)

    @Update
    suspend fun update(playlists: List<Playlist>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Delete
    suspend fun delete(playlists: List<Playlist>)
}