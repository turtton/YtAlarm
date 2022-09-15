package net.turtton.ytalarm.structure

import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.annotation.StringRes
import net.turtton.ytalarm.util.OnSeekBarChangeListenerBuilder

sealed interface AramSettingData {
    val nameResourceId: Int

    data class NormalData(@StringRes override val nameResourceId: Int, var value: String, var onClick: ((View) -> Unit)? = null): AramSettingData
    data class ToggleData(@StringRes override val nameResourceId: Int, var value: Boolean, @StringRes val descriptionKeyId: Int? = null, var onCheckedChanged: ((CompoundButton, Boolean) -> Unit)? = null): AramSettingData
    data class PercentData(@StringRes override val nameResourceId: Int, var value: Int, val max: Int, val builder: OnSeekBarChangeListenerBuilder.() -> Unit): AramSettingData


}