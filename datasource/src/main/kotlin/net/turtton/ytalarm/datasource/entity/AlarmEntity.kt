package net.turtton.ytalarm.datasource.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.turtton.ytalarm.datasource.serializer.DateSerializer
import java.util.Calendar

@Entity(tableName = "alarms")
data class AlarmEntity(
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
    val volume: Int = 50,
    val snoozeMinute: Int = 10,
    val shouldVibrate: Boolean = true,
    @ColumnInfo(name = "isEnable")
    val isEnabled: Boolean = false,
    @ColumnInfo(name = "creation_date")
    val creationDate: Calendar = Calendar.getInstance(),
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Calendar = Calendar.getInstance()
) {
    @Serializable
    sealed interface RepeatType {
        @Serializable
        data object Once : RepeatType

        @Serializable
        data object Everyday : RepeatType

        @Serializable
        data object Snooze : RepeatType

        @Serializable
        data class Days(val days: List<Int>) : RepeatType {
            init {
                require(days.isNotEmpty()) { "Days list must not be empty" }
                require(days.all { it in Calendar.SUNDAY..Calendar.SATURDAY }) {
                    "Days must contain valid Calendar day constants (1-7)"
                }
            }
        }

        @Serializable
        data class Date(
            @Serializable(DateSerializer::class)
            @SerialName("target_date")
            val targetDate: java.util.Date
        ) : RepeatType
    }
}