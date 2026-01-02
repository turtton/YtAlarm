package net.turtton.ytalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.turtton.ytalarm.BuildConfig
import net.turtton.ytalarm.R
import net.turtton.ytalarm.idling.VideoPlayerLoadingResourceContainer
import net.turtton.ytalarm.navigation.YtAlarmDestination
import net.turtton.ytalarm.ui.compose.screens.PermissionScreen
import net.turtton.ytalarm.ui.compose.screens.hasMissingPermissions
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModel

/**
 * YtAlarmアプリのメイン画面
 *
 * ModalNavigationDrawerを含み、全体のナビゲーション構造を管理する。
 *
 * @param playlistViewModel プレイリスト管理用ViewModel
 * @param videoViewModel 動画管理用ViewModel
 * @param videoPlayerResourceContainer 動画プレーヤーリソース提供コンテナ
 */
@Composable
fun MainScreen(
    playlistViewModel: PlaylistViewModel,
    videoViewModel: VideoViewModel,
    videoPlayerResourceContainer: VideoPlayerLoadingResourceContainer
) {
    val context = LocalContext.current

    // 権限画面の表示状態（デバッグビルドではスキップ）
    var showPermissionScreen by remember {
        mutableStateOf(!BuildConfig.DEBUG && hasMissingPermissions(context))
    }

    // 権限画面を表示
    if (showPermissionScreen) {
        PermissionScreen(
            onContinue = {
                showPermissionScreen = false
            }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // 現在のルートを取得（Drawer選択状態の管理用）
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CompositionLocalProvider(
        LocalPlaylistViewModel provides playlistViewModel,
        LocalVideoViewModel provides videoViewModel,
        LocalVideoPlayerResourceContainer provides videoPlayerResourceContainer
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerHeader()
                    HorizontalDivider()
                    DrawerContent(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            // 現在のルートを取得
                            val current = navController.currentBackStackEntry?.destination?.route
                            // ルートが異なる場合のみナビゲート
                            if (current != route) {
                                // Drawerを非同期で閉じる（完了を待たない）
                                scope.launch {
                                    drawerState.close()
                                }
                                // 即座にナビゲーションを実行（タイムラグ解消）
                                navController.navigate(route) {
                                    // スタート画面まで戻り、状態を保存/復元してナビゲート
                                    popUpTo(YtAlarmDestination.ALARM_LIST) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                // 同じルートの場合はDrawerを閉じるのみ
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        }
                    )
                }
            }
        ) {
            YtAlarmApp(
                navController = navController,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )
        }
    }
}

/**
 * Drawerのヘッダー部分
 *
 * アプリ名を表示する。
 */
@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Drawerのコンテンツ（メニュー項目）
 *
 * @param currentRoute 現在のルート（選択状態の判定用）
 * @param onNavigate ナビゲーションコールバック
 */
@Composable
private fun DrawerContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // アラームリスト
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.menu_item_aram_list)) },
            selected = currentRoute == YtAlarmDestination.ALARM_LIST,
            onClick = { onNavigate(YtAlarmDestination.ALARM_LIST) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // プレイリスト
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.menu_title_playlist)) },
            selected = currentRoute == YtAlarmDestination.PLAYLIST,
            onClick = { onNavigate(YtAlarmDestination.PLAYLIST) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // ビデオリスト（全動画一覧）
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.menu_title_video_list)) },
            selected = currentRoute == YtAlarmDestination.ALL_VIDEOS,
            onClick = { onNavigate(YtAlarmDestination.ALL_VIDEOS) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Settings
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.menu_title_settings)) },
            selected = currentRoute == YtAlarmDestination.SETTINGS,
            onClick = { onNavigate(YtAlarmDestination.SETTINGS) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // About
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.menu_title_aboutpage)) },
            selected = currentRoute == YtAlarmDestination.ABOUT,
            onClick = { onNavigate(YtAlarmDestination.ABOUT) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}