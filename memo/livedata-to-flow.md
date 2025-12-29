# LiveDataとFlow

## LiveDataの使用

```kotlin
val alarms by viewModel.allAlarms.observeAsState(initial = emptyList())
```

## Flowの使用（推奨）

```kotlin
val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
```

## 移行推奨理由

1. **非同期処理に最適**: FlowはCoroutinesと完全に統合
2. **バックプレッシャー対応**: データの流量制御が可能
3. **演算子が豊富**: map, filter, combine等が使いやすい
4. **Composeと相性が良い**: collectAsStateが自然

## 移行パターン

### DAO層

```kotlin
// Before (LiveData)
@Query("SELECT * FROM alarm")
fun getAll(): LiveData<List<Alarm>>

// After (Flow)
@Query("SELECT * FROM alarm")
fun getAll(): Flow<List<Alarm>>
```

### ViewModel層

```kotlin
// Before (LiveData)
val allAlarms: LiveData<List<Alarm>> = repository.allAlarms

// After (Flow)
val allAlarms: Flow<List<Alarm>> = repository.allAlarms

// StateFlowに変換（推奨）
val allAlarms: StateFlow<List<Alarm>> = repository.allAlarms
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

## StateFlow vs Flow

### Flow
- Cold stream（購読されるまで実行されない）
- 初期値なし
- 複数のコレクターで独立した値を受け取る

### StateFlow
- Hot stream（常に最新の値を保持）
- 初期値が必須
- 複数のコレクターで同じ値を共有
- **Composeでの使用に推奨**

## Flowの便利な演算子

### map

```kotlin
val alarmTitles: Flow<List<String>> = allAlarms.map { alarms ->
    alarms.map { "${it.hour}:${it.minute}" }
}
```

### filter

```kotlin
val enabledAlarms: Flow<List<Alarm>> = allAlarms.map { alarms ->
    alarms.filter { it.isEnable }
}
```

### combine

```kotlin
val uiState: Flow<UiState> = combine(
    alarms,
    playlists,
    videos
) { alarms, playlists, videos ->
    UiState(alarms, playlists, videos)
}
```

### debounce

```kotlin
val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

val searchResults: Flow<List<Video>> = searchQuery
    .debounce(300)  // 300ms待機
    .flatMapLatest { query ->
        repository.search(query)
    }
```

## Compose内での使用

### collectAsState

```kotlin
@Composable
fun AlarmListScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

    LazyColumn {
        items(alarms) { alarm ->
            AlarmItem(alarm)
        }
    }
}
```

### collectAsStateWithLifecycle（推奨）

ライフサイクルに応じて自動的にコレクションを停止/再開：

```kotlin
@Composable
fun AlarmListScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.allAlarms.collectAsStateWithLifecycle()

    LazyColumn {
        items(alarms) { alarm ->
            AlarmItem(alarm)
        }
    }
}
```

必要な依存関係：
```toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "..." }
```

## 関連ドキュメント

- [Kotlin Flows](https://kotlinlang.org/docs/flow.html)
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Collect flows in Compose](https://developer.android.com/jetpack/compose/side-effects#collectAsStateWithLifecycle)
