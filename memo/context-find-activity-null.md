# Context.findActivity()がnullを返す

## 問題

Composable内で`LocalContext.current.findActivity()`を使用すると、`findActivity()`メソッドが見つからないエラー。

## 原因

`Context`から`AppCompatActivity`を取得する標準APIが存在しない。

## 解決策

ヘルパー拡張関数を定義：

```kotlin
fun Context.findActivity(): AppCompatActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is AppCompatActivity) return context
        context = context.baseContext
    }
    return null
}
```

使用例：

```kotlin
@Composable
fun SomeScreen() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val preferences = activity?.privatePreferences
}
```

## ポイント

- ContextWrapper階層を遡ってAppCompatActivityを探す
- nullの可能性があるため、null安全な処理が必要

## 関連ファイル

- `ContextExtensions.kt`（または適切な拡張関数ファイル）

## 関連ドキュメント

- [LocalContext - Compose](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary#LocalContext())
- [ContextWrapper](https://developer.android.com/reference/android/content/ContextWrapper)
