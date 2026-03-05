package net.turtton.ytalarm.datasource.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.turtton.ytalarm.datasource.dao.VideoDao
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.repository.VideoRepository

/**
 * [VideoRepository] の Room 実装。
 *
 * @param Executor DAO を保持する任意の型（例: AppDatabase）
 * @param daoProvider Executor から [VideoDao] を取得するラムダ
 */
class RoomVideoRepository<Executor>(private val daoProvider: (Executor) -> VideoDao) :
    VideoRepository<Executor> {

    override fun getAll(executor: Executor): Flow<List<Video>> =
        daoProvider(executor).getAll().map { list -> list.map { it.toDomain() } }

    override fun getFromIds(executor: Executor, ids: List<Long>): Flow<List<Video>> =
        daoProvider(executor).getFromIds(ids).map { list -> list.map { it.toDomain() } }

    override fun getFromVideoIds(executor: Executor, ids: List<String>): Flow<List<Video>> =
        daoProvider(executor).getFromVideoIds(ids).map { list -> list.map { it.toDomain() } }

    override suspend fun getFromIdSync(executor: Executor, id: Long): Video? =
        daoProvider(executor).getFromIdSync(id)?.toDomain()

    override suspend fun getFromIdsSync(executor: Executor, ids: List<Long>): List<Video> =
        daoProvider(executor).getFromIdsSync(ids).map { it.toDomain() }

    override suspend fun getExceptIdsSync(executor: Executor, ids: List<Long>): List<Video> =
        daoProvider(executor).getExceptIdsSync(ids).map { it.toDomain() }

    override suspend fun getFromVideoIdSync(executor: Executor, id: String): Video? =
        daoProvider(executor).getFromVideoIdSync(id)?.toDomain()

    override suspend fun getFromVideoIdsSync(executor: Executor, ids: List<String>): List<Video> =
        daoProvider(executor).getFromVideoIdsSync(ids).map { it.toDomain() }

    override suspend fun getExceptVideoIdsSync(executor: Executor, ids: List<String>): List<Video> =
        daoProvider(executor).getExceptVideoIdsSync(ids).map { it.toDomain() }

    override suspend fun update(executor: Executor, video: Video) {
        daoProvider(executor).update(video.toEntity())
    }

    override suspend fun insert(executor: Executor, video: Video): Long =
        daoProvider(executor).insert(video.toEntity())

    override suspend fun insertAll(executor: Executor, videos: List<Video>): List<Long> =
        daoProvider(executor).insert(videos.map { it.toEntity() })

    override suspend fun delete(executor: Executor, video: Video) {
        daoProvider(executor).delete(video.toEntity())
    }

    override suspend fun deleteAll(executor: Executor, videos: List<Video>) {
        daoProvider(executor).delete(videos.map { it.toEntity() })
    }
}