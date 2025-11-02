# Navigation Arguments

## 概要

型安全なナビゲーションのため、Navigation 2.8.0+ではKotlin Serializationの使用を推奨。

## String-based Routes（現在の実装）

Navigation 2.7.7で使用可能な従来の方式。

```kotlin
object YtAlarmDestination {
    const val ALARM_LIST = "alarm_list"
    const val ALARM_SETTINGS = "alarm_settings"

    fun alarmSettings(alarmId: Long) = "$ALARM_SETTINGS/$alarmId"
}

// Navigation
navController.navigate(YtAlarmDestination.alarmSettings(123))

// Route定義
composable(
    route = "alarm_settings/{alarmId}",
    arguments = listOf(
        navArgument("alarmId") { type = NavType.LongType }
    )
) { backStackEntry ->
    val alarmId = backStackEntry.arguments?.getLong("alarmId")
    AlarmSettingsScreen(alarmId = alarmId)
}
```

## 型安全なRoutes（Navigation 2.8.0+で推奨）

kotlinx.serializationを使用した型安全な方式。

```kotlin
@Serializable
data class AlarmSettingsRoute(val alarmId: Long)

// Navigation
navController.navigate(AlarmSettingsRoute(alarmId = 123))

// Route定義
composable<AlarmSettingsRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<AlarmSettingsRoute>()
    AlarmSettingsScreen(alarmId = route.alarmId)
}
```

## 現在のプロジェクトの方針

- **Navigation 2.7.7**: String-based routesを使用（現状）
- **将来的に2.8.0+へ移行**: kotlinx.serializationベースの型安全なroutesに更新予定

## Optional Argumentsの扱い

```kotlin
// String-based
composable(
    route = "video_list/{playlistId}",
    arguments = listOf(
        navArgument("playlistId") {
            type = NavType.LongType
            defaultValue = 0L  // Optional
        }
    )
) { backStackEntry ->
    val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
    VideoListScreen(playlistId = playlistId)
}

// 型安全
@Serializable
data class VideoListRoute(val playlistId: Long = 0L)
```

## 複数の引数

```kotlin
// String-based
fun videoPlayer(videoId: String, isAlarmMode: Boolean) =
    "video_player/$videoId/$isAlarmMode"

// 型安全
@Serializable
data class VideoPlayerRoute(
    val videoId: String,
    val isAlarmMode: Boolean
)
```

## 関連ドキュメント

- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Type Safety in Navigation](https://developer.android.com/guide/navigation/design/type-safety)
