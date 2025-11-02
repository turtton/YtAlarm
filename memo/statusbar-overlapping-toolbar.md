# ステータスバーがToolbarに重なる

## 問題

Composeに移行後、ステータスバーがToolbarに重なって表示される。

## 原因

`fitsSystemWindows`属性が設定されていない。

## 解決策

XMLレイアウトのルートコンテナに`android:fitsSystemWindows="true"`を追加：

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">
    <!-- ... -->
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

## Composeでの対応

全画面Composeの場合は、WindowInsetsを使用：

```kotlin
@Composable
fun MainScreen() {
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // コンテンツ
    }
}
```

## 関連ファイル

- `activity_main.xml`

## 関連ドキュメント

- [Window Insets in Compose](https://developer.android.com/jetpack/compose/layouts/insets)
- [fitsSystemWindows](https://medium.com/androiddevelopers/why-would-i-want-to-fitssystemwindows-4e26d9ce1eec)
