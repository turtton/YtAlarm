package net.turtton.ytalarm.util

import android.content.Context
import kotlinx.serialization.Serializable
import net.turtton.ytalarm.R
import net.turtton.ytalarm.util.serializer.DateSerializer
import java.text.DateFormat
import java.util.Date as JavaDate

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
        val targetDate: JavaDate
    ) : RepeatType {
        override fun getDisplay(context: Context): String {
            return DateFormat.getDateInstance().format(targetDate)
        }
    }
}