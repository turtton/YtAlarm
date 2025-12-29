# ダイアログ選択内容がUIに反映されない

## 問題

MultiChoiceVideoDialogでプレイリストを選択しても、AlarmSettingsScreenのPlaylistフィールドに選択内容が反映されない。

## 原因

ダイアログのonConfirmコールバック内でDBは更新されるが、**ローカルステート（`editingAlarm`）が更新されない**ため、`LaunchedEffect(editingAlarm?.playListId)`が再実行されずUIが更新されない。

## 解決策

onConfirmコールバック内で**ローカルステートを直接更新**：

```kotlin
// ❌ 悪い例: ローカル変数に代入するだけ
val newAlarm = editingAlarm?.copy(playListId = selectedPlaylistIds)

// ✅ 良い例: ステート変数を直接更新
editingAlarm = editingAlarm?.copy(playListId = selectedPlaylistIds)
```

## ポイント

- Composeの再コンポジションはステート変更で発動
- ローカル変数への代入では再コンポジションされない
- `LaunchedEffect(key)`のkeyとなるステートを更新する

## 関連ファイル

- `AlarmSettingsScreen.kt:515`

## 関連ドキュメント

- [State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)
- [Recomposition](https://developer.android.com/jetpack/compose/mental-model#recomposition)
