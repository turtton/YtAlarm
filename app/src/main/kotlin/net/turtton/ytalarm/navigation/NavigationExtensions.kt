package net.turtton.ytalarm.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions

/**
 * Navigation用の拡張関数群
 *
 * 頻繁に使われるナビゲーションパターンを簡潔に記述するためのヘルパー。
 */

/**
 * 指定したrouteへナビゲートし、backStackから前の画面をポップする
 *
 * 例: ログイン後にログイン画面を履歴から削除する場合
 *
 * @param route ナビゲート先のルート
 * @param popUpToRoute ポップする画面のルート
 */
fun NavHostController.navigateAndPopUp(
    route: String,
    popUpToRoute: String
) {
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
fun NavHostController.navigateAndClearBackStack(
    route: String
) {
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
fun NavHostController.navigateSingleTop(
    route: String
) {
    navigate(route) {
        launchSingleTop = true
    }
}
