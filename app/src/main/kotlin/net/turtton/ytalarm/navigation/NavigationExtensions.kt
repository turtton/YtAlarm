package net.turtton.ytalarm.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

/**
 * source となる [NavBackStackEntry] が現在のtop owner であるときだけ navigate する。
 *
 * Navigation Composeのnavigate/popBackStackは非同期に処理されるため、
 * 退場アニメーション中や遷移完了前のtap連打で、すでに退場済みの screen に
 * 紐づいたcallbackから多重navigateが入り、意図しない画面遷移が起きることがある
 * (playlist再タップで動画が直接再生される等)。
 *
 * lifecycle state (RESUMED/STARTED) ではなく `currentBackStackEntry == from` を判定軸にすることで、
 * - 退場済みscreenからの stale callback は確実に遮断 (top ではない)
 * - top にいる正常画面からの tap は、復帰直後のアニメーション中(まだ STARTED)でも遮断しない
 * という両立を実現する。
 */
fun NavHostController.navigateIfOwner(
    from: NavBackStackEntry,
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean = if (currentBackStackEntry?.id == from.id) {
    navigate(route, builder)
    true
} else {
    false
}

/**
 * source [NavBackStackEntry] が現在のtop owner かつ 戻り先がある場合のみ popBackStack する。
 * 返り値は実際に pop が実行されたかどうか。
 */
fun NavHostController.popBackStackIfOwner(from: NavBackStackEntry): Boolean =
    if (currentBackStackEntry?.id == from.id && previousBackStackEntry != null) {
        popBackStack()
    } else {
        false
    }

fun NavHostController.navigateAndPopUp(route: String, popUpToRoute: String) {
    navigate(route) {
        popUpTo(popUpToRoute) {
            inclusive = true
        }
    }
}

/**
 * 指定したrouteへナビゲートし、backStackをクリアする
 *
 * 例: ログアウト時に全画面履歴をクリアする場合
 *
 * @param route ナビゲート先のルート
 */
fun NavHostController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
    }
}

/**
 * 単一トップモードでナビゲート
 *
 * 同じrouteが既にスタックにある場合、新しいインスタンスを作らず既存のものを再利用。
 *
 * @param route ナビゲート先のルート
 */
fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * 安全にバックスタックをポップする
 *
 * バックスタックに戻る先がある場合のみpopBackStack()を実行する。
 * 戻る連打による白画面バグを防止する。
 *
 * @return popBackStackが実行された場合はtrue、バックスタックが空で実行されなかった場合はfalse
 */
fun NavHostController.popBackStackSafely(): Boolean = if (previousBackStackEntry != null) {
    popBackStack()
} else {
    false
}