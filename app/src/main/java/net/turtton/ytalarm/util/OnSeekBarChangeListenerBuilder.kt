package net.turtton.ytalarm.util

import android.widget.SeekBar

typealias OnProgressChangedFunction = (seekBar: SeekBar?, progress: Int, fromUser: Boolean) -> Unit

typealias NullableSeekBarFunction = (SeekBar?) -> Unit

class OnSeekBarChangeListenerBuilder {
    var onProgressChanged: OnProgressChangedFunction? = null

    var onStartTrackingTouch: NullableSeekBarFunction? = null

    var onStopTrackingTouch: NullableSeekBarFunction? = null

    fun build(): SeekBar.OnSeekBarChangeListener = Impl(onProgressChanged, onStartTrackingTouch, onStopTrackingTouch)

    class Impl(
        private val onProgressChanged: OnProgressChangedFunction?,
        private val onStartTrackingTouch: NullableSeekBarFunction?,
        private val onStopTrackingTouch: NullableSeekBarFunction?
    ) : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            onProgressChanged?.invoke(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            onStartTrackingTouch?.invoke(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            onStopTrackingTouch?.invoke(seekBar)
        }
    }
}