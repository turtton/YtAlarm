package net.turtton.ytalarm.database.structure

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.DayOfWeekCompat
import net.turtton.ytalarm.util.serializer.DateSerializer
import java.text.DateFormat
import java.util.*

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hour: Int = 0,
    val minute: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val repeatType: RepeatType = RepeatType.Once,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val playListId: List<Long> = emptyList(),
    val shouldLoop: Boolean = false,
    val shouldShuffle: Boolean = false,
    val volume: Volume = Volume(),
    val snoozeMinute: Int = 10,
    val shouldVibrate: Boolean = true,
    val isEnable: Boolean = false,
    @ColumnInfo(name = "creation_date")
    val creationDate: Calendar = Calendar.getInstance(),
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Calendar = Calendar.getInstance()
) {

    data class Volume(val volume: Int = 50) {
        init {
            check(volume in REQUIRED_RANGE)
        }
        companion object {
            const val MAX_VOLUME = 100
            const val MIN_VOLUME = 0
            private val REQUIRED_RANGE = MIN_VOLUME..MAX_VOLUME
        }
    }

    @Serializable
    sealed interface RepeatType {
        fun getDisplay(context: Context): String

        @Serializable
        object Once : RepeatType {
            override fun getDisplay(context: Context): String {
                return context.getString(R.string.repeat_type_once)
            }
        }

        @Serializable
        object Everyday : RepeatType {
            override fun getDisplay(context: Context): String {
                return context.getString(R.string.repeat_type_everyday)
            }
        }

        @Serializable
        object Snooze : RepeatType {
            override fun getDisplay(context: Context): String = ""
        }

        @Serializable
        data class Days(val days: List<DayOfWeekCompat>) : RepeatType {
            override fun getDisplay(context: Context): String {
                return days.mapNotNull {
                    it.getDisplay(context)
                }.joinToString(separator = ", ") { it }
            }
        }

        @Serializable
        data class Date(
            @Serializable(DateSerializer::class)
            @SerialName("target_date")
            val targetDate: java.util.Date
        ) : RepeatType {
            override fun getDisplay(context: Context): String {
                return DateFormat.getDateInstance().format(targetDate)
            }
        }
    }
}