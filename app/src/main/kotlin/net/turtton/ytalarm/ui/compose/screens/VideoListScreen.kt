package net.turtton.ytalarm.ui.compose.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.YtApplication.Companion.dataContainerProvider
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.ui.compose.components.VideoItem
import net.turtton.ytalarm.ui.compose.components.VideoItemDropdownMenu
import net.turtton.ytalarm.ui.compose.dialogs.DisplayData
import net.turtton.ytalarm.ui.compose.dialogs.DisplayDataThumbnail
import net.turtton.ytalarm.ui.compose.dialogs.MultiChoiceVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.RemoveOrDeleteVideoDialog
import net.turtton.ytalarm.ui.compose.dialogs.UrlInputDialog
import net.turtton.ytalarm.ui.compose.dialogs.VideoReimportDialog
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.ui.compose.theme.Dimensions
import net.turtton.ytalarm.ui.model.VideoUiModel
import net.turtton.ytalarm.ui.model.toUiModel
import net.turtton.ytalarm.util.extensions.createImportingPlaylist
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.sorted
import net.turtton.ytalarm.util.extensions.updateThumbnail
import net.turtton.ytalarm.util.extensions.videoOrderRule
import net.turtton.ytalarm.util.extensions.videoOrderUp
import net.turtton.ytalarm.util.order.VideoOrder
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.ReimportResult
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.VideoFileDownloadWorker
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

// Sync回転アニメーション設定
// マイナス値はSyncアイコンの矢印方向と回転方向を合わせるため
private const val SYNC_ROTATION_DEGREES = -360f
private const val SYNC_ANIMATION_DURATION_MS = 2000

// アニメーションOFF端末でのCPU負荷防止用の閾値と待機時間
private const val ANIMATION_MIN_DURATION_MS = 50L
private const val ANIMATION_FALLBACK_DELAY_MS = 1000L

/**
 * 動画一覧画面のコンテンツ（プレビュー可能）
 *
 * ViewModelに依存せず、すべてのデータと関数を引数として受け取る純粋なComposable。
 * これにより、@Previewアノテーションでプレビュー可能になる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreenContent(
    playlistTitle: String,
    isNewPlaylist: Boolean,
    isAllVideosMode: Boolean,
    videos: List<VideoUiModel>,
    playlistType: Playlist.Type?,
    orderRule: VideoOrder,
    orderUp: Boolean,
    selectedItems: List<Long>,
    isFabExpanded: Boolean,
    isSyncing: Boolean,
    expandedMenus: Map<Long, Boolean>,
    onItemSelect: (Long, Boolean) -> Unit,
    onItemClick: (String) -> Unit,
    onMenuClick: (Long) -> Unit,
    onMenuDismiss: (Long) -> Unit,
    onSetThumbnail: (Long) -> Unit,
    onDownload: (Long) -> Unit,
    onReimport: (Long) -> Unit,
    onDeleteSingleVideo: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteVideos: () -> Unit,
    onSortRuleChange: (VideoOrder) -> Unit,
    onOrderUpToggle: () -> Unit,
    onSyncRuleChange: (Playlist.SyncRule) -> Unit,
    onFabExpandToggle: () -> Unit,
    onFabMainClick: () -> Unit,
    onFabUrlClick: () -> Unit,
    onFabMultiChoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onOpenDrawer: () -> Unit = {}
) {
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSyncRuleDialog by remember { mutableStateOf(false) }

    val isOriginalMode = playlistType is Playlist.Type.Original || playlistType == null
    val isSyncMode = playlistType is Playlist.Type.CloudPlaylist
    val isImportingMode = playlistType is Playlist.Type.Importing

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(playlistTitle) },
                navigationIcon = {
                    if (isAllVideosMode) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // 選択時の削除ボタン（全動画モードとSyncモードでは非表示）
                    if (selectedItems.isNotEmpty() && !isSyncMode && !isAllVideosMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    // Syncモード時のSyncRuleボタン
                    if (isSyncMode) {
                        IconButton(onClick = { showSyncRuleDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync rule")
                        }
                    }
                    // 並び替えボタン（Importingモード以外）
                    if (!isImportingMode) {
                        IconButton(onClick = onOrderUpToggle) {
                            Icon(
                                imageVector = if (orderUp) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (orderUp) {
                                    "Sort ascending"
                                } else {
                                    "Sort descending"
                                }
                            )
                        }
                        // ソートルール選択ボタン
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort rule")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            // ImportingモードのみFABを非表示
            // 全動画モード、新規プレイリストモード、既存プレイリストモードでは表示
            if (!isImportingMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // Expanded状態のサブFAB（全動画モードでは非表示）
                    AnimatedVisibility(
                        visible = isFabExpanded && isOriginalMode && !isAllVideosMode
                    ) {
                        Column {
                            // URLから追加
                            SmallFloatingActionButton(
                                onClick = onFabUrlClick,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Add from URL")
                            }
                            // 既存動画から追加
                            SmallFloatingActionButton(
                                onClick = onFabMultiChoiceClick,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Add from videos"
                                )
                            }
                        }
                    }

                    // Sync中の回転アニメーション
                    val syncRotation = remember { Animatable(0f) }
                    val shouldRotate = isSyncMode && isSyncing

                    if (isSyncMode) {
                        val currentShouldRotate by rememberUpdatedState(shouldRotate)

                        // LaunchedEffect(Unit)を使用する理由:
                        // - shouldRotateがfalseになっても即座に停止せず、1周完了してから停止させたい
                        // - LaunchedEffect(shouldRotate)だと変更時に即キャンセル→再起動され、
                        //   回転途中から0°へアニメーションして逆回転になってしまう
                        // - コンポーネント破棄時はCoroutineScopeがキャンセルされるため問題なし
                        LaunchedEffect(Unit) {
                            while (true) {
                                snapshotFlow { currentShouldRotate }.filter { it }.first()
                                val startTime = System.currentTimeMillis()
                                syncRotation.animateTo(
                                    targetValue = SYNC_ROTATION_DEGREES,
                                    animationSpec = tween(
                                        durationMillis = SYNC_ANIMATION_DURATION_MS,
                                        easing = LinearEasing
                                    )
                                )
                                // アニメーションOFF端末ではanimateToが即座に完了するため、
                                // 無限ループが高速回転してCPU負荷が発生する。これを防止する。
                                val elapsedTime = System.currentTimeMillis() - startTime
                                if (elapsedTime < ANIMATION_MIN_DURATION_MS) {
                                    delay(ANIMATION_FALLBACK_DELAY_MS)
                                }
                                syncRotation.snapTo(0f)
                            }
                        }
                    }

                    // 通常のFAB展開アニメーション
                    val expandRotation by animateFloatAsState(
                        targetValue = if (isFabExpanded && !isAllVideosMode) 45f else 0f,
                        label = "fab_rotation"
                    )

                    // 最終的な回転角度
                    val rotation = if (shouldRotate) syncRotation.value else expandRotation

                    // メインFAB
                    FloatingActionButton(
                        onClick = {
                            when {
                                // 全動画モード: 直接URL入力ダイアログを表示
                                isAllVideosMode -> onFabUrlClick()

                                // Originalモード（新規/既存プレイリスト）: 展開/折りたたみ
                                isOriginalMode -> onFabExpandToggle()

                                // Syncモード: 同期実行
                                else -> onFabMainClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when {
                                isSyncMode -> Icons.Default.Sync
                                isAllVideosMode -> Icons.Default.Link
                                else -> Icons.Default.Add
                            },
                            contentDescription = when {
                                isSyncMode -> "Sync"
                                isAllVideosMode -> "Add from URL"
                                else -> "Add video"
                            },
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (videos.isEmpty()) {
                Text(
                    text = stringResource(
                        if (isNewPlaylist) {
                            R.string.video_list_empty_new_message
                        } else {
                            R.string.video_list_empty_message
                        }
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Dimensions.fabContentPadding()
                ) {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->
                        VideoItem(
                            video = video,
                            domainOrSize = video.domain,
                            isSelected = selectedItems.contains(video.id),
                            showCheckbox = selectedItems.isNotEmpty(),
                            onToggleSelection = {
                                onItemSelect(video.id, !selectedItems.contains(video.id))
                            },
                            onClick = {
                                if (selectedItems.isEmpty()) {
                                    // Navigate to player using external video ID (String)
                                    onItemClick(video.videoId)
                                } else {
                                    // Toggle selection using internal DB ID (Long)
                                    onItemSelect(video.id, !selectedItems.contains(video.id))
                                }
                            },
                            menuExpanded = expandedMenus[video.id] ?: false,
                            onMenuClick = {
                                onMenuClick(video.id)
                            },
                            onMenuDismiss = { onMenuDismiss(video.id) },
                            menuContent = {
                                VideoItemDropdownMenu(
                                    expanded = expandedMenus[video.id] ?: false,
                                    onDismiss = { onMenuDismiss(video.id) },
                                    onSetThumbnail = { onSetThumbnail(video.id) },
                                    onDownload = { onDownload(video.id) },
                                    onReimport = { onReimport(video.id) },
                                    onDelete = { onDeleteSingleVideo(video.id) },
                                    isDownloaded = video.isDownloaded
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // ソートルール選択ダイアログ
    if (showSortDialog) {
        val sortOptions = stringArrayResource(R.array.dialog_video_order)
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.menu_video_list_option_sortrule)) },
            text = {
                Column {
                    sortOptions.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = orderRule.ordinal == index,
                                onClick = {
                                    onSortRuleChange(VideoOrder.entries[index])
                                    showSortDialog = false
                                }
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_remove_video_title)) },
            text = { Text(stringResource(R.string.dialog_remove_video_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVideos()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_remove_video_negative))
                }
            }
        )
    }

    // SyncRule選択ダイアログ（Syncモード時）
    if (showSyncRuleDialog && isSyncMode) {
        val syncRuleOptions = stringArrayResource(R.array.dialog_video_list_syncrule)
        val currentRule = (playlistType as? Playlist.Type.CloudPlaylist)?.syncRule
        AlertDialog(
            onDismissRequest = { showSyncRuleDialog = false },
            title = { Text(stringResource(R.string.menu_video_list_option_sync_rule)) },
            text = {
                Column {
                    syncRuleOptions.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentRule?.ordinal == index,
                                onClick = {
                                    onSyncRuleChange(Playlist.SyncRule.entries[index])
                                    showSyncRuleDialog = false
                                }
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncRuleDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * 動画一覧画面（Compose版）
 *
 * 機能:
 * - 動画一覧表示（LazyColumn）
 * - 複数選択機能
 * - ソート機能（タイトル、作成日）
 * - 並び替え（昇順/降順）
 * - 削除機能
 * - FAB（URLから追加、既存動画から追加、同期）
 * - 3つのモード: Original, Sync, Importing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    playlistId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit,
    onNavigateToVideoList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
        )
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
        )
    )
) {
    val currentId = playlistId
    val isNewPlaylistMode = currentId == 0L

    // playlistIdが変わった場合にscreen-local stateをリセットするためkeyブロックで囲む
    key(playlistId) {
        VideoListScreenInner(
            currentId = currentId,
            isNewPlaylistMode = isNewPlaylistMode,
            onNavigateBack = onNavigateBack,
            onNavigateToVideoPlayer = onNavigateToVideoPlayer,
            onNavigateToVideoList = onNavigateToVideoList,
            modifier = modifier,
            videoViewModel = videoViewModel,
            playlistViewModel = playlistViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoListScreenInner(
    currentId: Long,
    isNewPlaylistMode: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit,
    onNavigateToVideoList: (Long) -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
        )
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-fetch string resources for use in lambdas
    val msgThumbnailSet = stringResource(R.string.message_thumbnail_set)
    val msgReimportSuccess = stringResource(R.string.message_reimport_success)
    val msgReimportFailed = stringResource(R.string.message_reimport_failed)
    val msgSyncStarted = stringResource(R.string.snackbar_sync_started)
    val msgVideoDeleted = stringResource(R.string.message_video_deleted)
    val msgVideoRemovedFromPlaylist = stringResource(R.string.message_video_removed_from_playlist)
    val msgReimportStarted = stringResource(R.string.message_reimport_started)
    val msgReimportErrorParse = stringResource(R.string.message_reimport_error_parse)
    val msgReimportErrorNetwork = stringResource(R.string.message_reimport_error_network)
    val msgVideosAdded = stringResource(R.string.message_videos_added)
    val msgOperationFailed = stringResource(R.string.message_operation_failed)

    val playlist by if (isNewPlaylistMode) {
        remember { mutableStateOf<Playlist?>(null) }
    } else {
        playlistViewModel.getFromId(currentId).observeAsState()
    }
    val selectedItems = remember { mutableStateListOf<Long>() }
    var isFabExpanded by remember { mutableStateOf(false) }

    val expandedMenus = remember { mutableStateMapOf<Long, Boolean>() }
    var videoToDeleteId by remember { mutableStateOf<Long?>(null) }
    var videoToReimportId by remember { mutableStateOf<Long?>(null) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var showMultiChoiceDialog by remember { mutableStateOf(false) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.videoOrderRule
    val orderUp = preferences.videoOrderUp

    val playlistType = playlist?.type
    val isSyncMode = playlistType is Playlist.Type.CloudPlaylist

    val syncWorkInfo by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData("SyncWorker_$currentId")
        .observeAsState(emptyList())
    val isSyncing = syncWorkInfo.any { workInfo ->
        workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED
    }

    val videos by if (isNewPlaylistMode) {
        remember { mutableStateOf(emptyList<Video>()) }
    } else {
        val videoIds = playlist?.videos ?: emptyList()
        videoViewModel.getFromIds(videoIds).observeAsState(emptyList())
    }

    val sortedVideos = remember(videos, orderRule, orderUp) {
        videos.sorted(orderRule, orderUp)
    }

    val videoMap = remember(videos) { videos.associateBy { it.id } }

    val videoUiModels = remember(sortedVideos) { sortedVideos.map { it.toUiModel() } }

    val playlistTitle = if (isNewPlaylistMode) {
        stringResource(R.string.nav_video_list_new)
    } else {
        playlist?.title ?: stringResource(R.string.nav_video_list)
    }
    // isAllVideosMode = false: 全動画モードは AllVideosScreen で処理
    VideoListScreenContent(
        playlistTitle = playlistTitle,
        isNewPlaylist = isNewPlaylistMode,
        isAllVideosMode = false,
        videos = videoUiModels,
        playlistType = playlistType,
        orderRule = orderRule,
        orderUp = orderUp,
        selectedItems = selectedItems.toList(),
        isFabExpanded = isFabExpanded,
        isSyncing = isSyncing,
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
        onSetThumbnail = { videoId ->
            playlist?.let { pl ->
                scope.launch(Dispatchers.IO) {
                    val result = playlistViewModel.update(
                        pl.copy(thumbnail = Playlist.Thumbnail.Video(videoId))
                    )
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(msgThumbnailSet)
                        } else {
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }
            }
        },
        onDownload = { videoId ->
            VideoFileDownloadWorker.registerWorker(context, videoId)
        },
        onReimport = { videoId ->
            videoToReimportId = videoId
        },
        onDeleteSingleVideo = { videoId ->
            videoToDeleteId = videoId
        },
        onNavigateBack = onNavigateBack,
        onDeleteVideos = {
            scope.launch(Dispatchers.IO) {
                // 選択された動画をプレイリストから削除
                val currentPlaylist = playlistViewModel.getFromIdAsync(currentId).await()
                currentPlaylist?.let { pl ->
                    val result = playlistViewModel.removeVideosFromPlaylist(
                        pl,
                        selectedItems.toList()
                    )
                    if (result.isFailure) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    selectedItems.clear()
                }
            }
        },
        onSortRuleChange = { rule ->
            preferences.videoOrderRule = rule
        },
        onOrderUpToggle = {
            preferences.videoOrderUp = !orderUp
        },
        onSyncRuleChange = { rule ->
            scope.launch(Dispatchers.IO) {
                val pl = playlistViewModel.getFromIdAsync(currentId).await()
                val type = pl?.type as? Playlist.Type.CloudPlaylist
                if (pl != null && type != null) {
                    val updatedType = type.copy(syncRule = rule)
                    val result = playlistViewModel.update(pl.copy(type = updatedType))
                    if (result.isFailure) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }
            }
        },
        onFabExpandToggle = {
            isFabExpanded = !isFabExpanded
        },
        onFabMainClick = {
            // Syncモード: 同期実行
            val cloudType = playlist?.type as? Playlist.Type.CloudPlaylist
            if (cloudType != null) {
                // Worker登録を先に実行（snackbarは待機するため）
                VideoInfoDownloadWorker.registerSyncWorker(
                    context,
                    currentId,
                    cloudType.url
                )
                // スナックバーは非同期で表示
                scope.launch {
                    snackbarHostState.showSnackbar(msgSyncStarted)
                }
            }
        },
        onFabUrlClick = {
            isFabExpanded = false
            showUrlInputDialog = true
        },
        onFabMultiChoiceClick = {
            isFabExpanded = false
            showMultiChoiceDialog = true
        },
        modifier = modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState
    )

    // 削除確認ダイアログ（プレイリストから外す or 動画を削除する）
    videoToDeleteId?.let { id ->
        val video = videoMap[id] ?: run {
            videoToDeleteId = null
            return@let
        }
        RemoveOrDeleteVideoDialog(
            videoTitle = video.title,
            onRemoveFromPlaylist = {
                scope.launch(Dispatchers.IO) {
                    val result = playlist?.let { pl ->
                        playlistViewModel.removeVideosFromPlaylist(pl, listOf(id))
                    }
                    withContext(Dispatchers.Main) {
                        videoToDeleteId = null
                        if (result?.isFailure == true) {
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        } else {
                            snackbarHostState.showSnackbar(msgVideoRemovedFromPlaylist)
                        }
                    }
                }
            },
            onDeleteVideo = {
                scope.launch(Dispatchers.IO) {
                    val removeResult = playlist?.let { pl ->
                        playlistViewModel.removeVideosFromPlaylist(pl, listOf(id))
                    }
                    if (removeResult?.isFailure == true) {
                        withContext(Dispatchers.Main) {
                            videoToDeleteId = null
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                        return@launch
                    }
                    val result = videoViewModel.delete(video)
                    withContext(Dispatchers.Main) {
                        videoToDeleteId = null
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(msgVideoDeleted)
                        } else {
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }
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

    if (showUrlInputDialog) {
        UrlInputDialog(
            onConfirm = { url ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val targetPlaylistId = if (currentId != 0L) {
                            currentId
                        } else {
                            val newPlaylist = createImportingPlaylist()
                            playlistViewModel.insertAsync(newPlaylist).await()
                        }
                        VideoInfoDownloadWorker.registerWorker(
                            context,
                            url,
                            longArrayOf(targetPlaylistId)
                        )
                        withContext(Dispatchers.Main) {
                            showUrlInputDialog = false
                            if (currentId == 0L) {
                                onNavigateToVideoList(targetPlaylistId)
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        Log.e(TAG, "Failed to create playlist for URL import", e)
                        withContext(Dispatchers.Main) {
                            showUrlInputDialog = false
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }
            },
            onDismiss = { showUrlInputDialog = false }
        )
    }

    if (showMultiChoiceDialog) {
        val allVideos by videoViewModel.allVideos.observeAsState(emptyList())
        val existingVideoIds = playlist?.videos?.toSet() ?: emptySet()

        MultiChoiceVideoDialog(
            displayDataList = allVideos
                .filter { it.id !in existingVideoIds }
                .map { video ->
                    DisplayData(
                        id = video.id,
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl.takeIf {
                            it.isNotEmpty()
                        }?.let { DisplayDataThumbnail.Url(it) }
                    )
                },
            onConfirm = { selectedIds ->
                scope.launch(Dispatchers.IO) {
                    try {
                        if (currentId == 0L) {
                            var newPlaylist = Playlist(videos = selectedIds.toList())
                            newPlaylist.updateThumbnail()?.let { newPlaylist = it }
                            val newId = playlistViewModel.insertAsync(newPlaylist).await()
                            withContext(Dispatchers.Main) {
                                showMultiChoiceDialog = false
                                onNavigateToVideoList(newId)
                                snackbarHostState.showSnackbar(msgVideosAdded)
                            }
                        } else {
                            val targetPlaylist = playlistViewModel
                                .getFromIdAsync(currentId)
                                .await()
                            if (targetPlaylist != null) {
                                val updatedVideos =
                                    (targetPlaylist.videos + selectedIds).distinct()
                                val updateResult = playlistViewModel.update(
                                    targetPlaylist.copy(videos = updatedVideos)
                                )
                                withContext(Dispatchers.Main) {
                                    showMultiChoiceDialog = false
                                    if (updateResult.isSuccess) {
                                        snackbarHostState.showSnackbar(msgVideosAdded)
                                    } else {
                                        snackbarHostState.showSnackbar(msgOperationFailed)
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showMultiChoiceDialog = false
                                    snackbarHostState.showSnackbar(msgOperationFailed)
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: android.database.sqlite.SQLiteException) {
                        Log.e(TAG, "Database error adding videos to playlist", e)
                        withContext(Dispatchers.Main) {
                            showMultiChoiceDialog = false
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Failed to add videos to playlist", e)
                        withContext(Dispatchers.Main) {
                            showMultiChoiceDialog = false
                            snackbarHostState.showSnackbar(msgOperationFailed)
                        }
                    }
                }
            },
            onDismiss = { showMultiChoiceDialog = false }
        )
    }
}

private const val TAG = "VideoListScreen"

@Preview(showBackground = true)
@Composable
private fun VideoListScreenPreview() {
    AppTheme {
        // ダミーデータを作成
        val dummyVideos = listOf(
            VideoUiModel.preview(
                id = 1L,
                videoId = "video1",
                title = "Morning Meditation"
            ),
            VideoUiModel.preview(
                id = 2L,
                videoId = "video2",
                title = "Workout Music Mix"
            ),
            VideoUiModel.preview(
                id = 3L,
                videoId = "video3",
                title = "Relaxing Sounds",
                domain = "soundcloud.com"
            )
        )

        VideoListScreenContent(
            playlistTitle = "My Playlist",
            isNewPlaylist = false,
            isAllVideosMode = false,
            videos = dummyVideos,
            playlistType = Playlist.Type.Original,
            orderRule = VideoOrder.TITLE,
            orderUp = true,
            selectedItems = emptyList(),
            isFabExpanded = false,
            isSyncing = false,
            expandedMenus = emptyMap(),
            onItemSelect = { _, _ -> },
            onItemClick = { },
            onMenuClick = { },
            onMenuDismiss = { },
            onSetThumbnail = { },
            onDownload = { },
            onReimport = { },
            onDeleteSingleVideo = { },
            onNavigateBack = { },
            onDeleteVideos = { },
            onSortRuleChange = { },
            onOrderUpToggle = { },
            onSyncRuleChange = { },
            onFabExpandToggle = { },
            onFabMainClick = { },
            onFabUrlClick = { },
            onFabMultiChoiceClick = { },
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}