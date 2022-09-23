package net.turtton.ytalarm.structure

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.turtton.ytalarm.util.RepeatType

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val hour: Int = 0,
    val minute: Int = 0,
    val repeatType: RepeatType = RepeatType.Once,
    val playListId: List<Long> = emptyList(),
    val loop: Boolean = false,
    // max 100(%)
    val volume: Int = 50,
    val snoozeMinute: Int = 5,
    val enable: Boolean = false
)