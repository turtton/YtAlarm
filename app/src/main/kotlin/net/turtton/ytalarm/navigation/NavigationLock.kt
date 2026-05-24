package net.turtton.ytalarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry

/**
 * 画面単位の one-shot navigation lock。
 *
 * 最初の navigate/pop 試行で lock し、画面が再 RESUME したときに解除する。
 * これにより、退場アニメ中のtap連打による多重画面遷移を、UX を犠牲にせず防げる。
 * 時間ベースの debounce と違い、画面が戻ってきた瞬間に再 tap 可能になる。
 *
 * **スレッド前提**: Compose UI スレッド (click ハンドラ / Lifecycle callback) からのみ
 * 呼び出すこと。内部は単なる `MutableState<Boolean>` で thread-safe ではない。
 */
class NavigationLock internal constructor(private val state: MutableState<Boolean>) {
    fun tryAcquire(): Boolean = if (state.value) {
        false
    } else {
        state.value = true
        true
    }
    fun release() {
        state.value = false
    }
}

@Composable
fun rememberNavigationLock(backStackEntry: NavBackStackEntry?): NavigationLock {
    val state = remember { mutableStateOf(false) }
    val lock = remember { NavigationLock(state) }
    DisposableEffect(backStackEntry) {
        if (backStackEntry == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) lock.release()
            }
            backStackEntry.lifecycle.addObserver(observer)
            onDispose { backStackEntry.lifecycle.removeObserver(observer) }
        }
    }
    return lock
}