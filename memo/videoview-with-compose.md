# VideoViewの扱い

## 概要

ComposeネイティブのVideoPlayerは存在しないため、AndroidViewでラップする必要があります。

## 実装パターン

```kotlin
@Composable
fun VideoViewComposable(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(videoUri)
                start()
            }
        },
        update = { videoView ->
            videoView.setVideoURI(videoUri)
        },
        modifier = modifier
    )
}
```

## ポイント

- `factory`: VideoViewのインスタンス作成
- `update`: URIが変更された時の更新処理
- `modifier`: サイズやレイアウト制約を指定

## 注意事項

- VideoViewはComposeの再コンポジションには直接反応しない
- `update`ブロックで明示的に更新する必要がある
- ライフサイクルイベントの管理が必要な場合は`DisposableEffect`を使用

## ライフサイクル管理の例

```kotlin
@Composable
fun VideoPlayerWithLifecycle(videoUri: Uri) {
    var videoView: VideoView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(videoUri)
                start()
                videoView = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

## 関連ドキュメント

- [AndroidView - Compose](https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose)
- [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)
