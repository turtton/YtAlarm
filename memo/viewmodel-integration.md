# ViewModel統合

## 概要

既存のViewModelはそのまま使用可能です。

## 基本パターン

```kotlin
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(
            LocalContext.current.applicationContext.repository
        )
    )
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

    // UI
    LazyColumn {
        items(alarms, key = { it.id }) { alarm ->
            AlarmItem(alarm = alarm)
        }
    }
}
```

## ViewModelスコープ

### Activity スコープ

```kotlin
val viewModel: AlarmViewModel = viewModel(LocalContext.current as ComponentActivity)
```

### Navigation グラフスコープ

```kotlin
val viewModel: AlarmViewModel = viewModel(navController.getBackStackEntry("alarm_list"))
```

### 親Composableスコープ

```kotlin
@Composable
fun ParentScreen(viewModel: AlarmViewModel = viewModel()) {
    // このViewModelは子Composableで共有される
    ChildScreen1(viewModel)
    ChildScreen2(viewModel)
}
```

## 依存性注入

CompositionLocalを使用してViewModelをアプリ全体で共有：

```kotlin
val LocalAlarmViewModel = compositionLocalOf<AlarmViewModel> {
    error("No AlarmViewModel provided")
}

// 提供
@Composable
fun App() {
    val alarmViewModel = viewModel<AlarmViewModel>(
        factory = AlarmViewModelFactory(repository)
    )

    CompositionLocalProvider(
        LocalAlarmViewModel provides alarmViewModel
    ) {
        YtAlarmApp()
    }
}

// 使用
@Composable
fun SomeScreen() {
    val viewModel = LocalAlarmViewModel.current
}
```

## ViewModel Factoryの実装

```kotlin
class AlarmViewModelFactory(
    private val repository: DataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

## Hiltを使用する場合

```kotlin
@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {
    // ...
}

@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel = hiltViewModel()
) {
    // ...
}
```

## 関連ドキュメント

- [ViewModel in Compose](https://developer.android.com/jetpack/compose/libraries#viewmodel)
- [CompositionLocal](https://developer.android.com/jetpack/compose/compositionlocal)
- [Hilt and Jetpack integrations](https://developer.android.com/training/dependency-injection/hilt-jetpack)
