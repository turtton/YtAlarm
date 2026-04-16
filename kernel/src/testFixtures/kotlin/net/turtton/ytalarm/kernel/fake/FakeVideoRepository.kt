package net.turtton.ytalarm.kernel.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.repository.VideoRepository

class FakeVideoRepository : VideoRepository<Unit> {
    private val store = MutableStateFlow<List<Video>>(emptyList())
    private var nextId = 1L

    val currentData: List<Video> get() = store.value
    val updateHistory: MutableList<Video> = mutableListOf()

    fun seed(vararg videos: Video) {
        store.value = videos.toList()
        nextId = (videos.maxOfOrNull { it.id } ?: 0L) + 1
        updateHistory.clear()
    }

    override fun getAll(executor: Unit): Flow<List<Video>> = store

    override fun getFromIds(executor: Unit, ids: List<Long>): Flow<List<Video>> =
        store.map { list -> list.filter { it.id in ids } }

    override fun getFromVideoIds(executor: Unit, ids: List<String>): Flow<List<Video>> =
        store.map { list -> list.filter { it.videoId in ids } }

    override suspend fun getFromIdSync(executor: Unit, id: Long): Video? =
        store.value.find { it.id == id }

    override suspend fun getFromIdsSync(executor: Unit, ids: List<Long>): List<Video> =
        store.value.filter { it.id in ids }

    override suspend fun getExceptIdsSync(executor: Unit, ids: List<Long>): List<Video> =
        store.value.filter { it.id !in ids }

    override suspend fun getFromVideoIdSync(executor: Unit, id: String): Video? =
        store.value.find { it.videoId == id }

    override suspend fun getFromVideoIdsSync(executor: Unit, ids: List<String>): List<Video> =
        store.value.filter { it.videoId in ids }

    override suspend fun getExceptVideoIdsSync(executor: Unit, ids: List<String>): List<Video> =
        store.value.filter { it.videoId !in ids }

    override suspend fun update(executor: Unit, video: Video) {
        updateHistory.add(video)
        store.update { list -> list.map { if (it.id == video.id) video else it } }
    }

    override suspend fun insert(executor: Unit, video: Video): Long {
        val id = nextId++
        store.update { it + video.copy(id = id) }
        return id
    }

    override suspend fun insertAll(executor: Unit, videos: List<Video>): List<Long> {
        val ids = videos.map { nextId++ }
        val stored = videos.zip(ids) { v, id -> v.copy(id = id) }
        store.update { it + stored }
        return ids
    }

    override suspend fun delete(executor: Unit, video: Video) {
        store.update { list -> list.filter { it.id != video.id } }
    }

    override suspend fun deleteAll(executor: Unit, videos: List<Video>) {
        val deleteIds = videos.map { it.id }.toSet()
        store.update { list -> list.filter { it.id !in deleteIds } }
    }
}