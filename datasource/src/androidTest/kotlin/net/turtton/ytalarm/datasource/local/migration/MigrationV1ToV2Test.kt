package net.turtton.ytalarm.datasource.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.turtton.ytalarm.datasource.local.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalSerializationApi::class)
@RunWith(AndroidJUnit4::class)
class MigrationV1ToV2Test {

    private val cbor = Cbor

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ----- v1 CBOR structures for test data insertion -----

    @Serializable
    private sealed interface V1VideoState {
        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.State.Importing")
        data class Importing(val state: V1WorkerState) : V1VideoState

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.State.Information")
        data class Information(
            @SerialName("is_streamable")
            val isStreamable: Boolean = true
        ) : V1VideoState

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.State.Downloading")
        data class Downloading(val state: V1WorkerState) : V1VideoState

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.State.Downloaded")
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            @SerialName("file_size")
            val fileSize: Int,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : V1VideoState
    }

    @Serializable
    private sealed interface V1WorkerState {
        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.WorkerState.Failed")
        data class Failed(val url: String) : V1WorkerState

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Video.WorkerState.Working")
        data class Working(
            @SerialName("worker_id")
            val workerId: String
        ) : V1WorkerState
    }

    @Serializable
    private sealed interface V1PlaylistType {
        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Playlist.Type.Importing")
        data object Importing : V1PlaylistType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Playlist.Type.Original")
        data object Original : V1PlaylistType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Playlist.Type.CloudPlaylist")
        data class CloudPlaylist(
            val url: String,
            @SerialName("worker_id")
            val workerId: String,
            @SerialName("sync_rule")
            val syncRule: String = "ALWAYS_ADD"
        ) : V1PlaylistType
    }

    @Serializable
    private sealed interface V1PlaylistThumbnail {
        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Playlist.Thumbnail.Video")
        data class Video(val id: Long) : V1PlaylistThumbnail

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Playlist.Thumbnail.Drawable")
        data class Drawable(val id: Int) : V1PlaylistThumbnail
    }

    @Serializable
    private sealed interface V1RepeatType {
        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Alarm.RepeatType.Once")
        data object Once : V1RepeatType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Alarm.RepeatType.Everyday")
        data object Everyday : V1RepeatType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Alarm.RepeatType.Snooze")
        data object Snooze : V1RepeatType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Alarm.RepeatType.Days")
        data class Days(val days: List<V1DayOfWeekCompat>) : V1RepeatType

        @Serializable
        @SerialName("net.turtton.ytalarm.database.structure.Alarm.RepeatType.Date")
        data class Date(
            @SerialName("target_date")
            val targetDate: String
        ) : V1RepeatType
    }

    @Serializable
    private enum class V1DayOfWeekCompat {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }

    // ----- v2 CBOR structures for reading migrated data -----

    @Serializable
    private sealed interface V2VideoState {
        @Serializable
        @SerialName("Importing")
        data object Importing : V2VideoState

        @Serializable
        @SerialName("Information")
        data class Information(
            @SerialName("is_streamable")
            val isStreamable: Boolean = true
        ) : V2VideoState

        @Serializable
        @SerialName("Downloading")
        data object Downloading : V2VideoState

        @Serializable
        @SerialName("Downloaded")
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            @SerialName("file_size")
            val fileSize: Long,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : V2VideoState

        @Serializable
        @SerialName("Failed")
        data class Failed(
            @SerialName("source_url")
            val sourceUrl: String
        ) : V2VideoState
    }

    @Serializable
    private sealed interface V2PlaylistType {
        @Serializable
        @SerialName("Importing")
        data object Importing : V2PlaylistType

        @Serializable
        @SerialName("Original")
        data object Original : V2PlaylistType

        @Serializable
        @SerialName("CloudPlaylist")
        data class CloudPlaylist(
            val url: String,
            @SerialName("sync_rule")
            val syncRule: String = "ALWAYS_ADD"
        ) : V2PlaylistType
    }

    @Serializable
    private sealed interface V2PlaylistThumbnail {
        @Serializable
        @SerialName("Video")
        data class Video(val id: Long) : V2PlaylistThumbnail

        @Serializable
        @SerialName("None")
        data object None : V2PlaylistThumbnail
    }

    @Serializable
    private sealed interface V2RepeatType {
        @Serializable
        @SerialName("Once")
        data object Once : V2RepeatType

        @Serializable
        @SerialName("Everyday")
        data object Everyday : V2RepeatType

        @Serializable
        @SerialName("Snooze")
        data object Snooze : V2RepeatType

        @Serializable
        @SerialName("Days")
        data class Days(val days: List<Int>) : V2RepeatType

        @Serializable
        @SerialName("Date")
        data class Date(
            @SerialName("target_date")
            val targetDate: String
        ) : V2RepeatType
    }

    // ----- Tests -----

    @Test
    fun migrateVideoImportingWorking_becomesImporting() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1StateBlob: ByteArray = cbor.encodeToByteArray<V1VideoState>(
            V1VideoState.Importing(V1WorkerState.Working(workerId = "uuid-1234"))
        )

        db.execSQL(
            """
            INSERT INTO videos (id, video_id, title, thumbnail_link, video_url, domain, state_data, creation_date)
            VALUES (1, 'vid1', 'Title1', '', 'http://example.com', 'example.com', ?, 0)
            """.trimIndent(),
            arrayOf(v1StateBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT state_data FROM videos WHERE id = 1")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val state: V2VideoState = cbor.decodeFromByteArray<V2VideoState>(blob)
            assertEquals(V2VideoState.Importing, state)
        }
    }

    @Test
    fun migrateVideoImportingFailed_becomesFailed() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1StateBlob: ByteArray = cbor.encodeToByteArray<V1VideoState>(
            V1VideoState.Importing(V1WorkerState.Failed(url = "http://fail.example.com"))
        )

        db.execSQL(
            """
            INSERT INTO videos (id, video_id, title, thumbnail_link, video_url, domain, state_data, creation_date)
            VALUES (2, 'vid2', 'Title2', '', 'http://example.com', 'example.com', ?, 0)
            """.trimIndent(),
            arrayOf(v1StateBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT state_data FROM videos WHERE id = 2")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val state: V2VideoState = cbor.decodeFromByteArray<V2VideoState>(blob)
            val failed = state as V2VideoState.Failed
            assertEquals("http://fail.example.com", failed.sourceUrl)
        }
    }

    @Test
    fun migrateVideoDownloadingWorking_becomesDownloading() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1StateBlob: ByteArray = cbor.encodeToByteArray<V1VideoState>(
            V1VideoState.Downloading(V1WorkerState.Working(workerId = "uuid-5678"))
        )

        db.execSQL(
            """
            INSERT INTO videos (id, video_id, title, thumbnail_link, video_url, domain, state_data, creation_date)
            VALUES (3, 'vid3', 'Title3', '', 'http://example.com', 'example.com', ?, 0)
            """.trimIndent(),
            arrayOf(v1StateBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT state_data FROM videos WHERE id = 3")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val state: V2VideoState = cbor.decodeFromByteArray<V2VideoState>(blob)
            assertEquals(V2VideoState.Downloading, state)
        }
    }

    @Test
    fun migrateVideoInformation_preserved() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1StateBlob: ByteArray = cbor.encodeToByteArray<V1VideoState>(
            V1VideoState.Information(isStreamable = false)
        )

        db.execSQL(
            """
            INSERT INTO videos (id, video_id, title, thumbnail_link, video_url, domain, state_data, creation_date)
            VALUES (4, 'vid4', 'Title4', '', 'http://example.com', 'example.com', ?, 0)
            """.trimIndent(),
            arrayOf(v1StateBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT state_data FROM videos WHERE id = 4")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val state: V2VideoState = cbor.decodeFromByteArray<V2VideoState>(blob)
            val info = state as V2VideoState.Information
            assertEquals(false, info.isStreamable)
        }
    }

    @Test
    fun migratePlaylistCloudType_removesWorkerId() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1TypeBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistType>(
            V1PlaylistType.CloudPlaylist(
                url = "http://playlist.example.com",
                workerId = "worker-uuid",
                syncRule = "ALWAYS_ADD"
            )
        )
        val v1ThumbBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistThumbnail>(
            V1PlaylistThumbnail.Drawable(id = 0)
        )
        val v1VideosBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO playlists (id, title, thumbnail, videos, type, creation_date, last_updated)
            VALUES (1, 'Playlist1', ?, ?, ?, 0, 0)
            """.trimIndent(),
            arrayOf(v1ThumbBlob, v1VideosBlob, v1TypeBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT type, thumbnail FROM playlists WHERE id = 1")
        cursor.use {
            assert(cursor.moveToFirst())
            val typeBlob = cursor.getBlob(0)
            val thumbBlob = cursor.getBlob(1)
            assertNotNull(typeBlob)
            assertNotNull(thumbBlob)

            val type: V2PlaylistType = cbor.decodeFromByteArray<V2PlaylistType>(typeBlob)
            val cloud = type as V2PlaylistType.CloudPlaylist
            assertEquals("http://playlist.example.com", cloud.url)
            assertEquals("ALWAYS_ADD", cloud.syncRule)

            val thumb: V2PlaylistThumbnail = cbor.decodeFromByteArray<V2PlaylistThumbnail>(
                thumbBlob
            )
            assertEquals(V2PlaylistThumbnail.None, thumb)
        }
    }

    @Test
    fun migratePlaylistDrawableThumbnail_becomesNone() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1TypeBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistType>(V1PlaylistType.Original)
        val v1ThumbBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistThumbnail>(
            V1PlaylistThumbnail.Drawable(id = 12345)
        )
        val v1VideosBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO playlists (id, title, thumbnail, videos, type, creation_date, last_updated)
            VALUES (2, 'Playlist2', ?, ?, ?, 0, 0)
            """.trimIndent(),
            arrayOf(v1ThumbBlob, v1VideosBlob, v1TypeBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT thumbnail FROM playlists WHERE id = 2")
        cursor.use {
            assert(cursor.moveToFirst())
            val thumbBlob = cursor.getBlob(0)
            assertNotNull(thumbBlob)
            val thumb: V2PlaylistThumbnail =
                cbor.decodeFromByteArray<V2PlaylistThumbnail>(thumbBlob)
            assertEquals(V2PlaylistThumbnail.None, thumb)
        }
    }

    @Test
    fun migratePlaylistVideoThumbnail_preserved() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1TypeBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistType>(V1PlaylistType.Original)
        val v1ThumbBlob: ByteArray = cbor.encodeToByteArray<V1PlaylistThumbnail>(
            V1PlaylistThumbnail.Video(id = 42L)
        )
        val v1VideosBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO playlists (id, title, thumbnail, videos, type, creation_date, last_updated)
            VALUES (3, 'Playlist3', ?, ?, ?, 0, 0)
            """.trimIndent(),
            arrayOf(v1ThumbBlob, v1VideosBlob, v1TypeBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT thumbnail FROM playlists WHERE id = 3")
        cursor.use {
            assert(cursor.moveToFirst())
            val thumbBlob = cursor.getBlob(0)
            assertNotNull(thumbBlob)
            val thumb: V2PlaylistThumbnail = cbor.decodeFromByteArray<V2PlaylistThumbnail>(
                thumbBlob
            )
            val video = thumb as V2PlaylistThumbnail.Video
            assertEquals(42L, video.id)
        }
    }

    @Test
    fun migrateAlarmDaysRepeatType_convertsDayOfWeekCompatToCalendarCode() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        // MONDAY=2, WEDNESDAY=4, FRIDAY=6 in Calendar
        val v1RepeatTypeBlob: ByteArray = cbor.encodeToByteArray<V1RepeatType>(
            V1RepeatType.Days(
                listOf(
                    V1DayOfWeekCompat.MONDAY,
                    V1DayOfWeekCompat.WEDNESDAY,
                    V1DayOfWeekCompat.FRIDAY
                )
            )
        )
        val v1PlayListIdBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO alarms (id, hour, minute, repeatType, playListId, shouldLoop, shouldShuffle, volume, snoozeMinute, shouldVibrate, isEnable, creation_date, last_updated)
            VALUES (1, 7, 0, ?, ?, 0, 0, 50, 10, 1, 0, 0, 0)
            """.trimIndent(),
            arrayOf(v1RepeatTypeBlob, v1PlayListIdBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT repeatType FROM alarms WHERE id = 1")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val repeatType: V2RepeatType = cbor.decodeFromByteArray<V2RepeatType>(blob)
            val days = repeatType as V2RepeatType.Days
            // Calendar.MONDAY=2, Calendar.WEDNESDAY=4, Calendar.FRIDAY=6
            assertEquals(listOf(2, 4, 6), days.days)
        }
    }

    @Test
    fun migrateAlarmOnceRepeatType_preserved() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1RepeatTypeBlob: ByteArray = cbor.encodeToByteArray<V1RepeatType>(V1RepeatType.Once)
        val v1PlayListIdBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO alarms (id, hour, minute, repeatType, playListId, shouldLoop, shouldShuffle, volume, snoozeMinute, shouldVibrate, isEnable, creation_date, last_updated)
            VALUES (2, 8, 30, ?, ?, 0, 0, 50, 10, 1, 0, 0, 0)
            """.trimIndent(),
            arrayOf(v1RepeatTypeBlob, v1PlayListIdBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT repeatType FROM alarms WHERE id = 2")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val repeatType: V2RepeatType = cbor.decodeFromByteArray<V2RepeatType>(blob)
            assertEquals(V2RepeatType.Once, repeatType)
        }
    }

    @Test
    fun migrateAlarmAllDaysOfWeek_correctCalendarCodes() {
        val db = helper.createDatabase(AppDatabase.DATABASE_NAME, 1)

        val v1RepeatTypeBlob: ByteArray = cbor.encodeToByteArray<V1RepeatType>(
            V1RepeatType.Days(
                listOf(
                    V1DayOfWeekCompat.SUNDAY,
                    V1DayOfWeekCompat.MONDAY,
                    V1DayOfWeekCompat.TUESDAY,
                    V1DayOfWeekCompat.WEDNESDAY,
                    V1DayOfWeekCompat.THURSDAY,
                    V1DayOfWeekCompat.FRIDAY,
                    V1DayOfWeekCompat.SATURDAY
                )
            )
        )
        val v1PlayListIdBlob: ByteArray = cbor.encodeToByteArray(emptyList<Long>())

        db.execSQL(
            """
            INSERT INTO alarms (id, hour, minute, repeatType, playListId, shouldLoop, shouldShuffle, volume, snoozeMinute, shouldVibrate, isEnable, creation_date, last_updated)
            VALUES (3, 6, 0, ?, ?, 0, 0, 50, 10, 1, 1, 0, 0)
            """.trimIndent(),
            arrayOf(v1RepeatTypeBlob, v1PlayListIdBlob)
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            2,
            true,
            MigrationV1ToV2
        )

        val cursor = migratedDb.query("SELECT repeatType FROM alarms WHERE id = 3")
        cursor.use {
            assert(cursor.moveToFirst())
            val blob = cursor.getBlob(0)
            assertNotNull(blob)
            val repeatType: V2RepeatType = cbor.decodeFromByteArray<V2RepeatType>(blob)
            val days = repeatType as V2RepeatType.Days
            // Calendar: SUNDAY=1, MONDAY=2, TUESDAY=3, WEDNESDAY=4, THURSDAY=5, FRIDAY=6, SATURDAY=7
            assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), days.days)
        }
    }
}