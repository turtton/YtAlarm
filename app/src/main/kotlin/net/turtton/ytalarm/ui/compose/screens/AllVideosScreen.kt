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
import kotlinx.coroutines.launch
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.YtApplication.Companion.dataContainerProvider
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.ui.compose.dialogs.DeleteVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.VideoReimportDialog
import net.turtton.ytalarm.ui.model.toUiModel
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.sorted
import net.turtton.ytalarm.util.extensions.videoOrderRule
import net.turtton.ytalarm.util.extensions.videoOrderUp
import net.turtton.ytalarm.viewmodel.ReimportResult
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
            (LocalContext.current.applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
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
    val msgReimportErrorParse = stringResource(R.string.message_reimport_error_parse)
    val msgReimportErrorNetwork = stringResource(R.string.message_reimport_error_network)

    val selectedItems = remember { mutableStateListOf<Long>() }
    val expandedMenus = remember { mutableStateMapOf<Long, Boolean>() }
    var videoToDeleteId by remember { mutableStateOf<Long?>(null) }
    var videoToReimportId by remember { mutableStateOf<Long?>(null) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.videoOrderRule
    val orderUp = preferences.videoOrderUp

    // 全動画を取得
    val videos by videoViewModel.allVideos.observeAsState(emptyList())

    // ソート処理
    val sortedVideos = remember(videos, orderRule, orderUp) {
        videos.sorted(orderRule, orderUp)
    }

    // ID→Videoマップ（ダイアログ等でVideoオブジェクトが必要な場合に使用）
    val videoMap = remember(videos) { videos.associateBy { it.id } }

    // UiModel変換
    val videoUiModels = remember(sortedVideos) { sortedVideos.map { it.toUiModel() } }

    val playlistTitle = stringResource(R.string.nav_video_list_all)

    Box(modifier = modifier.fillMaxSize()) {
        VideoListScreenContent(
            playlistTitle = playlistTitle,
            isNewPlaylist = false,
            isAllVideosMode = true,
            videos = videoUiModels,
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
            onMenuClick = { videoId ->
                expandedMenus[videoId] = true
            },
            onMenuDismiss = { videoId ->
                expandedMenus.remove(videoId)
            },
            onSetThumbnail = { /* Not applicable for all videos mode */ },
            onDownload = { /* Not implemented yet */ },
            onReimport = { videoId ->
                videoToReimportId = videoId
            },
            onDeleteSingleVideo = { videoId ->
                videoToDeleteId = videoId
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
    videoToDeleteId?.let { id ->
        val video = videoMap[id] ?: run {
            videoToDeleteId = null
            return@let
        }
        DeleteVideoDialog(
            videoTitle = video.title,
            onConfirm = {
                videoViewModel.delete(video)
                videoToDeleteId = null
            },
            onDismiss = { videoToDeleteId = null }
        )
    }

    // 再インポートダイアログ
    videoToReimportId?.let { id ->
        val video = videoMap[id] ?: run {
            videoToReimportId = null
            return@let
        }
        VideoReimportDialog(
            videoTitle = video.title,
            onConfirm = {
                videoToReimportId = null
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(msgReimportStarted)
                }
                scope.launch {
                    val result = videoViewModel.reimportVideo(video)
                    val message = when (result) {
                        is ReimportResult.Success -> msgReimportSuccess
                        is ReimportResult.Error.Parse -> msgReimportErrorParse
                        is ReimportResult.Error.Network -> msgReimportErrorNetwork
                        is ReimportResult.Error.NoUrl -> msgReimportFailed
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            },
            onDismiss = { videoToReimportId = null }
        )
    }
}