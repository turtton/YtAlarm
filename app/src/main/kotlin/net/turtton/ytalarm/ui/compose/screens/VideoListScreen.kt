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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
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
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.components.VideoItem
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
    onShowUrlInputDialog: (Long) -> Unit,
    onShowMultiChoiceDialog: (Long) -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    ),
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentId = remember { MutableStateFlow(playlistId) }
    val playlist by playlistViewModel.getFromId(currentId.value).observeAsState()
    val selectedItems = remember { mutableStateListOf<Long>() }

    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSyncRuleDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.videoOrderRule
    val orderUp = preferences.videoOrderUp

    // プレイリストのタイプを取得
    val playlistType = playlist?.type
    val isOriginalMode = playlistType is Playlist.Type.Original || playlistType == null
    val isSyncMode = playlistType is Playlist.Type.CloudPlaylist
    val isImportingMode = playlistType is Playlist.Type.Importing

    // 動画リストを取得
    val videoIds = playlist?.videos ?: emptyList()
    val videos by videoViewModel.getFromIds(videoIds).observeAsState(emptyList())

    // ソート処理
    val sortedVideos = remember(videos, orderRule, orderUp) {
        val mutableList: MutableList<net.turtton.ytalarm.database.structure.Video> = videos.toMutableList()
        when (orderRule) {
            VideoOrder.TITLE -> mutableList.sortBy { it.title }
            VideoOrder.CREATION_DATE -> mutableList.sortBy { it.creationDate.timeInMillis }
        }
        if (!orderUp) {
            mutableList.reverse()
        }
        mutableList
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        playlist?.title ?: stringResource(
                            if (currentId.value == 0L) R.string.playlist_new_title
                            else R.string.nav_video_list
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 選択時の削除ボタン
                    if (selectedItems.isNotEmpty() && !isSyncMode) {
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
                        IconButton(onClick = {
                            preferences.videoOrderUp = !orderUp
                        }) {
                            Icon(
                                imageVector = if (orderUp) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (orderUp) "Sort ascending" else "Sort descending"
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
            if (!isImportingMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // Expanded状態のサブFAB
                    AnimatedVisibility(visible = isFabExpanded && isOriginalMode) {
                        Column {
                            // URLから追加
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onShowUrlInputDialog(currentId.value)
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Add from URL")
                            }
                            // 既存動画から追加
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onShowMultiChoiceDialog(currentId.value)
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Add from videos")
                            }
                        }
                    }

                    // メインFAB
                    FloatingActionButton(
                        onClick = {
                            when {
                                isSyncMode -> {
                                    // Syncモード: 同期実行
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.snackbar_sync_started)
                                        )
                                        // TODO: 同期処理の実装
                                    }
                                }
                                isOriginalMode -> {
                                    // Originalモード: FABを展開
                                    isFabExpanded = !isFabExpanded
                                }
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
            if (sortedVideos.isEmpty()) {
                Text(
                    text = stringResource(
                        if (currentId.value == 0L) R.string.video_list_empty_new_message
                        else R.string.video_list_empty_message
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedVideos,
                        key = { it.id }
                    ) { video ->
                        VideoItem(
                            video = video,
                            domainOrSize = video.domain,
                            isSelected = selectedItems.contains(video.id),
                            showCheckbox = selectedItems.isNotEmpty(),
                            onToggleSelection = {
                                if (selectedItems.contains(video.id)) {
                                    selectedItems.remove(video.id)
                                } else {
                                    selectedItems.add(video.id)
                                }
                            },
                            onClick = {
                                if (selectedItems.isEmpty()) {
                                    // TODO: 動画プレーヤーへ遷移
                                } else {
                                    if (selectedItems.contains(video.id)) {
                                        selectedItems.remove(video.id)
                                    } else {
                                        selectedItems.add(video.id)
                                    }
                                }
                            },
                            onMenuClick = {
                                // 個別メニューアクション（今後実装）
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
                                preferences.videoOrderRule = VideoOrder.values()[index]
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
                        scope.launch(Dispatchers.IO) {
                            // 選択された動画をプレイリストから削除
                            val currentPlaylist = playlistViewModel.getFromIdAsync(currentId.value).await()
                            currentPlaylist?.let { pl ->
                                val updatedVideos = pl.videos.filter { !selectedItems.contains(it) }
                                playlistViewModel.update(pl.copy(videos = updatedVideos))
                            }

                            withContext(Dispatchers.Main) {
                                selectedItems.clear()
                                showDeleteDialog = false
                            }
                        }
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
                                scope.launch(Dispatchers.IO) {
                                    val pl = playlistViewModel.getFromIdAsync(currentId.value).await()
                                    val type = pl?.type as? Playlist.Type.CloudPlaylist
                                    if (pl != null && type != null) {
                                        val newRule = Playlist.SyncRule.values()[index]
                                        val updatedType = type.copy(syncRule = newRule)
                                        playlistViewModel.update(pl.copy(type = updatedType))
                                    }
                                }
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

@Preview(showBackground = true)
@Composable
fun VideoListScreenPreview() {
    AppTheme {
        // Preview用のダミーデータは省略
    }
}
