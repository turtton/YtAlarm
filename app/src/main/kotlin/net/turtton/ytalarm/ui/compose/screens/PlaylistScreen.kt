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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateMapOf
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
import net.turtton.ytalarm.ui.compose.components.PlaylistItemDropdownMenu
import net.turtton.ytalarm.ui.compose.dialogs.RenamePlaylistDialog
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
 * プレイリスト一覧画面のコンテンツ（プレビュー可能）
 *
 * ViewModelに依存せず、すべてのデータと関数を引数として受け取る純粋なComposable。
 * これにより、@Previewアノテーションでプレビュー可能になる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreenContent(
    playlists: List<Playlist>,
    orderRule: PlaylistOrder,
    orderUp: Boolean,
    selectedItems: List<Long>,
    expandedMenus: Map<Long, Boolean>,
    onItemSelect: (Long, Boolean) -> Unit,
    onItemClick: (Long) -> Unit,
    onMenuClick: (Playlist) -> Unit,
    onMenuDismiss: (Long) -> Unit,
    onRename: (Playlist) -> Unit,
    onOpenDrawer: () -> Unit,
    onDeletePlaylists: () -> Unit,
    onSortRuleChange: (PlaylistOrder) -> Unit,
    onOrderUpToggle: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            FloatingActionButton(
                onClick = onCreatePlaylist
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
            if (playlists.isEmpty()) {
                Text(
                    text = stringResource(R.string.playlist_empty_message),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = playlists,
                        key = { it.id }
                    ) { playlist ->
                        // サムネイル取得
                        val thumbnailUrl = remember(playlist.thumbnail) {
                            mutableStateOf<Any?>(null)
                        }

                        playlist.thumbnail?.let { thumbnail ->
                            when (thumbnail) {
                                is Playlist.Thumbnail.Video -> {
                                    // Video thumbnailの場合、VideoからURLを取得
                                    LaunchedEffect(thumbnail.id) {
                                        videoViewModel?.getFromIdAsync(
                                            thumbnail.id
                                        )?.let { deferred ->
                                            val video = withContext(Dispatchers.IO) {
                                                deferred.await()
                                            }
                                            thumbnailUrl.value = video?.thumbnailUrl
                                        }
                                    }
                                }
                                is Playlist.Thumbnail.Drawable -> {
                                    thumbnailUrl.value = thumbnail.id
                                }
                            }
                        }
                        val videoCount = playlist.videos.size

                        PlaylistItem(
                            playlist = playlist,
                            thumbnailUrl = thumbnailUrl.value,
                            videoCount = videoCount,
                            isSelected = selectedItems.contains(playlist.id),
                            onToggleSelection = {
                                onItemSelect(playlist.id, !selectedItems.contains(playlist.id))
                            },
                            onClick = {
                                if (selectedItems.isEmpty()) {
                                    onItemClick(playlist.id)
                                } else {
                                    onItemSelect(playlist.id, !selectedItems.contains(playlist.id))
                                }
                            },
                            menuExpanded = expandedMenus[playlist.id] ?: false,
                            onMenuClick = {
                                onMenuClick(playlist)
                            },
                            onMenuDismiss = { onMenuDismiss(playlist.id) },
                            menuContent = {
                                PlaylistItemDropdownMenu(
                                    playlist = playlist,
                                    expanded = expandedMenus[playlist.id] ?: false,
                                    onDismiss = { onMenuDismiss(playlist.id) },
                                    onRename = onRename
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
                                onSortRuleChange(PlaylistOrder.values()[index])
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
                        onDeletePlaylists()
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
}

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
        factory = PlaylistViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    ),
    videoViewModel: VideoViewModel = viewModel(
        factory = VideoViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    ),
    alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(
            (LocalContext.current.applicationContext as YtApplication).repository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlists by playlistViewModel.allPlaylists.observeAsState(emptyList())
    val selectedItems = remember { mutableStateListOf<Long>() }

    // メニュー展開状態の管理
    val expandedMenus = remember { mutableStateMapOf<Long, Boolean>() }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    val activity = context.findActivity() ?: return
    val preferences = activity.privatePreferences
    val orderRule = preferences.playlistOrderRule
    val orderUp = preferences.playlistOrderUp

    // ガベージコレクション（Importing状態で終了したプレイリストを削除）
    LaunchedEffect(playlists) {
        scope.launch(Dispatchers.IO) {
            try {
                // 最新のplaylistsを取得
                val currentPlaylists = playlistViewModel.allPlaylistsAsync.await()
                val garbage = currentPlaylists.filter { playlist ->
                    if (playlist.type !is Playlist.Type.Importing) return@filter false
                    val videoId = playlist.videos.firstOrNull() ?: return@filter false
                    val video = videoViewModel.getFromIdAsync(
                        videoId
                    ).await() ?: return@filter false
                    val state = when (val stateData = video.stateData) {
                        is Video.State.Importing -> stateData.state as? Video.WorkerState.Working
                        is Video.State.Downloading -> stateData.state as? Video.WorkerState.Working
                        else -> return@filter false
                    } ?: return@filter false

                    val workManager = WorkManager.getInstance(context)
                    try {
                        val workerState = workManager.getWorkInfoById(state.workerId).get()?.state
                        workerState == null || workerState.isFinished
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "PlaylistScreen",
                            "Failed to check worker state: ${state.workerId}",
                            e
                        )
                        true // ワーカー情報取得失敗時はガベージコレクション対象とする
                    }
                }
                if (garbage.isNotEmpty()) {
                    playlistViewModel.delete(garbage)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // キャンセル例外は再スロー
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlaylistScreen", "Garbage collection failed", e)
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

    // PlaylistScreenContentを呼び出す
    PlaylistScreenContent(
        playlists = sortedPlaylists,
        orderRule = orderRule,
        orderUp = orderUp,
        selectedItems = selectedItems.toList(),
        expandedMenus = expandedMenus,
        onItemSelect = { id, isSelected ->
            if (isSelected) {
                selectedItems.add(id)
            } else {
                selectedItems.remove(id)
            }
        },
        onItemClick = onNavigateToVideoList,
        onMenuClick = { playlist ->
            expandedMenus[playlist.id] = true
        },
        onMenuDismiss = { playlistId ->
            expandedMenus.remove(playlistId)
        },
        onRename = { playlist ->
            playlistToRename = playlist
        },
        onOpenDrawer = onOpenDrawer,
        onDeletePlaylists = {
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
                }
            }
        },
        onSortRuleChange = { rule ->
            preferences.playlistOrderRule = rule
        },
        onOrderUpToggle = {
            preferences.playlistOrderUp = !orderUp
        },
        onCreatePlaylist = {
            onNavigateToVideoList(0L)
        },
        modifier = modifier,
        videoViewModel = videoViewModel
    )

    // リネームダイアログ
    playlistToRename?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onConfirm = { newName ->
                playlistViewModel.update(playlist.copy(title = newName))
                playlistToRename = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.message_playlist_renamed)
                    )
                }
            },
            onDismiss = { playlistToRename = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    AppTheme {
        // ダミーデータを作成
        val dummyPlaylists = listOf(
            Playlist(
                id = 1L,
                title = "Morning Playlist",
                videos = listOf(1L, 2L, 3L),
                thumbnail = Playlist.Thumbnail.Drawable(R.drawable.ic_no_image),
                type = Playlist.Type.Original,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            ),
            Playlist(
                id = 2L,
                title = "Workout Music",
                videos = listOf(4L, 5L),
                thumbnail = Playlist.Thumbnail.Drawable(R.drawable.ic_no_image),
                type = Playlist.Type.Original,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            ),
            Playlist(
                id = 3L,
                title = "Relaxing Sounds",
                videos = listOf(6L, 7L, 8L, 9L),
                thumbnail = Playlist.Thumbnail.Drawable(R.drawable.ic_no_image),
                type = Playlist.Type.Original,
                creationDate = java.util.Calendar.getInstance(),
                lastUpdated = java.util.Calendar.getInstance()
            )
        )

        PlaylistScreenContent(
            playlists = dummyPlaylists,
            orderRule = PlaylistOrder.TITLE,
            orderUp = true,
            selectedItems = emptyList(),
            expandedMenus = emptyMap(),
            onItemSelect = { _, _ -> },
            onItemClick = { },
            onMenuClick = { },
            onMenuDismiss = { },
            onRename = { },
            onOpenDrawer = { },
            onDeletePlaylists = { },
            onSortRuleChange = { },
            onOrderUpToggle = { },
            onCreatePlaylist = { }
        )
    }
}