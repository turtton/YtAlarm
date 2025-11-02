package net.turtton.ytalarm.ui.compose.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.ui.compose.components.VideoItem
import net.turtton.ytalarm.ui.compose.components.VideoItemDropdownMenu
import net.turtton.ytalarm.ui.compose.dialogs.VideoReimportDialog
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.extensions.videoOrderRule
import net.turtton.ytalarm.util.extensions.videoOrderUp
import net.turtton.ytalarm.util.order.VideoOrder
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

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
    videos: List<net.turtton.ytalarm.database.structure.Video>,
    playlistType: Playlist.Type?,
    orderRule: VideoOrder,
    orderUp: Boolean,
    selectedItems: List<Long>,
    isFabExpanded: Boolean,
    expandedMenus: Map<Long, Boolean>,
    onItemSelect: (Long, Boolean) -> Unit,
    onItemClick: (String) -> Unit,
    onMenuClick: (net.turtton.ytalarm.database.structure.Video) -> Unit,
    onMenuDismiss: (Long) -> Unit,
    onSetThumbnail: (net.turtton.ytalarm.database.structure.Video) -> Unit,
    onDownload: (net.turtton.ytalarm.database.structure.Video) -> Unit,
    onReimport: (net.turtton.ytalarm.database.structure.Video) -> Unit,
    onDeleteSingleVideo: (net.turtton.ytalarm.database.structure.Video) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteVideos: () -> Unit,
    onSortRuleChange: (VideoOrder) -> Unit,
    onOrderUpToggle: () -> Unit,
    onSyncRuleChange: (Playlist.SyncRule) -> Unit,
    onFabExpandToggle: () -> Unit,
    onFabMainClick: () -> Unit,
    onFabUrlClick: () -> Unit,
    onFabMultiChoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                            Icon(Icons.Default.Sort, contentDescription = "Sort rule")
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
            // 全動画モードとImportingモードではFABを非表示
            if (!isImportingMode && !isAllVideosMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // Expanded状態のサブFAB
                    AnimatedVisibility(visible = isFabExpanded && isOriginalMode) {
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

                    // メインFAB
                    FloatingActionButton(
                        onClick = {
                            if (isOriginalMode) {
                                onFabExpandToggle()
                            } else {
                                onFabMainClick()
                            }
                        }
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isFabExpanded) 45f else 0f,
                            label = "fab_rotation"
                        )
                        Icon(
                            imageVector = if (isSyncMode) Icons.Default.Sync else Icons.Default.Add,
                            contentDescription = if (isSyncMode) "Sync" else "Add video",
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
                    modifier = Modifier.fillMaxSize()
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
                                onMenuClick(video)
                            },
                            onMenuDismiss = { onMenuDismiss(video.id) },
                            menuContent = {
                                VideoItemDropdownMenu(
                                    video = video,
                                    expanded = expandedMenus[video.id] ?: false,
                                    onDismiss = { onMenuDismiss(video.id) },
                                    onSetThumbnail = onSetThumbnail,
                                    onDownload = onDownload,
                                    onReimport = onReimport,
                                    onDelete = onDeleteSingleVideo
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
                        androidx.compose.material3.RadioButton(
                            selected = orderRule.ordinal == index,
                            onClick = {
                                onSortRuleChange(VideoOrder.values()[index])
                                showSortDialog = false
                            }
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
                    Text(stringResource(R.string.dialog_remove_video_positive))
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
                        androidx.compose.material3.RadioButton(
                            selected = currentRule?.ordinal == index,
                            onClick = {
                                onSyncRuleChange(Playlist.SyncRule.values()[index])
                                showSyncRuleDialog = false
                            }
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
    onShowUrlInputDialog: (Long) -> Unit,
    onShowMultiChoiceDialog: (Long) -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentId = remember { MutableStateFlow(playlistId) }
    // playlistId == 0の場合は全動画モード
    val isAllVideosMode = currentId.value == 0L
    val playlist by if (isAllVideosMode) {
        remember { mutableStateOf<Playlist?>(null) }
    } else {
        playlistViewModel.getFromId(currentId.value).observeAsState()
    }
    val selectedItems = remember { mutableStateListOf<Long>() }
    var isFabExpanded by remember { mutableStateOf(false) }

    // メニュー展開状態の管理
    val expandedMenus = remember { mutableStateMapOf<Long, Boolean>() }
    var videoToDelete by remember { mutableStateOf<net.turtton.ytalarm.database.structure.Video?>(null) }
    var videoToReimport by remember { mutableStateOf<net.turtton.ytalarm.database.structure.Video?>(null) }
    var videoForThumbnail by remember { mutableStateOf<net.turtton.ytalarm.database.structure.Video?>(null) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.videoOrderRule
    val orderUp = preferences.videoOrderUp

    // プレイリストのタイプを取得
    val playlistType = playlist?.type
    val isOriginalMode = playlistType is Playlist.Type.Original || playlistType == null
    val isSyncMode = playlistType is Playlist.Type.CloudPlaylist

    // 動画リストを取得
    // 全動画モード(playlistId=0)の場合は全動画を取得、それ以外はプレイリストの動画を取得
    val videos by if (isAllVideosMode) {
        videoViewModel.allVideos.observeAsState(emptyList())
    } else {
        val videoIds = playlist?.videos ?: emptyList()
        videoViewModel.getFromIds(videoIds).observeAsState(emptyList())
    }

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

    val playlistTitle = playlist?.title ?: stringResource(R.string.nav_video_list)
    // 全動画モードでは「新規プレイリスト」ではないので、常にfalse
    val isNewPlaylist = false

    // VideoListScreenContentを呼び出す
    VideoListScreenContent(
        playlistTitle = playlistTitle,
        isNewPlaylist = isNewPlaylist,
        isAllVideosMode = isAllVideosMode,
        videos = sortedVideos,
        playlistType = playlistType,
        orderRule = orderRule,
        orderUp = orderUp,
        selectedItems = selectedItems.toList(),
        isFabExpanded = isFabExpanded,
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
        onSetThumbnail = { video ->
            videoForThumbnail = video
            playlist?.let { pl ->
                playlistViewModel.update(
                    pl.copy(thumbnail = Playlist.Thumbnail.Video(video.id))
                )
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.message_thumbnail_set)
                )
            }
        },
        onDownload = { video ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.message_download_not_implemented)
                )
            }
        },
        onReimport = { video ->
            videoToReimport = video
            scope.launch(Dispatchers.IO) {
                try {
                    // キャンセルチェック
                    ensureActive()

                    // YoutubeDLで動画情報を再取得
                    val request = YoutubeDLRequest(video.videoUrl)
                        .addOption("--dump-single-json")
                        .addOption("-f", "b")
                    val result = YoutubeDL.getInstance().execute(request) { _, _, _ -> }

                    // 再度キャンセルチェック
                    ensureActive()

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
                        videoToReimport = null
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.message_reimport_success)
                        )
                    }
                } catch (e: CancellationException) {
                    // キャンセル例外はログに記録して再スロー
                    android.util.Log.d("VideoListScreen", "Reimport cancelled for video: ${video.videoId}")
                    throw e
                } catch (e: kotlinx.serialization.SerializationException) {
                    android.util.Log.e("VideoListScreen", "JSON parse error during reimport: ${video.videoId}", e)
                    withContext(Dispatchers.Main) {
                        videoToReimport = null
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.message_reimport_failed) + ": Parse error"
                        )
                    }
                } catch (e: java.net.UnknownHostException) {
                    android.util.Log.e("VideoListScreen", "Network error during reimport: ${video.videoId}", e)
                    withContext(Dispatchers.Main) {
                        videoToReimport = null
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.message_reimport_failed) + ": Network error"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoListScreen", "Reimport failed for video: ${video.videoId}", e)
                    withContext(Dispatchers.Main) {
                        videoToReimport = null
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.message_reimport_failed)
                        )
                    }
                }
            }
        },
        onDeleteSingleVideo = { video ->
            videoToDelete = video
        },
        onNavigateBack = onNavigateBack,
        onDeleteVideos = {
            scope.launch(Dispatchers.IO) {
                // 選択された動画をプレイリストから削除
                val currentPlaylist = playlistViewModel.getFromIdAsync(currentId.value).await()
                currentPlaylist?.let { pl ->
                    val updatedVideos = pl.videos.filter { !selectedItems.contains(it) }
                    playlistViewModel.update(pl.copy(videos = updatedVideos))
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
                val pl = playlistViewModel.getFromIdAsync(currentId.value).await()
                val type = pl?.type as? Playlist.Type.CloudPlaylist
                if (pl != null && type != null) {
                    val updatedType = type.copy(syncRule = rule)
                    playlistViewModel.update(pl.copy(type = updatedType))
                }
            }
        },
        onFabExpandToggle = {
            isFabExpanded = !isFabExpanded
        },
        onFabMainClick = {
            // Syncモード: 同期実行
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_sync_started)
                )
                // TODO: 同期処理の実装
            }
        },
        onFabUrlClick = {
            isFabExpanded = false
            onShowUrlInputDialog(currentId.value)
        },
        onFabMultiChoiceClick = {
            isFabExpanded = false
            onShowMultiChoiceDialog(currentId.value)
        },
        modifier = modifier
    )

    // 削除確認ダイアログ
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_video_title)) },
            text = { Text(stringResource(R.string.dialog_delete_video_message, video.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoViewModel.delete(video)
                        videoToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.message_video_deleted)
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_remove_video_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text(stringResource(R.string.dialog_remove_video_negative))
                }
            }
        )
    }

    // 再インポートダイアログ
    videoToReimport?.let { video ->
        VideoReimportDialog(
            video = video,
            onConfirm = {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.message_reimport_started)
                    )
                }
                videoToReimport = null
            },
            onDismiss = { videoToReimport = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VideoListScreenPreview() {
    AppTheme {
        // ダミーデータを作成
        val dummyVideos = listOf(
            net.turtton.ytalarm.database.structure.Video(
                id = 1L,
                videoId = "video1",
                title = "Morning Meditation",
                domain = "youtube.com",
                stateData = net.turtton.ytalarm.database.structure.Video.State.Information(
                    isStreamable = true
                ),
                creationDate = java.util.Calendar.getInstance()
            ),
            net.turtton.ytalarm.database.structure.Video(
                id = 2L,
                videoId = "video2",
                title = "Workout Music Mix",
                domain = "youtube.com",
                stateData = net.turtton.ytalarm.database.structure.Video.State.Information(
                    isStreamable = true
                ),
                creationDate = java.util.Calendar.getInstance()
            ),
            net.turtton.ytalarm.database.structure.Video(
                id = 3L,
                videoId = "video3",
                title = "Relaxing Sounds",
                domain = "soundcloud.com",
                stateData = net.turtton.ytalarm.database.structure.Video.State.Information(
                    isStreamable = true
                ),
                creationDate = java.util.Calendar.getInstance()
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
            onFabMultiChoiceClick = { }
        )
    }
}