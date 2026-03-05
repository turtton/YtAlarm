package net.turtton.ytalarm.datasource.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.turtton.ytalarm.datasource.dao.PlaylistDao
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.repository.PlaylistRepository

/**
 * [PlaylistRepository] の Room 実装。
 *
 * @param Executor DAO を保持する任意の型（例: AppDatabase）
 * @param daoProvider Executor から [PlaylistDao] を取得するラムダ
 */
class RoomPlaylistRepository<Executor>(private val daoProvider: (Executor) -> PlaylistDao) :
    PlaylistRepository<Executor> {

    override fun getAll(executor: Executor): Flow<List<Playlist>> =
        daoProvider(executor).getAll().map { list -> list.map { it.toDomain() } }

    override fun getFromId(executor: Executor, id: Long): Flow<Playlist?> =
        daoProvider(executor).getFromId(id).map { it?.toDomain() }

    override suspend fun getAllSync(executor: Executor): List<Playlist> =
        daoProvider(executor).getAllSync().map { it.toDomain() }

    override suspend fun getFromIdSync(executor: Executor, id: Long): Playlist? =
        daoProvider(executor).getFromIdSync(id)?.toDomain()

    override suspend fun getFromIdsSync(executor: Executor, ids: List<Long>): List<Playlist> =
        daoProvider(executor).getFromIdsSync(ids).map { it.toDomain() }

    override suspend fun update(executor: Executor, playlist: Playlist) {
        daoProvider(executor).update(playlist.toEntity())
    }

    override suspend fun updateAll(executor: Executor, playlists: List<Playlist>) {
        daoProvider(executor).update(playlists.map { it.toEntity() })
    }

    override suspend fun insert(executor: Executor, playlist: Playlist): Long =
        daoProvider(executor).insert(playlist.toEntity())

    override suspend fun delete(executor: Executor, playlist: Playlist) {
        daoProvider(executor).delete(playlist.toEntity())
    }

    override suspend fun deleteAll(executor: Executor, playlists: List<Playlist>) {
        daoProvider(executor).delete(playlists.map { it.toEntity() })
    }
}