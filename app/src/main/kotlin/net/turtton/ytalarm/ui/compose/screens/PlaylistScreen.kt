package net.turtton.ytalarm.ui.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.compose.components.PlaylistItem
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.findActivity
import net.turtton.ytalarm.util.extensions.playlistOrderRule
import net.turtton.ytalarm.util.extensions.playlistOrderUp
import net.turtton.ytalarm.util.extensions.privatePreferences
import net.turtton.ytalarm.util.order.PlaylistOrder
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

/**
 * プレイリスト一覧画面（Compose版）
 *
 * 機能:
 * - プレイリスト一覧表示（LazyColumn）
 * - 複数選択機能
 * - ソート機能（タイトル、作成日、最終更新日）
 * - 並び替え（昇順/降順）
 * - 削除機能（アラームで使用中のプレイリストは削除不可）
 * - 新規プレイリスト作成（FAB）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateToVideoList: (playlistId: Long) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    ),
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    ),
    alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory((LocalContext.current.applicationContext as YtApplication).repository)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlists by playlistViewModel.allPlaylists.observeAsState(emptyList())
    val selectedItems = remember { mutableStateListOf<Long>() }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.playlistOrderRule
    val orderUp = preferences.playlistOrderUp

    // ガベージコレクション（Importing状態で終了したプレイリストを削除）
    LaunchedEffect(playlists) {
        scope.launch(Dispatchers.IO) {
            val garbage = playlists.filter { playlist ->
                if (playlist.type !is Playlist.Type.Importing) return@filter false
                val videoId = playlist.videos.firstOrNull() ?: return@filter false
                val video = videoViewModel.getFromIdAsync(videoId).await() ?: return@filter false
                val state = when (val stateData = video.stateData) {
                    is Video.State.Importing -> stateData.state as? Video.WorkerState.Working
                    is Video.State.Downloading -> stateData.state as? Video.WorkerState.Working
                    else -> return@filter false
                } ?: return@filter false
                val workManager = WorkManager.getInstance(context)
                val workerState = workManager.getWorkInfoById(state.workerId).get()?.state
                workerState == null || workerState.isFinished
            }
            if (garbage.isNotEmpty()) {
                playlistViewModel.delete(garbage)
            }
        }
    }

    // ソート処理
    val sortedPlaylists = remember(playlists, orderRule, orderUp) {
        val mutableList: MutableList<Playlist> = playlists.toMutableList()
        when (orderRule) {
            PlaylistOrder.TITLE -> mutableList.sortBy { it.title }
            PlaylistOrder.CREATION_DATE -> mutableList.sortBy { it.creationDate }
            PlaylistOrder.LAST_UPDATED -> mutableList.sortBy { it.lastUpdated }
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
                title = { Text(stringResource(R.string.nav_playlist)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // 選択時の削除ボタン
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    // 並び替えボタン
                    IconButton(onClick = {
                        preferences.playlistOrderUp = !orderUp
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToVideoList(0L) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add playlist")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (sortedPlaylists.isEmpty()) {
                Text(
                    text = stringResource(R.string.playlist_empty_message),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedPlaylists,
                        key = { it.id }
                    ) { playlist ->
                        val thumbnailUrl = playlist.thumbnail?.let { thumbnail ->
                            when (thumbnail) {
                                is Playlist.Thumbnail.Video -> {
                                    // TODO: Load video thumbnail by ID
                                    null
                                }
                                is Playlist.Thumbnail.Drawable -> {
                                    thumbnail.id
                                }
                            }
                        }
                        val videoCount = playlist.videos.size

                        PlaylistItem(
                            playlist = playlist,
                            thumbnailUrl = thumbnailUrl,
                            videoCount = videoCount,
                            isSelected = selectedItems.contains(playlist.id),
                            onToggleSelection = {
                                if (selectedItems.contains(playlist.id)) {
                                    selectedItems.remove(playlist.id)
                                } else {
                                    selectedItems.add(playlist.id)
                                }
                            },
                            onClick = {
                                if (selectedItems.isEmpty()) {
                                    onNavigateToVideoList(playlist.id)
                                } else {
                                    if (selectedItems.contains(playlist.id)) {
                                        selectedItems.remove(playlist.id)
                                    } else {
                                        selectedItems.add(playlist.id)
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
        val sortOptions = stringArrayResource(R.array.dialog_playlist_order)
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.menu_playlist_option_sortrule)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    sortOptions.forEachIndexed { index, option ->
                        androidx.compose.material3.RadioButton(
                            selected = orderRule.ordinal == index,
                            onClick = {
                                preferences.playlistOrderRule = PlaylistOrder.values()[index]
                                showSortDialog = false
                            }
                        )
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                        scope.launch(Dispatchers.IO) {
                            val alarmsAsync = alarmViewModel.getAllAlarmsAsync()
                            val playlistsAsync = playlistViewModel.getFromIdsAsync(selectedItems.toList())

                            val usingList = alarmsAsync.await().flatMap { it.playListId }.distinct()
                            val deletable = arrayListOf<Playlist>()
                            var detectUsage = false

                            playlistsAsync.await().forEach { playlist ->
                                if (!usingList.contains(playlist.id)) {
                                    deletable += playlist
                                } else {
                                    detectUsage = true
                                }
                            }

                            if (deletable.isNotEmpty()) {
                                playlistViewModel.delete(deletable)
                            }

                            if (detectUsage) {
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.snackbar_detect_playlist_usage)
                                    )
                                }
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
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    AppTheme {
        // Preview用のダミーデータは省略
    }
}
