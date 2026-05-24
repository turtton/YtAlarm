@file:Suppress("FunctionNaming") // Composable functions use PascalCase

package net.turtton.ytalarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.screens.AboutPageScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmListScreen
import net.turtton.ytalarm.ui.compose.screens.AllVideosScreen
import net.turtton.ytalarm.ui.compose.screens.PlaylistScreen
import net.turtton.ytalarm.ui.compose.screens.SettingsScreen
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
        playlistScreen(navController, onOpenDrawer)
        videoListScreen(navController)
        allVideosScreen(navController, onOpenDrawer)
        videoPlayerScreen(navController)
        aboutScreen(navController)
        settingsScreen(navController)
    }
}

/**
 * source backStackEntryがtop owner ならlockを経由して navigate する。
 *
 * lock取得失敗時は何もしない(進行中のnav遷移を尊重)、
 * navigateIfOwner が拒否した場合は即lock解放(stale callback時に永続lockを避ける)。
 */
private fun NavHostController.navigateGuarded(
    from: androidx.navigation.NavBackStackEntry,
    lock: NavigationLock,
    route: String,
    builder: androidx.navigation.NavOptionsBuilder.() -> Unit = {}
) {
    if (!lock.tryAcquire()) return
    val accepted = navigateIfOwner(from, route, builder)
    if (!accepted) lock.release()
}

private fun NavHostController.popBackStackGuarded(
    from: androidx.navigation.NavBackStackEntry,
    lock: NavigationLock
) {
    if (!lock.tryAcquire()) return
    val accepted = popBackStackIfOwner(from)
    if (!accepted) lock.release()
}

/**
 * アラーム一覧画面のルート定義
 *
 * Deep link（ytalarm://alarm/{alarmId}）にも対応し、
 * 指定されたアラームIDでボトムシートを開く
 */
@Suppress("UnusedParameter") // navController reserved for potential future use with deep links
private fun NavGraphBuilder.alarmListScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    composable(
        route = YtAlarmDestination.ALARM_LIST,
        deepLinks = listOf(
            navDeepLink { uriPattern = "ytalarm://alarm/{alarmId}" }
        ),
        arguments = listOf(
            navArgument("alarmId") {
                type = NavType.LongType
                defaultValue = 0L // 0Lはボトムシートを開かない
            }
        )
    ) { backStackEntry ->
        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
        val context = LocalContext.current

        // Pre-fetch string resource for use in lambda
        val errorInvalidAlarmId = stringResource(R.string.error_invalid_alarm_id)

        // 不正なIDのエラー処理
        // 0はボトムシートを開かない（通常のアラーム一覧表示）
        // -1Lは新規作成を意味する（許可）
        // -1L未満の負値は無効
        val initialAlarmId: Long? = when {
            alarmId == 0L -> null

            // 不正なID
            alarmId < -1L -> {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    android.widget.Toast.makeText(
                        context,
                        errorInvalidAlarmId,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }

            else -> alarmId // -1L（新規作成）または正のID（編集）
        }

        AlarmListScreen(
            onOpenDrawer = onOpenDrawer,
            initialAlarmId = initialAlarmId
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
    composable(route = YtAlarmDestination.PLAYLIST) { backStackEntry ->
        val lock = rememberNavigationLock(navController, backStackEntry)
        PlaylistScreen(
            onNavigateToVideoList = { playlistId ->
                navController.navigateGuarded(
                    backStackEntry,
                    lock,
                    YtAlarmDestination.videoList(playlistId)
                ) {
                    launchSingleTop = true
                }
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
        val lock = rememberNavigationLock(navController, backStackEntry)

        VideoListScreen(
            playlistId = playlistId,
            onNavigateBack = {
                navController.popBackStackGuarded(backStackEntry, lock)
            },
            onNavigateToVideoPlayer = { videoId ->
                navController.navigateGuarded(
                    backStackEntry,
                    lock,
                    YtAlarmDestination.videoPlayer(videoId, isAlarmMode = false)
                ) {
                    launchSingleTop = true
                }
            },
            onNavigateToVideoList = { newPlaylistId ->
                navController.navigateGuarded(
                    backStackEntry,
                    lock,
                    YtAlarmDestination.videoList(newPlaylistId)
                ) {
                    launchSingleTop = true
                    popUpTo(YtAlarmDestination.videoList(0L)) {
                        inclusive = true
                    }
                }
            }
        )
    }
}

/**
 * 全動画一覧画面のルート定義
 */
private fun NavGraphBuilder.allVideosScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    composable(route = YtAlarmDestination.ALL_VIDEOS) { backStackEntry ->
        val lock = rememberNavigationLock(navController, backStackEntry)
        AllVideosScreen(
            onOpenDrawer = onOpenDrawer,
            onNavigateToVideoPlayer = { videoId ->
                navController.navigateGuarded(
                    backStackEntry,
                    lock,
                    YtAlarmDestination.videoPlayer(videoId, isAlarmMode = false)
                ) {
                    launchSingleTop = true
                }
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
        val videoId = backStackEntry.arguments?.getString("videoId")
        val isAlarmMode = backStackEntry.arguments?.getBoolean("isAlarmMode") ?: false

        // videoIdが不正な場合は前の画面に戻る
        if (videoId.isNullOrEmpty()) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.popBackStackSafely()
            }
            return@composable
        }

        val lock = rememberNavigationLock(navController, backStackEntry)
        VideoPlayerScreen(
            videoId = videoId,
            isAlarmMode = isAlarmMode,
            onDismiss = {
                navController.popBackStackGuarded(backStackEntry, lock)
            }
        )
    }
}

/**
 * About画面のルート定義
 */
private fun NavGraphBuilder.aboutScreen(navController: NavHostController) {
    composable(route = YtAlarmDestination.ABOUT) { backStackEntry ->
        val lock = rememberNavigationLock(navController, backStackEntry)
        AboutPageScreen(
            onNavigateBack = {
                navController.popBackStackGuarded(backStackEntry, lock)
            }
        )
    }
}

/**
 * 設定画面のルート定義
 */
private fun NavGraphBuilder.settingsScreen(navController: NavHostController) {
    composable(route = YtAlarmDestination.SETTINGS) { backStackEntry ->
        val lock = rememberNavigationLock(navController, backStackEntry)
        SettingsScreen(
            onNavigateBack = {
                navController.popBackStackGuarded(backStackEntry, lock)
            }
        )
    }
}