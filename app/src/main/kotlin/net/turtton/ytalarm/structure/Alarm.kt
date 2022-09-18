package net.turtton.ytalarm.structure

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.turtton.ytalarm.util.RepeatType

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val time: String = "00:00",
    val repeatType: RepeatType = RepeatType.Once,
    val playListId: Long? = null,
    val loop: Boolean = false,
    // max 100(%)
    val volume: Int = 50,
    val enable: Boolean = true
)