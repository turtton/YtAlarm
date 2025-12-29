# Video型サムネイルがプレースホルダーのまま

## 問題

`Playlist.Thumbnail.Video`型のサムネイルで、Video IDからサムネイルURLを取得する処理が未実装のため、常にプレースホルダー（ic_no_image）が表示される。

## 原因

Video IDからVideoエンティティを取得し、thumbnailUrlを非同期で取得する処理が必要だが未実装。

## 解決策

`LaunchedEffect`でVideo IDから非同期でサムネイルURLを取得：

```kotlin
@Composable
fun PlaylistScreen(
    playlist: Playlist,
    videoViewModel: VideoViewModel
) {
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlist.thumbnail) {
        when (val thumb = playlist.thumbnail) {
            is Playlist.Thumbnail.Video -> {
                val video = videoViewModel.getFromIdAsync(thumb.videoId).await()
                thumbnailUrl = video?.thumbnailUrl
            }
            is Playlist.Thumbnail.Drawable -> {
                thumbnailUrl = null // ImageRequestが対応
            }
            is Playlist.Thumbnail.Url -> {
                thumbnailUrl = thumb.url
            }
        }
    }

    PlaylistItem(thumbnailUrl = thumbnailUrl ?: R.drawable.ic_no_image)
}
```

## ポイント

- `LaunchedEffect(key)`で非同期処理を実行
- `remember`でサムネイルURL状態を保持
- VideoViewModelを引数として受け取る必要がある

## 関連ファイル

- `AlarmListScreen.kt`
- `PlaylistScreen.kt`
- `AlarmSettingsScreen.kt`

## 関連ドキュメント

- [LaunchedEffect - Compose Side-effects](https://developer.android.com/jetpack/compose/side-effects#launchedeffect)
- [remember - Compose State](https://developer.android.com/jetpack/compose/state#remember)
