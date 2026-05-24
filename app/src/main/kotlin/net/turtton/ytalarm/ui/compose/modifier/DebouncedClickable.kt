package net.turtton.ytalarm.ui.compose.modifier

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

private const val DEFAULT_DEBOUNCE_INTERVAL_MS = 500L

private class ClickDebouncer(private val intervalMs: Long) {
    private var lastClickAt: Long = 0L

    fun canClick(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        return if (now - lastClickAt >= intervalMs) {
            lastClickAt = now
            true
        } else {
            false
        }
    }
}

/**
 * 一定間隔内の連打を弾く `clickable`。
 *
 * Composeの`clickable`はタップを全部素通しするため、
 * Navigation遷移などの非同期処理を伴うコールバックでは多重発火が起きやすい。
 * 退場アニメーション中のscreenが残った状態でtapを受けて意図しない遷移が
 * 走るのを防ぐため、入力レイヤでも一段ガードする。
 */
@Composable
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    intervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: () -> Unit
): Modifier = composed {
    val debouncer = remember(intervalMs) { ClickDebouncer(intervalMs) }
    this.clickable(enabled = enabled) {
        if (debouncer.canClick()) {
            onClick()
        }
    }
}