@file:Suppress("FunctionNaming") // Composable functions use PascalCase

package net.turtton.ytalarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import net.turtton.ytalarm.ui.compose.screens.AboutPageScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmListScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmSettingsScreen
import net.turtton.ytalarm.ui.compose.screens.PlaylistScreen
import net.turtton.ytalarm.ui.compose.screens.VideoListScreen
import net.turtton.ytalarm.ui.compose.screens.VideoPlayerScreen

/**
 * YtAlarmアプリのNavigation Graph
 *
 * @param navController ナビゲーション制御用のコントローラー
 * @param onOpenDrawer ドロワーを開くコールバック
 * @param modifier Composableに適用するModifier
 * @param startDestination 初期表示画面のルート
 */
@Composable
fun YtAlarmNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = YtAlarmDestination.ALARM_LIST
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        alarmListScreen(navController, onOpenDrawer)
        alarmSettingsScreen(navController)
        playlistScreen(navController, onOpenDrawer)
        videoListScreen(navController)
        videoPlayerScreen(navController)
        aboutScreen()
    }
}

/**
 * アラーム一覧画面のルート定義
 */
private fun NavGraphBuilder.alarmListScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    composable(route = YtAlarmDestination.ALARM_LIST) {
        AlarmListScreen(
            onNavigateToAlarmSettings = { alarmId ->
                navController.navigate(YtAlarmDestination.alarmSettings(alarmId))
            },
            onOpenDrawer = onOpenDrawer
        )
    }
}

/**
 * アラーム設定画面のルート定義
 */
private fun NavGraphBuilder.alarmSettingsScreen(navController: NavHostController) {
    composable(
        route = YtAlarmDestination.ALARM_SETTINGS,
        arguments = listOf(
            navArgument("alarmId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L

        AlarmSettingsScreen(
            alarmId = alarmId,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

/**
 * プレイリスト一覧画面のルート定義
 */
private fun NavGraphBuilder.playlistScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    composable(route = YtAlarmDestination.PLAYLIST) {
        PlaylistScreen(
            onNavigateToVideoList = { playlistId ->
                navController.navigate(YtAlarmDestination.videoList(playlistId))
            },
            onOpenDrawer = onOpenDrawer
        )
    }
}

/**
 * 動画一覧画面のルート定義
 */
private fun NavGraphBuilder.videoListScreen(navController: NavHostController) {
    composable(
        route = YtAlarmDestination.VIDEO_LIST,
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.LongType
                defaultValue = 0L
            }
        )
    ) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L

        VideoListScreen(
            playlistId = playlistId,
            onNavigateBack = {
                navController.popBackStack()
            },
            onShowUrlInputDialog = { playlistId ->
                // TODO: UrlInputDialogの統合（Stage 2で実装）
            },
            onShowMultiChoiceDialog = { playlistId ->
                // TODO: MultiChoiceDialogの統合（Stage 2で実装）
            }
        )
    }
}

/**
 * 動画プレーヤー画面のルート定義
 */
private fun NavGraphBuilder.videoPlayerScreen(navController: NavHostController) {
    composable(
        route = YtAlarmDestination.VIDEO_PLAYER,
        arguments = listOf(
            navArgument("videoId") {
                type = NavType.StringType
            },
            navArgument("isAlarmMode") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        val isAlarmMode = backStackEntry.arguments?.getBoolean("isAlarmMode") ?: false

        VideoPlayerScreen(
            videoId = videoId,
            isAlarmMode = isAlarmMode,
            onDismiss = {
                navController.popBackStack()
            }
        )
    }
}

/**
 * About画面のルート定義
 */
private fun NavGraphBuilder.aboutScreen() {
    composable(route = YtAlarmDestination.ABOUT) {
        AboutPageScreen()
    }
}