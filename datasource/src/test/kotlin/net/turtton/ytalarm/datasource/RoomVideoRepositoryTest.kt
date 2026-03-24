package net.turtton.ytalarm.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import net.turtton.ytalarm.datasource.dao.VideoDao
import net.turtton.ytalarm.datasource.entity.VideoEntity
import net.turtton.ytalarm.datasource.repository.RoomVideoRepository
import net.turtton.ytalarm.kernel.entity.Video
import java.util.Calendar
import kotlin.time.Instant

private class FakeVideoDao(private val videos: MutableList<VideoEntity> = mutableListOf()) :
    VideoDao {
    override fun getAll(): Flow<List<VideoEntity>> = flowOf(videos.toList())

    override suspend fun getFromIdSync(id: Long): VideoEntity? = videos.find { it.id == id }

    override suspend fun getFromIdsSync(ids: List<Long>): List<VideoEntity> =
        videos.filter { it.id in ids }

    override fun getFromIds(ids: List<Long>): Flow<List<VideoEntity>> =
        flowOf(videos.filter { it.id in ids })

    override suspend fun getExceptIdsSync(ids: List<Long>): List<VideoEntity> =
        videos.filterNot { it.id in ids }

    override fun getFromVideoIds(ids: List<String>): Flow<List<VideoEntity>> =
        flowOf(videos.filter { it.videoId in ids })

    override suspend fun getFromVideoIdSync(id: String): VideoEntity? =
        videos.find { it.videoId == id }

    override suspend fun getFromVideoIdsSync(ids: List<String>): List<VideoEntity> =
        videos.filter { it.videoId in ids }

    override suspend fun getExceptVideoIdsSync(ids: List<String>): List<VideoEntity> =
        videos.filterNot { it.videoId in ids }

    override suspend fun update(video: VideoEntity) {
        val index = videos.indexOfFirst { it.id == video.id }
        if (index >= 0) videos[index] = video
    }

    override suspend fun insert(video: VideoEntity): Long {
        val id = (videos.maxOfOrNull { it.id } ?: 0L) + 1
        videos.add(video.copy(id = id))
        return id
    }

    override suspend fun insert(videos: List<VideoEntity>): List<Long> = videos.map { insert(it) }

    override suspend fun delete(video: VideoEntity) {
        this.videos.removeAll { it.id == video.id }
    }

    override suspend fun delete(videos: List<VideoEntity>) {
        val ids = videos.map { it.id }.toSet()
        this.videos.removeAll { it.id in ids }
    }

    override suspend fun deleteAll() {
        videos.clear()
    }
}

private class FakeVideoExecutor(val dao: VideoDao)

private val repository = RoomVideoRepository<FakeVideoExecutor> { it.dao }

class RoomVideoRepositoryTest :
    FunSpec({
        val calendar = Calendar.getInstance().apply { timeInMillis = 0L }

        fun makeVideo(id: Long = 0L, videoId: String = "vid_$id"): VideoEntity = VideoEntity(
            id = id,
            videoId = videoId,
            title = "Title $id",
            thumbnailUrl = "",
            videoUrl = "",
            domain = "youtube.com",
            stateData = VideoEntity.State.Information(isStreamable = true),
            creationDate = calendar
        )

        test("getAll returns all mapped videos") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(1L, "vid_1"), makeVideo(2L, "vid_2")))
            val executor = FakeVideoExecutor(dao)
            val result = repository.getAll(executor).first()
            result.size shouldBe 2
            result[0].id shouldBe 1L
            result[1].id shouldBe 2L
        }

        test("getFromIdSync returns correct video") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(5L, "vid_5")))
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromIdSync(executor, 5L)
            result?.videoId shouldBe "vid_5"
        }

        test("getFromIdSync returns null when not found") {
            val executor = FakeVideoExecutor(FakeVideoDao())
            val result = repository.getFromIdSync(executor, 99L)
            result shouldBe null
        }

        test("getFromIdsSync returns matching videos") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L), makeVideo(2L), makeVideo(3L))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromIdsSync(executor, listOf(1L, 3L))
            result.size shouldBe 2
            result.map { it.id }.toSet() shouldBe setOf(1L, 3L)
        }

        test("getExceptIdsSync excludes specified ids") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L), makeVideo(2L), makeVideo(3L))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getExceptIdsSync(executor, listOf(2L))
            result.size shouldBe 2
            result.map { it.id }.toSet() shouldBe setOf(1L, 3L)
        }

        test("getFromVideoIdSync returns correct video") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(1L, "abc123")))
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromVideoIdSync(executor, "abc123")
            result?.videoId shouldBe "abc123"
        }

        test("getFromVideoIdsSync returns matching videos") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L, "a"), makeVideo(2L, "b"), makeVideo(3L, "c"))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromVideoIdsSync(executor, listOf("a", "c"))
            result.size shouldBe 2
            result.map { it.videoId }.toSet() shouldBe setOf("a", "c")
        }

        test("getExceptVideoIdsSync excludes specified videoIds") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L, "a"), makeVideo(2L, "b"), makeVideo(3L, "c"))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getExceptVideoIdsSync(executor, listOf("b"))
            result.size shouldBe 2
            result.map { it.videoId }.toSet() shouldBe setOf("a", "c")
        }

        test("getFromIds returns matching videos as flow") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L), makeVideo(2L), makeVideo(3L))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromIds(executor, listOf(1L, 2L)).first()
            result.size shouldBe 2
        }

        test("getFromVideoIds returns matching videos as flow") {
            val dao = FakeVideoDao(
                mutableListOf(makeVideo(1L, "a"), makeVideo(2L, "b"))
            )
            val executor = FakeVideoExecutor(dao)
            val result = repository.getFromVideoIds(executor, listOf("a")).first()
            result.size shouldBe 1
            result[0].videoId shouldBe "a"
        }

        test("insert adds video and returns id") {
            val executor = FakeVideoExecutor(FakeVideoDao())
            val video = Video(
                id = 0L,
                videoId = "new_vid",
                title = "New Video",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "youtube.com",
                state = Video.State.Information(isStreamable = true),
                creationDate = Instant.fromEpochMilliseconds(0)
            )
            val id = repository.insert(executor, video)
            id shouldBe 1L
        }

        test("insertAll adds multiple videos") {
            val executor = FakeVideoExecutor(FakeVideoDao())
            val videos = listOf(
                Video(
                    id = 0L,
                    videoId = "v1",
                    title = "V1",
                    thumbnailUrl = "",
                    videoUrl = "",
                    domain = "youtube.com",
                    state = Video.State.Information(),
                    creationDate = Instant.fromEpochMilliseconds(0)
                ),
                Video(
                    id = 0L,
                    videoId = "v2",
                    title = "V2",
                    thumbnailUrl = "",
                    videoUrl = "",
                    domain = "youtube.com",
                    state = Video.State.Information(),
                    creationDate = Instant.fromEpochMilliseconds(0)
                )
            )
            val ids = repository.insertAll(executor, videos)
            ids.size shouldBe 2
        }

        test("update modifies existing video") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(1L, "vid_1")))
            val executor = FakeVideoExecutor(dao)
            val updated = Video(
                id = 1L,
                videoId = "vid_1",
                title = "Updated Title",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "youtube.com",
                state = Video.State.Downloading,
                creationDate = Instant.fromEpochMilliseconds(0)
            )
            repository.update(executor, updated)
            val result = repository.getFromIdSync(executor, 1L)
            result?.title shouldBe "Updated Title"
            result?.state shouldBe Video.State.Downloading
        }

        test("delete removes single video") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(1L), makeVideo(2L)))
            val executor = FakeVideoExecutor(dao)
            val toDelete = Video(
                id = 1L,
                videoId = "vid_1",
                title = "",
                thumbnailUrl = "",
                videoUrl = "",
                domain = "",
                state = Video.State.Importing,
                creationDate = Instant.fromEpochMilliseconds(0)
            )
            repository.delete(executor, toDelete)
            val result = repository.getAll(executor).first()
            result.size shouldBe 1
            result[0].id shouldBe 2L
        }

        test("deleteAll removes multiple videos") {
            val dao = FakeVideoDao(mutableListOf(makeVideo(1L), makeVideo(2L), makeVideo(3L)))
            val executor = FakeVideoExecutor(dao)
            val toDelete = listOf(
                Video(
                    id = 1L,
                    videoId = "",
                    title = "",
                    thumbnailUrl = "",
                    videoUrl = "",
                    domain = "",
                    state = Video.State.Importing,
                    creationDate = Instant.fromEpochMilliseconds(0)
                ),
                Video(
                    id = 3L,
                    videoId = "",
                    title = "",
                    thumbnailUrl = "",
                    videoUrl = "",
                    domain = "",
                    state = Video.State.Importing,
                    creationDate = Instant.fromEpochMilliseconds(0)
                )
            )
            repository.deleteAll(executor, toDelete)
            val result = repository.getAll(executor).first()
            result.size shouldBe 1
            result[0].id shouldBe 2L
        }
    })