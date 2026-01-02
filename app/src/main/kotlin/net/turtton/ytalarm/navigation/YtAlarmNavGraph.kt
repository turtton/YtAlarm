@file:Suppress("FunctionNaming") // Composable functions use PascalCase

package net.turtton.ytalarm.navigation

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.ui.compose.dialogs.DisplayData
import net.turtton.ytalarm.ui.compose.dialogs.DisplayDataThumbnail
import net.turtton.ytalarm.ui.compose.dialogs.MultiChoiceVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.UrlInputDialog
import net.turtton.ytalarm.ui.compose.screens.AboutPageScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmListScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmSettingsScreen
import net.turtton.ytalarm.ui.compose.screens.AllVideosScreen
import net.turtton.ytalarm.ui.compose.screens.PlaylistScreen
import net.turtton.ytalarm.ui.compose.screens.SettingsScreen
import net.turtton.ytalarm.ui.compose.screens.VideoListScreen
import net.turtton.ytalarm.ui.compose.screens.VideoPlayerScreen
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

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
        allVideosScreen(navController, onOpenDrawer)
        videoPlayerScreen(navController)
        aboutScreen()
        settingsScreen(navController)
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
        ),
        deepLinks = listOf(
            navDeepLink { uriPattern = "ytalarm://alarm/{alarmId}" }
        )
    ) { backStackEntry ->
        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
        val context = LocalContext.current

        // Pre-fetch string resource for use in lambda
        val errorInvalidAlarmId = stringResource(R.string.error_invalid_alarm_id)

        // Deep Linkで不正なIDが渡された場合のエラー処理
        // 0はRoom DBで使われないID、負の値も無効
        // -1Lは内部的に新規作成を意味するが、Deep Linkからは許可しない
        if (alarmId <= 0L) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                android.widget.Toast.makeText(
                    context,
                    errorInvalidAlarmId,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                navController.navigate(YtAlarmDestination.ALARM_LIST) {
                    popUpTo(YtAlarmDestination.ALARM_LIST) { inclusive = true }
                }
            }
            return@composable
        }

        AlarmSettingsScreen(
            alarmId = alarmId,
            onNavigateBack = {
                navController.popBackStackSafely()
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
@Suppress("LongMethod")
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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Pre-fetch string resources for use in lambdas
        val msgVideosAdded = stringResource(R.string.message_videos_added)
        val msgOperationFailed = stringResource(R.string.message_operation_failed)

        // ViewModels
        val videoViewModel: VideoViewModel = viewModel(
            factory = VideoViewModelFactory(
                (context.applicationContext as YtApplication).repository
            )
        )
        val playlistViewModel: PlaylistViewModel = viewModel(
            factory = PlaylistViewModelFactory(
                (context.applicationContext as YtApplication).repository
            )
        )

        // ダイアログ状態管理
        var showUrlInputDialog by remember { mutableStateOf(false) }
        var showMultiChoiceDialog by remember { mutableStateOf(false) }
        var currentPlaylistIdForDialog by remember { mutableLongStateOf(playlistId) }

        VideoListScreen(
            playlistId = playlistId,
            onNavigateBack = {
                navController.popBackStackSafely()
            },
            onNavigateToVideoPlayer = { videoId ->
                navController.navigate(YtAlarmDestination.videoPlayer(videoId, isAlarmMode = false))
            },
            onShowUrlInputDialog = { plId ->
                currentPlaylistIdForDialog = plId
                showUrlInputDialog = true
            },
            onShowMultiChoiceDialog = { plId ->
                currentPlaylistIdForDialog = plId
                showMultiChoiceDialog = true
            }
        )

        // UrlInputDialog: URLから動画/プレイリストを追加
        if (showUrlInputDialog) {
            UrlInputDialog(
                onConfirm = { url ->
                    // VideoInfoDownloadWorkerを使用してバックグラウンドでインポート
                    // Workerが重複チェック、プレイリスト追加、通知などを処理
                    val targetPlaylists = if (currentPlaylistIdForDialog != 0L) {
                        longArrayOf(currentPlaylistIdForDialog)
                    } else {
                        longArrayOf()
                    }
                    VideoInfoDownloadWorker.registerWorker(
                        context,
                        url,
                        targetPlaylists
                    )
                    showUrlInputDialog = false
                },
                onDismiss = { showUrlInputDialog = false }
            )
        }

        // MultiChoiceVideoDialog: 既存動画をプレイリストに追加
        if (showMultiChoiceDialog) {
            val allVideos by videoViewModel.allVideos.observeAsState(emptyList())

            MultiChoiceVideoDialog(
                displayDataList = allVideos.map { video ->
                    DisplayData(
                        id = video.id,
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl?.let { DisplayDataThumbnail.Url(it) }
                    )
                },
                onConfirm = { selectedIds ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val playlist = playlistViewModel
                                .getFromIdAsync(currentPlaylistIdForDialog)
                                .await()
                            if (playlist != null) {
                                val updatedVideos = (playlist.videos + selectedIds).distinct()
                                playlistViewModel.update(playlist.copy(videos = updatedVideos))

                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(msgVideosAdded)
                                }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: android.database.sqlite.SQLiteException) {
                            Log.e("YtAlarmNavGraph", "Database error adding videos to playlist", e)
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(msgOperationFailed)
                            }
                        } catch (e: IllegalStateException) {
                            Log.e("YtAlarmNavGraph", "Failed to add videos to playlist", e)
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(msgOperationFailed)
                            }
                        }
                    }
                },
                onDismiss = { showMultiChoiceDialog = false }
            )
        }
    }
}

/**
 * 全動画一覧画面のルート定義
 */
private fun NavGraphBuilder.allVideosScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    composable(route = YtAlarmDestination.ALL_VIDEOS) {
        val context = LocalContext.current
        var showUrlInputDialog by remember { mutableStateOf(false) }

        AllVideosScreen(
            onOpenDrawer = onOpenDrawer,
            onNavigateToVideoPlayer = { videoId ->
                navController.navigate(YtAlarmDestination.videoPlayer(videoId, isAlarmMode = false))
            },
            onShowUrlInputDialog = {
                showUrlInputDialog = true
            }
        )

        // UrlInputDialog: URLから動画を追加
        if (showUrlInputDialog) {
            UrlInputDialog(
                onConfirm = { url ->
                    // 全動画モードでは特定のプレイリストに追加しない
                    VideoInfoDownloadWorker.registerWorker(
                        context,
                        url,
                        longArrayOf()
                    )
                    showUrlInputDialog = false
                },
                onDismiss = { showUrlInputDialog = false }
            )
        }
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

        VideoPlayerScreen(
            videoId = videoId,
            isAlarmMode = isAlarmMode,
            onDismiss = {
                navController.popBackStackSafely()
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

/**
 * 設定画面のルート定義
 */
private fun NavGraphBuilder.settingsScreen(navController: NavHostController) {
    composable(route = YtAlarmDestination.SETTINGS) {
        SettingsScreen(
            onNavigateBack = {
                navController.popBackStackSafely()
            }
        )
    }
}