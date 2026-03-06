package net.turtton.ytalarm.datasource.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/**
 * Room Database の Migration v1 → v2。
 *
 * スキーマ変更内容:
 * - videos.state_data: WorkerState除去、Failed(sourceUrl)追加
 * - playlists.type: CloudPlaylist.workerId除去
 * - playlists.thumbnail: Drawable(id) → None
 * - alarms.repeatType: DayOfWeekCompat(type文字列) → Calendar整数コード
 *
 * すべてのカラムは BLOB (CBOR バイナリ) 形式。
 * Migration では全行を読み込み、CBOR を変換して UPDATE する。
 *
 * 注意: このマイグレーションはスキーマDDL（テーブル構造）の変更を伴わない。
 * 変更されるのは BLOB カラム内の CBOR バイナリデータフォーマットのみである。
 */
@OptIn(ExperimentalSerializationApi::class)
object MigrationV1ToV2 : Migration(1, 2) {

    private val cbor = Cbor

    override fun migrate(db: SupportSQLiteDatabase) {
        migrateVideos(db)
        migratePlaylists(db)
        migrateAlarms(db)
    }

    // ----- Old CBOR data structures (v1) -----

    /**
     * 旧 Video.State (v1):
     * - Importing(state: WorkerState)
     * - Information(is_streamable: Boolean)
     * - Downloading(state: WorkerState)
     * - Downloaded(internal_link, file_size, is_streamable)
     */
    @Serializable
    private sealed interface OldVideoState {
        @Serializable
        @SerialName("Importing")
        data class Importing(val state: OldWorkerState) : OldVideoState

        @Serializable
        @SerialName("Information")
        data class Information(
            @SerialName("is_streamable")
            val isStreamable: Boolean = true
        ) : OldVideoState

        @Serializable
        @SerialName("Downloading")
        data class Downloading(val state: OldWorkerState) : OldVideoState

        @Serializable
        @SerialName("Downloaded")
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            @SerialName("file_size")
            val fileSize: Int,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : OldVideoState
    }

    /**
     * 旧 WorkerState (v1):
     * - Failed(url: String)
     * - Working(worker_id: UUID)
     */
    @Serializable
    private sealed interface OldWorkerState {
        @Serializable
        @SerialName("Failed")
        data class Failed(val url: String) : OldWorkerState

        @Serializable
        @SerialName("Working")
        data class Working(
            @SerialName("worker_id")
            val workerId: String
        ) : OldWorkerState
    }

    // ----- New CBOR data structures (v2) -----

    /**
     * 新 VideoEntity.State (v2):
     * - Importing (data object)
     * - Information(is_streamable)
     * - Downloading (data object)
     * - Downloaded(internal_link, file_size, is_streamable)
     * - Failed(source_url)
     */
    @Serializable
    private sealed interface NewVideoState {
        @Serializable
        @SerialName("Importing")
        data object Importing : NewVideoState

        @Serializable
        @SerialName("Information")
        data class Information(
            @SerialName("is_streamable")
            val isStreamable: Boolean = true
        ) : NewVideoState

        @Serializable
        @SerialName("Downloading")
        data object Downloading : NewVideoState

        @Serializable
        @SerialName("Downloaded")
        data class Downloaded(
            @SerialName("internal_link")
            val internalLink: String,
            @SerialName("file_size")
            val fileSize: Int,
            @SerialName("is_streamable")
            val isStreamable: Boolean
        ) : NewVideoState

        @Serializable
        @SerialName("Failed")
        data class Failed(
            @SerialName("source_url")
            val sourceUrl: String
        ) : NewVideoState
    }

    /**
     * 旧 Playlist.Type (v1):
     * - Importing
     * - Original
     * - CloudPlaylist(url, worker_id, sync_rule)
     */
    @Serializable
    private sealed interface OldPlaylistType {
        @Serializable
        @SerialName("Importing")
        data object Importing : OldPlaylistType

        @Serializable
        @SerialName("Original")
        data object Original : OldPlaylistType

        @Serializable
        @SerialName("CloudPlaylist")
        data class CloudPlaylist(
            val url: String,
            @SerialName("worker_id")
            val workerId: String,
            @SerialName("sync_rule")
            val syncRule: String = "ALWAYS_ADD"
        ) : OldPlaylistType
    }

    /**
     * 新 PlaylistEntity.Type (v2):
     * - Importing
     * - Original
     * - CloudPlaylist(url, sync_rule) (worker_id除去)
     */
    @Serializable
    private sealed interface NewPlaylistType {
        @Serializable
        @SerialName("Importing")
        data object Importing : NewPlaylistType

        @Serializable
        @SerialName("Original")
        data object Original : NewPlaylistType

        @Serializable
        @SerialName("CloudPlaylist")
        data class CloudPlaylist(
            val url: String,
            @SerialName("sync_rule")
            val syncRule: String = "ALWAYS_ADD"
        ) : NewPlaylistType
    }

    /**
     * 旧 Playlist.Thumbnail (v1):
     * - Video(id: Long)
     * - Drawable(id: Int)
     */
    @Serializable
    private sealed interface OldPlaylistThumbnail {
        @Serializable
        @SerialName("Video")
        data class Video(val id: Long) : OldPlaylistThumbnail

        @Serializable
        @SerialName("Drawable")
        data class Drawable(val id: Int) : OldPlaylistThumbnail
    }

    /**
     * 新 PlaylistEntity.Thumbnail (v2):
     * - Video(id: Long)
     * - None
     */
    @Serializable
    private sealed interface NewPlaylistThumbnail {
        @Serializable
        @SerialName("Video")
        data class Video(val id: Long) : NewPlaylistThumbnail

        @Serializable
        @SerialName("None")
        data object None : NewPlaylistThumbnail
    }

    /**
     * 旧 Alarm.RepeatType (v1):
     * - Once
     * - Everyday
     * - Snooze
     * - Days(days: List<OldDayOfWeekCompat>)
     * - Date(target_date: String)
     */
    @Serializable
    private sealed interface OldRepeatType {
        @Serializable
        @SerialName("Once")
        data object Once : OldRepeatType

        @Serializable
        @SerialName("Everyday")
        data object Everyday : OldRepeatType

        @Serializable
        @SerialName("Snooze")
        data object Snooze : OldRepeatType

        @Serializable
        @SerialName("Days")
        data class Days(val days: List<OldDayOfWeekCompat>) : OldRepeatType

        @Serializable
        @SerialName("Date")
        data class Date(
            @SerialName("target_date")
            val targetDate: String
        ) : OldRepeatType
    }

    /**
     * 旧 DayOfWeekCompat (v1): serializable enum
     * MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     */
    @Serializable
    private enum class OldDayOfWeekCompat {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY;

        @Suppress("MagicNumber")
        fun toCalendarCode(): Int = when (this) {
            MONDAY -> 2

            // Calendar.MONDAY
            TUESDAY -> 3

            // Calendar.TUESDAY
            WEDNESDAY -> 4

            // Calendar.WEDNESDAY
            THURSDAY -> 5

            // Calendar.THURSDAY
            FRIDAY -> 6

            // Calendar.FRIDAY
            SATURDAY -> 7

            // Calendar.SATURDAY
            SUNDAY -> 1 // Calendar.SUNDAY
        }
    }

    /**
     * 新 AlarmEntity.RepeatType (v2):
     * - Once
     * - Everyday
     * - Snooze
     * - Days(days: List<Int>) (Calendar曜日コード)
     * - Date(target_date: String)
     */
    @Serializable
    private sealed interface NewRepeatType {
        @Serializable
        @SerialName("Once")
        data object Once : NewRepeatType

        @Serializable
        @SerialName("Everyday")
        data object Everyday : NewRepeatType

        @Serializable
        @SerialName("Snooze")
        data object Snooze : NewRepeatType

        @Serializable
        @SerialName("Days")
        data class Days(val days: List<Int>) : NewRepeatType

        @Serializable
        @SerialName("Date")
        data class Date(
            @SerialName("target_date")
            val targetDate: String
        ) : NewRepeatType
    }

    // ----- Migration functions -----

    private fun migrateVideos(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT id, state_data FROM videos")
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val newBlob = cursor.getBlob(1)?.let { convertVideoStateBlob(it) }
                    ?: continue
                db.execSQL(
                    "UPDATE videos SET state_data = ? WHERE id = ?",
                    arrayOf(newBlob, id)
                )
            }
        }
    }

    private fun convertVideoStateBlob(blob: ByteArray): ByteArray? = runCatching {
        val oldState: OldVideoState = cbor.decodeFromByteArray(blob)
        val newState: NewVideoState = convertVideoState(oldState)
        cbor.encodeToByteArray(newState)
    }.getOrNull()

    private fun convertVideoState(old: OldVideoState): NewVideoState = when (old) {
        is OldVideoState.Importing -> when (val workerState = old.state) {
            is OldWorkerState.Failed -> NewVideoState.Failed(sourceUrl = workerState.url)
            is OldWorkerState.Working -> NewVideoState.Importing
        }

        is OldVideoState.Information -> NewVideoState.Information(isStreamable = old.isStreamable)

        is OldVideoState.Downloading -> when (val workerState = old.state) {
            is OldWorkerState.Failed -> NewVideoState.Failed(sourceUrl = workerState.url)
            is OldWorkerState.Working -> NewVideoState.Downloading
        }

        is OldVideoState.Downloaded -> NewVideoState.Downloaded(
            internalLink = old.internalLink,
            fileSize = old.fileSize,
            isStreamable = old.isStreamable
        )
    }

    private fun migratePlaylists(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT id, type, thumbnail FROM playlists")
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val typeBlob = cursor.getBlob(1)
                val thumbnailBlob = cursor.getBlob(2)

                val newTypeBlob = typeBlob?.let { blob ->
                    runCatching {
                        val oldType: OldPlaylistType = cbor.decodeFromByteArray(blob)
                        val newType: NewPlaylistType = convertPlaylistType(oldType)
                        cbor.encodeToByteArray(newType)
                    }.getOrNull()
                }

                val newThumbnailBlob = thumbnailBlob?.let { blob ->
                    runCatching {
                        val oldThumbnail: OldPlaylistThumbnail = cbor.decodeFromByteArray(blob)
                        val newThumbnail: NewPlaylistThumbnail = convertThumbnail(oldThumbnail)
                        cbor.encodeToByteArray(newThumbnail)
                    }.getOrNull()
                }

                if (newTypeBlob != null || newThumbnailBlob != null) {
                    val setType = if (newTypeBlob != null) "type = ?" else null
                    val setThumb = if (newThumbnailBlob != null) "thumbnail = ?" else null
                    val setClauses = listOfNotNull(setType, setThumb).joinToString(", ")
                    val args = buildList {
                        if (newTypeBlob != null) add(newTypeBlob)
                        if (newThumbnailBlob != null) add(newThumbnailBlob)
                        add(id)
                    }
                    db.execSQL("UPDATE playlists SET $setClauses WHERE id = ?", args.toTypedArray())
                }
            }
        }
    }

    private fun convertPlaylistType(old: OldPlaylistType): NewPlaylistType = when (old) {
        is OldPlaylistType.Importing -> NewPlaylistType.Importing

        is OldPlaylistType.Original -> NewPlaylistType.Original

        is OldPlaylistType.CloudPlaylist -> NewPlaylistType.CloudPlaylist(
            url = old.url,
            syncRule = old.syncRule
        )
    }

    private fun convertThumbnail(old: OldPlaylistThumbnail): NewPlaylistThumbnail = when (old) {
        is OldPlaylistThumbnail.Video -> NewPlaylistThumbnail.Video(id = old.id)
        is OldPlaylistThumbnail.Drawable -> NewPlaylistThumbnail.None
    }

    private fun migrateAlarms(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT id, repeatType FROM alarms")
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val newBlob = cursor.getBlob(1)?.let { convertRepeatTypeBlob(it) }
                    ?: continue
                db.execSQL(
                    "UPDATE alarms SET repeatType = ? WHERE id = ?",
                    arrayOf(newBlob, id)
                )
            }
        }
    }

    private fun convertRepeatTypeBlob(blob: ByteArray): ByteArray? = runCatching {
        val oldRepeatType: OldRepeatType = cbor.decodeFromByteArray(blob)
        val newRepeatType: NewRepeatType = convertRepeatType(oldRepeatType)
        cbor.encodeToByteArray(newRepeatType)
    }.getOrNull()

    private fun convertRepeatType(old: OldRepeatType): NewRepeatType = when (old) {
        is OldRepeatType.Once -> NewRepeatType.Once

        is OldRepeatType.Everyday -> NewRepeatType.Everyday

        is OldRepeatType.Snooze -> NewRepeatType.Snooze

        is OldRepeatType.Days -> NewRepeatType.Days(
            days = old.days.map { it.toCalendarCode() }
        )

        is OldRepeatType.Date -> NewRepeatType.Date(targetDate = old.targetDate)
    }
}