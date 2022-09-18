package net.turtton.ytalarm.structure

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.turtton.ytalarm.util.RepeatType

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    var time: String = "00:00",
    var repeatType: RepeatType = RepeatType.ONCE,
    var playListId: Long = 0,
    var loop: Boolean = false,
    // max 100(%)
    var volume: Int = 50,
    var enable: Boolean = true
)