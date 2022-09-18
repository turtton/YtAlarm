package net.turtton.ytalarm.structure

import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import net.turtton.ytalarm.util.OnSeekBarChangeListenerBuilder

sealed interface AlarmSettingData {
    val nameResourceId: Int

    data class NormalData(
        @StringRes override val nameResourceId: Int,
        var value: String,
        var onClick: ((View, description: TextView) -> Unit)? = null
    ) : AlarmSettingData
    data class ToggleData(
        @StringRes override val nameResourceId: Int,
        var value: Boolean,
        @StringRes val descriptionKeyId: Int? = null,
        var onCheckedChanged: ((CompoundButton, Boolean) -> Unit)? = null
    ) : AlarmSettingData
    data class PercentData(
        @StringRes override val nameResourceId: Int,
        var value: Int,
        val max: Int,
        val builder: OnSeekBarChangeListenerBuilder.() -> Unit
    ) : AlarmSettingData
}