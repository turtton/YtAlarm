# Switchのタップ範囲が広すぎて誤動作

## 問題

AlarmItemのSwitch部分をタップしていないのに、アイテム全体がクリック可能でSwitchが反応してしまう。

## 原因

外側のRowに`.clickable`修飾子が設定されており、Switch以外の部分をタップしてもSwitchが反応する。

## 解決策

`.clickable`修飾子をSwitch以外の部分にのみ適用：

```kotlin
@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // クリック可能な部分（Switchを除く）
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)  // ここに移動
        ) {
            // サムネイル、テキスト等
        }

        // Switch（独立したクリック範囲）
        Switch(
            checked = alarm.isEnable,
            onCheckedChange = onToggle
        )
    }
}
```

## ポイント

- 外側のRowには`.clickable`を付けない
- Switch以外の部分を内側のRowでラップし、そこに`.clickable`を適用
- Switchは独立したクリック範囲を持つ

## 関連ファイル

- `AlarmItem.kt`

## 関連ドキュメント

- [Clickable Modifier](https://developer.android.com/jetpack/compose/touch-input#clickable)
- [Gesture Detection](https://developer.android.com/jetpack/compose/touch-input)
