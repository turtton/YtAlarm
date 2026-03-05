package net.turtton.ytalarm.kernel.repository

import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.kernel.entity.Video

interface VideoRepository<Executor> {
    fun getAll(executor: Executor): Flow<List<Video>>

    fun getFromIds(executor: Executor, ids: List<Long>): Flow<List<Video>>

    fun getFromVideoIds(executor: Executor, ids: List<String>): Flow<List<Video>>

    suspend fun getFromIdSync(executor: Executor, id: Long): Video?

    suspend fun getFromIdsSync(executor: Executor, ids: List<Long>): List<Video>

    suspend fun getExceptIdsSync(executor: Executor, ids: List<Long>): List<Video>

    suspend fun getFromVideoIdSync(executor: Executor, id: String): Video?

    suspend fun getFromVideoIdsSync(executor: Executor, ids: List<String>): List<Video>

    suspend fun getExceptVideoIdsSync(executor: Executor, ids: List<String>): List<Video>

    suspend fun update(executor: Executor, video: Video)

    suspend fun insert(executor: Executor, video: Video): Long

    suspend fun insertAll(executor: Executor, videos: List<Video>): List<Long>

    suspend fun delete(executor: Executor, video: Video)

    suspend fun deleteAll(executor: Executor, videos: List<Video>)
}