@file:Suppress("FunctionNaming") // Composable functions use PascalCase

package net.turtton.ytalarm.navigation

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.compose.dialogs.DisplayData
import net.turtton.ytalarm.ui.compose.dialogs.DisplayDataThumbnail
import net.turtton.ytalarm.ui.compose.dialogs.MultiChoiceVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.UrlInputDialog
import net.turtton.ytalarm.ui.compose.screens.AboutPageScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmListScreen
import net.turtton.ytalarm.ui.compose.screens.AlarmSettingsScreen
import net.turtton.ytalarm.ui.compose.screens.PlaylistScreen
import net.turtton.ytalarm.ui.compose.screens.VideoListScreen
import net.turtton.ytalarm.ui.compose.screens.VideoPlayerScreen
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import java.util.Calendar

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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

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
        var currentPlaylistIdForDialog by remember { mutableStateOf(playlistId) }

        VideoListScreen(
            playlistId = playlistId,
            onNavigateBack = {
                navController.popBackStack()
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
                    scope.launch(Dispatchers.IO) {
                        try {
                            val request = YoutubeDLRequest(url)
                            val result = YoutubeDL.getInstance().getInfo(request)
                            val videoInfo = Json.decodeFromString<VideoInformation>(
                                result.toString()
                            )

                            // プレイリストかビデオか判定
                            if (videoInfo.typeData is VideoInformation.Type.Playlist) {
                                // プレイリストのインポート処理
                                handlePlaylistImport(
                                    videoInfo = videoInfo,
                                    playlistViewModel = playlistViewModel,
                                    videoViewModel = videoViewModel,
                                    context = context,
                                    snackbarHostState = snackbarHostState
                                )
                            } else {
                                // 単一ビデオの追加処理
                                handleVideoImport(
                                    videoInfo = videoInfo,
                                    playlistId = currentPlaylistIdForDialog,
                                    playlistViewModel = playlistViewModel,
                                    videoViewModel = videoViewModel,
                                    context = context,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("YtAlarmNavGraph", "URL import failed: $url", e)
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.message_import_failed)
                                )
                            }
                        }
                    }
                },
                onDismiss = { showUrlInputDialog = false }
            )
        }

        // MultiChoiceVideoDialog: 既存動画をプレイリストに追加
        if (showMultiChoiceDialog) {
            val allVideos = videoViewModel.allVideos.value ?: emptyList()

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
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.message_videos_added)
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("YtAlarmNavGraph", "Failed to add videos to playlist", e)
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.message_operation_failed)
                                )
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
                navController.popBackStack()
            }
            return@composable
        }

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

/**
 * プレイリストをインポートする処理
 */
@Suppress("LongParameterList")
private suspend fun handlePlaylistImport(
    videoInfo: VideoInformation,
    playlistViewModel: PlaylistViewModel,
    videoViewModel: VideoViewModel,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState
) {
    try {
        val playlistType = videoInfo.typeData as? VideoInformation.Type.Playlist
            ?: throw IllegalArgumentException("Not a playlist")

        // プレイリストを作成
        val newPlaylist = Playlist(
            id = 0,
            title = videoInfo.title ?: "Untitled Playlist",
            videos = emptyList(),
            type = Playlist.Type.Original,
            creationDate = Calendar.getInstance()
        )

        val playlistId = playlistViewModel.insertAsync(newPlaylist).await()

        // プレイリスト内の動画をインポート
        val videoIds = mutableListOf<Long>()
        for (entry in playlistType.entries) {
            if (entry.typeData is VideoInformation.Type.Video) {
                val video = entry.toVideo()
                val videoId = videoViewModel.insertAsync(video).await()
                videoIds.add(videoId)
            }
        }

        // プレイリストに動画IDを追加
        val playlist = playlistViewModel.getFromIdAsync(playlistId).await()
        if (playlist != null) {
            playlistViewModel.update(playlist.copy(videos = videoIds))
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.message_playlist_imported)
            )
        }
    } catch (e: Exception) {
        Log.e("YtAlarmNavGraph", "Playlist import failed", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.message_import_failed)
            )
        }
    }
}

/**
 * 単一ビデオをインポートしてプレイリストに追加する処理
 */
@Suppress("LongParameterList")
private suspend fun handleVideoImport(
    videoInfo: VideoInformation,
    playlistId: Long,
    playlistViewModel: PlaylistViewModel,
    videoViewModel: VideoViewModel,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState
) {
    try {
        // ビデオを作成
        val video = videoInfo.toVideo()
        val videoId = videoViewModel.insertAsync(video).await()

        // プレイリストに追加（playlistId == 0の場合は全動画モードなので追加しない）
        if (playlistId != 0L) {
            val playlist = playlistViewModel.getFromIdAsync(playlistId).await()
            if (playlist != null) {
                val updatedVideos = (playlist.videos + videoId).distinct()
                playlistViewModel.update(playlist.copy(videos = updatedVideos))
            }
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.message_video_imported)
            )
        }
    } catch (e: Exception) {
        Log.e("YtAlarmNavGraph", "Video import failed", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.message_import_failed)
            )
        }
    }
}