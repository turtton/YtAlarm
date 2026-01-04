package net.turtton.ytalarm.ui.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.compose.dialogs.VideoReimportDialog
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.videoOrderRule
import net.turtton.ytalarm.util.extensions.videoOrderUp
import net.turtton.ytalarm.util.order.VideoOrder
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

/**
 * 全動画一覧画面（Compose版）
 *
 * プレイリストに依存しない、全動画を表示する専用画面。
 * VideoListScreenContentを再利用してUIを構築する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllVideosScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit,
    onShowUrlInputDialog: () -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-fetch string resources for use in lambdas
    val msgReimportSuccess = stringResource(R.string.message_reimport_success)
    val msgReimportFailed = stringResource(R.string.message_reimport_failed)
    val msgReimportStarted = stringResource(R.string.message_reimport_started)

    val selectedItems = remember { mutableStateListOf<Long>() }
    val expandedMenus = remember { mutableStateMapOf<Long, Boolean>() }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }
    var videoToReimport by remember { mutableStateOf<Video?>(null) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.videoOrderRule
    val orderUp = preferences.videoOrderUp

    // 全動画を取得
    val videos by videoViewModel.allVideos.observeAsState(emptyList())

    // ソート処理
    val sortedVideos = remember(videos, orderRule, orderUp) {
        val mutableList = videos.toMutableList()
        when (orderRule) {
            VideoOrder.TITLE -> mutableList.sortBy { it.title }
            VideoOrder.CREATION_DATE -> mutableList.sortBy { it.creationDate.timeInMillis }
        }
        if (!orderUp) {
            mutableList.reverse()
        }
        mutableList
    }

    val playlistTitle = stringResource(R.string.nav_video_list_all)

    Box(modifier = modifier.fillMaxSize()) {
        VideoListScreenContent(
            playlistTitle = playlistTitle,
            isNewPlaylist = false,
            isAllVideosMode = true,
            videos = sortedVideos,
            playlistType = null,
            orderRule = orderRule,
            orderUp = orderUp,
            selectedItems = selectedItems.toList(),
            isFabExpanded = false,
            isSyncing = false,
            expandedMenus = expandedMenus,
            onItemSelect = { id, isSelected ->
                if (isSelected) {
                    selectedItems.add(id)
                } else {
                    selectedItems.remove(id)
                }
            },
            onItemClick = { videoId ->
                onNavigateToVideoPlayer(videoId)
            },
            onMenuClick = { video ->
                expandedMenus[video.id] = true
            },
            onMenuDismiss = { videoId ->
                expandedMenus.remove(videoId)
            },
            onSetThumbnail = { /* Not applicable for all videos mode */ },
            onDownload = { /* Not implemented yet */ },
            onReimport = { video ->
                videoToReimport = video
            },
            onDeleteSingleVideo = { video ->
                videoToDelete = video
            },
            onNavigateBack = { /* Not used in all videos mode */ },
            onOpenDrawer = onOpenDrawer,
            onDeleteVideos = { /* Not applicable for all videos mode */ },
            onSortRuleChange = { rule ->
                preferences.videoOrderRule = rule
            },
            onOrderUpToggle = {
                preferences.videoOrderUp = !orderUp
            },
            onSyncRuleChange = { /* Not applicable for all videos mode */ },
            onFabExpandToggle = { /* Not used in all videos mode */ },
            onFabMainClick = { /* Not used in all videos mode */ },
            onFabUrlClick = onShowUrlInputDialog,
            onFabMultiChoiceClick = { /* Not applicable for all videos mode */ },
            modifier = Modifier.fillMaxSize()
        )

        // Snackbar用のホスト
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 削除確認ダイアログ
    videoToDelete?.let { video ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = {
                androidx.compose.material3.Text(
                    stringResource(R.string.dialog_delete_video_title)
                )
            },
            text = {
                androidx.compose.material3.Text(
                    stringResource(R.string.dialog_delete_video_message, video.title)
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        videoViewModel.delete(video)
                        videoToDelete = null
                    }
                ) {
                    androidx.compose.material3.Text(
                        stringResource(R.string.dialog_remove_video_positive)
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { videoToDelete = null }) {
                    androidx.compose.material3.Text(
                        stringResource(R.string.dialog_remove_video_negative)
                    )
                }
            }
        )
    }

    // 再インポートダイアログ
    videoToReimport?.let { video ->
        VideoReimportDialog(
            video = video,
            onConfirm = {
                videoToReimport = null
                scope.launch {
                    snackbarHostState.showSnackbar(msgReimportStarted)
                }
                scope.launch(Dispatchers.IO) {
                    try {
                        // YoutubeDLで動画情報を再取得
                        val url = video.videoUrl.ifEmpty {
                            (video.stateData as? Video.State.Importing)
                                ?.state
                                ?.let { it as? Video.WorkerState.Failed }
                                ?.url
                                ?: error("No URL available for reimport")
                        }
                        val request = YoutubeDLRequest(url)
                            .addOption("--dump-single-json")
                            .addOption("-f", "b")
                        val result = YoutubeDL.getInstance().execute(request) { _, _, _ -> }

                        // JSONをパースして動画情報を取得
                        val json = Json { ignoreUnknownKeys = true }
                        val videoInfo = json.decodeFromString<VideoInformation>(result.out)
                        val newVideo = videoInfo.toVideo()

                        // 既存のVideoのIDと作成日時を維持したまま情報を更新
                        val updatedVideo = newVideo.copy(
                            id = video.id,
                            creationDate = video.creationDate
                        )
                        videoViewModel.update(updatedVideo)

                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(msgReimportSuccess)
                        }
                    } catch (e: CancellationException) {
                        android.util.Log.d(
                            "AllVideosScreen",
                            "Reimport cancelled for video: ${video.videoId}"
                        )
                        throw e
                    } catch (e: kotlinx.serialization.SerializationException) {
                        android.util.Log.e(
                            "AllVideosScreen",
                            "JSON parse error during reimport: ${video.videoId}",
                            e
                        )
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("$msgReimportFailed: Parse error")
                        }
                    } catch (e: java.net.UnknownHostException) {
                        android.util.Log.e(
                            "AllVideosScreen",
                            "Network error during reimport: ${video.videoId}",
                            e
                        )
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("$msgReimportFailed: Network error")
                        }
                    } catch (e: java.io.IOException) {
                        android.util.Log.e(
                            "AllVideosScreen",
                            "IO error during reimport: ${video.videoId}",
                            e
                        )
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("$msgReimportFailed: IO error")
                        }
                    } catch (e: YoutubeDLException) {
                        android.util.Log.e(
                            "AllVideosScreen",
                            "YoutubeDL error during reimport: ${video.videoId}",
                            e
                        )
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("$msgReimportFailed: Downloader error")
                        }
                    } catch (e: IllegalStateException) {
                        android.util.Log.e(
                            "AllVideosScreen",
                            "Reimport failed for video: ${video.videoId}",
                            e
                        )
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(msgReimportFailed)
                        }
                    }
                }
            },
            onDismiss = { videoToReimport = null }
        )
    }
}