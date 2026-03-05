package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import net.turtton.ytalarm.datasource.dao.PlaylistDao
import net.turtton.ytalarm.datasource.entity.PlaylistEntity
import net.turtton.ytalarm.datasource.repository.RoomPlaylistRepository
import net.turtton.ytalarm.kernel.entity.Playlist
import java.util.Calendar

private class FakePlaylistDao(
    private val playlists: MutableList<PlaylistEntity> = mutableListOf()
) : PlaylistDao {
    override fun getAll(): Flow<List<PlaylistEntity>> = flowOf(playlists.toList())

    override suspend fun getAllSync(): List<PlaylistEntity> = playlists.toList()

    override fun getFromId(id: Long): Flow<PlaylistEntity?> = flowOf(playlists.find { it.id == id })

    override suspend fun getFromIdSync(id: Long): PlaylistEntity? = playlists.find { it.id == id }

    override suspend fun getFromIdsSync(ids: List<Long>): List<PlaylistEntity> =
        playlists.filter { it.id in ids }

    override suspend fun update(playlist: PlaylistEntity) {
        val index = playlists.indexOfFirst { it.id == playlist.id }
        if (index >= 0) playlists[index] = playlist
    }

    override suspend fun update(playlists: List<PlaylistEntity>) {
        playlists.forEach { update(it) }
    }

    override suspend fun insert(playlist: PlaylistEntity): Long {
        val id = (playlists.maxOfOrNull { it.id } ?: 0L) + 1
        playlists.add(playlist.copy(id = id))
        return id
    }

    override suspend fun delete(playlist: PlaylistEntity) {
        playlists.removeAll { it.id == playlist.id }
    }

    override suspend fun delete(playlists: List<PlaylistEntity>) {
        val ids = playlists.map { it.id }.toSet()
        this.playlists.removeAll { it.id in ids }
    }

    override suspend fun deleteAll() {
        playlists.clear()
    }
}

private class FakePlaylistExecutor(val dao: PlaylistDao)

private val repository = RoomPlaylistRepository<FakePlaylistExecutor> { it.dao }

class RoomPlaylistRepositoryTest :
    FunSpec({
        val calendar = Calendar.getInstance().apply { timeInMillis = 0L }

        fun makePlaylist(id: Long = 0L, title: String = "Playlist $id"): PlaylistEntity =
            PlaylistEntity(
                id = id,
                title = title,
                thumbnail = PlaylistEntity.Thumbnail.None,
                videos = emptyList(),
                type = PlaylistEntity.Type.Original,
                creationDate = calendar,
                lastUpdated = calendar
            )

        test("getAll returns all playlists") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L), makePlaylist(2L))
            )
            val executor = FakePlaylistExecutor(dao)
            val result = repository.getAll(executor).first()
            result.size shouldBe 2
        }

        test("getAllSync returns all playlists") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L, "A"), makePlaylist(2L, "B"))
            )
            val executor = FakePlaylistExecutor(dao)
            val result = repository.getAllSync(executor)
            result.size shouldBe 2
            result.map { it.title }.toSet() shouldBe setOf("A", "B")
        }

        test("getFromId returns flow with correct playlist") {
            val dao = FakePlaylistDao(mutableListOf(makePlaylist(10L, "My Playlist")))
            val executor = FakePlaylistExecutor(dao)
            val result = repository.getFromId(executor, 10L).first()
            result?.id shouldBe 10L
            result?.title shouldBe "My Playlist"
        }

        test("getFromIdSync returns correct playlist") {
            val dao = FakePlaylistDao(mutableListOf(makePlaylist(5L)))
            val executor = FakePlaylistExecutor(dao)
            val result = repository.getFromIdSync(executor, 5L)
            result?.id shouldBe 5L
        }

        test("getFromIdSync returns null when not found") {
            val executor = FakePlaylistExecutor(FakePlaylistDao())
            val result = repository.getFromIdSync(executor, 99L)
            result shouldBe null
        }

        test("getFromIdsSync returns matching playlists") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L), makePlaylist(2L), makePlaylist(3L))
            )
            val executor = FakePlaylistExecutor(dao)
            val result = repository.getFromIdsSync(executor, listOf(1L, 3L))
            result.size shouldBe 2
            result.map { it.id }.toSet() shouldBe setOf(1L, 3L)
        }

        test("insert adds playlist and returns id") {
            val executor = FakePlaylistExecutor(FakePlaylistDao())
            val playlist = Playlist(
                id = 0L,
                title = "New Playlist",
                thumbnail = Playlist.Thumbnail.None,
                videos = emptyList(),
                type = Playlist.Type.Original,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            val id = repository.insert(executor, playlist)
            id shouldBe 1L
        }

        test("update modifies existing playlist") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L, "Original Title"))
            )
            val executor = FakePlaylistExecutor(dao)
            val updated = Playlist(
                id = 1L,
                title = "Updated Title",
                thumbnail = Playlist.Thumbnail.None,
                videos = listOf(1L, 2L),
                type = Playlist.Type.Original,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            repository.update(executor, updated)
            val result = repository.getFromIdSync(executor, 1L)
            result?.title shouldBe "Updated Title"
            result?.videos shouldBe listOf(1L, 2L)
        }

        test("updateAll modifies multiple playlists") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L, "P1"), makePlaylist(2L, "P2"))
            )
            val executor = FakePlaylistExecutor(dao)
            val updatedPlaylists = listOf(
                Playlist(
                    id = 1L,
                    title = "Updated P1",
                    thumbnail = Playlist.Thumbnail.None,
                    videos = emptyList(),
                    type = Playlist.Type.Original,
                    creationDate = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0)
                ),
                Playlist(
                    id = 2L,
                    title = "Updated P2",
                    thumbnail = Playlist.Thumbnail.None,
                    videos = emptyList(),
                    type = Playlist.Type.Original,
                    creationDate = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0)
                )
            )
            repository.updateAll(executor, updatedPlaylists)
            val result = repository.getAllSync(executor)
            result.map { it.title }.toSet() shouldBe setOf("Updated P1", "Updated P2")
        }

        test("delete removes playlist") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L), makePlaylist(2L))
            )
            val executor = FakePlaylistExecutor(dao)
            val toDelete = Playlist(
                id = 1L,
                title = "",
                thumbnail = Playlist.Thumbnail.None,
                videos = emptyList(),
                type = Playlist.Type.Original,
                creationDate = Instant.fromEpochMilliseconds(0),
                lastUpdated = Instant.fromEpochMilliseconds(0)
            )
            repository.delete(executor, toDelete)
            val result = repository.getAllSync(executor)
            result.size shouldBe 1
            result[0].id shouldBe 2L
        }

        test("deleteAll removes multiple playlists") {
            val dao = FakePlaylistDao(
                mutableListOf(makePlaylist(1L), makePlaylist(2L), makePlaylist(3L))
            )
            val executor = FakePlaylistExecutor(dao)
            val toDelete = listOf(
                Playlist(
                    id = 1L,
                    title = "",
                    thumbnail = Playlist.Thumbnail.None,
                    videos = emptyList(),
                    type = Playlist.Type.Original,
                    creationDate = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0)
                ),
                Playlist(
                    id = 2L,
                    title = "",
                    thumbnail = Playlist.Thumbnail.None,
                    videos = emptyList(),
                    type = Playlist.Type.Original,
                    creationDate = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0)
                )
            )
            repository.deleteAll(executor, toDelete)
            val result = repository.getAllSync(executor)
            result.size shouldBe 1
            result[0].id shouldBe 3L
        }
    })