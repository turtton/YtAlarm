package net.turtton.ytalarm.kernel.repository

import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.kernel.entity.Playlist

interface PlaylistRepository<Executor> {
    fun getAll(executor: Executor): Flow<List<Playlist>>

    fun getFromId(executor: Executor, id: Long): Flow<Playlist?>

    suspend fun getAllSync(executor: Executor): List<Playlist>

    suspend fun getFromIdSync(executor: Executor, id: Long): Playlist?

    suspend fun getFromIdsSync(executor: Executor, ids: List<Long>): List<Playlist>

    suspend fun update(executor: Executor, playlist: Playlist)

    suspend fun updateAll(executor: Executor, playlists: List<Playlist>)

    suspend fun insert(executor: Executor, playlist: Playlist): Long

    suspend fun delete(executor: Executor, playlist: Playlist)

    suspend fun deleteAll(executor: Executor, playlists: List<Playlist>)
}