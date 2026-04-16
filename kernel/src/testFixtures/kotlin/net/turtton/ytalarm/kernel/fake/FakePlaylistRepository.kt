package net.turtton.ytalarm.kernel.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.repository.PlaylistRepository
import java.util.concurrent.atomic.AtomicLong

class FakePlaylistRepository : PlaylistRepository<Unit> {
    private val store = MutableStateFlow<List<Playlist>>(emptyList())
    private val nextId = AtomicLong(1L)

    val currentData: List<Playlist> get() = store.value
    val updateHistory: MutableList<Playlist> = mutableListOf()

    fun resetWith(vararg playlists: Playlist) {
        store.value = playlists.toList()
        nextId.set((playlists.maxOfOrNull { it.id } ?: 0L) + 1)
        updateHistory.clear()
    }

    override fun getAll(executor: Unit): Flow<List<Playlist>> = store

    override fun getFromId(executor: Unit, id: Long): Flow<Playlist?> =
        store.map { list -> list.find { it.id == id } }

    override suspend fun getAllSync(executor: Unit): List<Playlist> = store.value

    override suspend fun getFromIdSync(executor: Unit, id: Long): Playlist? =
        store.value.find { it.id == id }

    override suspend fun getFromIdsSync(executor: Unit, ids: List<Long>): List<Playlist> =
        store.value.filter { it.id in ids }

    override suspend fun update(executor: Unit, playlist: Playlist) {
        updateHistory.add(playlist)
        store.update { list -> list.map { if (it.id == playlist.id) playlist else it } }
    }

    override suspend fun updateAll(executor: Unit, playlists: List<Playlist>) {
        updateHistory.addAll(playlists)
        store.update { list ->
            val updateMap = playlists.associateBy { it.id }
            list.map { updateMap[it.id] ?: it }
        }
    }

    override suspend fun insert(executor: Unit, playlist: Playlist): Long {
        val id = nextId.getAndIncrement()
        store.update { it + playlist.copy(id = id) }
        return id
    }

    override suspend fun delete(executor: Unit, playlist: Playlist) {
        store.update { list -> list.filter { it.id != playlist.id } }
    }

    override suspend fun deleteAll(executor: Unit, playlists: List<Playlist>) {
        val deleteIds = playlists.map { it.id }.toSet()
        store.update { list -> list.filter { it.id !in deleteIds } }
    }
}