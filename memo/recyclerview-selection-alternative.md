# RecyclerView Selection APIの代替

## 概要

Composeには直接の代替がないため、自前で実装します。

## 実装パターン

```kotlin
@Composable
fun <T> LazyListWithSelection(
    items: List<T>,
    selectedIds: Set<Long>,
    onSelectionChange: (Long, Boolean) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    LazyColumn {
        items(items, key = { /* key */ }) { item ->
            val isSelected = selectedIds.contains(item.id)
            itemContent(item, isSelected)
        }
    }
}
```

## 状態管理

```kotlin
@Composable
fun PlaylistScreen() {
    var selectedItems by remember { mutableStateOf(emptySet<Long>()) }

    // 選択/解除
    fun toggleSelection(id: Long) {
        selectedItems = if (id in selectedItems) {
            selectedItems - id
        } else {
            selectedItems + id
        }
    }

    // UI
    LazyColumn {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistItem(
                playlist = playlist,
                isSelected = playlist.id in selectedItems,
                onToggleSelection = { toggleSelection(playlist.id) }
            )
        }
    }
}
```

## ポイント

- `Set<Long>`で選択状態を管理
- `remember`でCompose内で状態を保持
- 長押しやモード切り替えは独自に実装

## 複数選択モードの実装例

```kotlin
@Composable
fun PlaylistScreenWithSelectionMode() {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(emptySet<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectionMode) "${selectedItems.size} selected" else "Playlists")
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { /* 削除 */ }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    isSelected = playlist.id in selectedItems,
                    onLongClick = { isSelectionMode = true },
                    onClick = {
                        if (isSelectionMode) {
                            toggleSelection(playlist.id)
                        } else {
                            // 通常のクリック処理
                        }
                    }
                )
            }
        }
    }
}
```

## 関連ドキュメント

- [State in Compose](https://developer.android.com/jetpack/compose/state)
- [Lists and grids](https://developer.android.com/jetpack/compose/lists)
