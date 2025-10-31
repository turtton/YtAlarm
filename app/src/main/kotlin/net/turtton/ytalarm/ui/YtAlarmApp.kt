package net.turtton.ytalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.turtton.ytalarm.navigation.YtAlarmDestination
import net.turtton.ytalarm.navigation.YtAlarmNavGraph
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * YtAlarmアプリのルートComposable
 *
 * テーマ適用とNavigation設定を行う。
 *
 * @param modifier Composableに適用するModifier
 * @param navController ナビゲーションコントローラー (テスト用にオーバーライド可能)
 * @param onOpenDrawer ドロワーを開くコールバック
 * @param startDestination 初期画面のルート (テスト用にオーバーライド可能)
 */
@Composable
fun YtAlarmApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onOpenDrawer: () -> Unit = {},
    startDestination: String = YtAlarmDestination.ALARM_LIST
) {
    AppTheme {
        YtAlarmNavGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            modifier = modifier,
            startDestination = startDestination
        )
    }
}
