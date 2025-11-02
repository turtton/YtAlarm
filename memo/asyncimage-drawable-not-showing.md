# AsyncImageでDrawableリソースIDが表示されない

## 問題

CoilのAsyncImageにDrawableリソースID（Int）を渡しても画像が表示されない。

## 原因

AsyncImageは直接IntをDrawableリソースIDとして認識しない場合がある。

## 解決策

`ImageRequest.Builder`を使用してDrawableとURLの両方に対応：

```kotlin
@Composable
fun ThumbnailImage(thumbnailUrl: Any?) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl ?: R.drawable.ic_no_image)
            .crossfade(true)
            .build(),
        contentDescription = "Thumbnail",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp)
    )
}
```

## 関連ファイル

- `AlarmItem.kt`
- `PlaylistItem.kt`
- `MultiChoiceVideoDialog.kt`

## 関連リンク

- [Coil - Image Requests](https://coil-kt.github.io/coil/image_requests/)
