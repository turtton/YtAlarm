# フルスクリーンモード

## 概要

SystemUiControllerを使用してシステムバーを制御します。

## 実装パターン

```kotlin
@Composable
fun FullScreenContent() {
    val systemUiController = rememberSystemUiController()

    DisposableEffect(Unit) {
        systemUiController.isSystemBarsVisible = false
        systemUiController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            systemUiController.isSystemBarsVisible = true
        }
    }

    // コンテンツ
}
```

## 必要なライブラリ

Accompanist System UI Controller（またはAndroidXのWindowInsetsControllerCompat）

```toml
[libraries]
accompanist-systemuicontroller = { group = "com.google.accompanist", name = "accompanist-systemuicontroller", version = "..." }
```

## ポイント

- `DisposableEffect`で画面表示時/非表示時の処理を定義
- `onDispose`で必ず元に戻す（メモリリーク防止）

## 代替実装（AndroidX WindowInsets）

```kotlin
@Composable
fun FullScreenContentWithInsets() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    DisposableEffect(Unit) {
        val insetsController = WindowCompat.getInsetsController(window!!, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // コンテンツ
}
```

## 関連ドキュメント

- [WindowInsets in Compose](https://developer.android.com/jetpack/compose/layouts/insets)
- [Accompanist System UI Controller](https://google.github.io/accompanist/systemuicontroller/)
