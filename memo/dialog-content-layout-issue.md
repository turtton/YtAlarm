# ダイアログとコンテンツが並列配置でレイアウト崩れ

## 問題

AlarmSettingsScreenでAlarmSettingsScreenContentとMultiChoiceVideoDialogを並列に配置すると、レイアウトが崩れる。

## 原因

複数のComposableを並列配置する場合、親コンテナ（Column, Box等）が必要。

## 解決策

Boxコンテナでラップ：

```kotlin
@Composable
fun AlarmSettingsScreen() {
    Box {
        AlarmSettingsScreenContent(/* ... */)

        if (showPlaylistDialog) {
            MultiChoiceVideoDialog(/* ... */)
        }
    }
}
```

## ポイント

- ダイアログはBoxの最上位レイヤーに表示される
- AlarmSettingsScreenContentは背景として表示される
- ダイアログ表示時も背景コンテンツは維持される

## 関連ファイル

- `AlarmSettingsScreen.kt`

## 関連ドキュメント

- [Box Layout](https://developer.android.com/jetpack/compose/layouts/box)
