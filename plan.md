# YtAlarm Compose移行計画

## 📊 進捗サマリー

**最終更新**: 2025-11-02

### ✅ 完了済みフェーズ

- **Phase 0: 準備** (2025-01-30)
  - Compose依存関係の追加（BOM 2024.10.00, Kotlin 2.0.21対応）
  - Material3テーマ作成

- **Phase 1: リストアイテムの移行** (2025-10-30)
  - AlarmItem, PlaylistItem, VideoItem, AlarmSettingItem Composable実装
  - AlarmListComposeAdapter実装・Fragment統合完了

- **Phase 2: ダイアログの移行** (2025-10-30)
  - RemoveVideoDialog, UrlInputDialog, ExecuteProgressDialog, MultiChoiceVideoDialog実装

- **Phase 3: シンプルな画面の移行** (2025-10-31)
  - AboutPageScreen, VideoPlayerScreen実装

- **Phase 4: リスト画面の移行** (2025-10-31)
  - PlaylistScreen, VideoListScreen, AlarmListScreen実装

- **Phase 4.5: Screen設計の改善** (2025-10-31)
  - ScreenContentとScreenに分割（プレビュー対応）

- **Phase 5: 複雑な画面の移行** (2025-10-31)
  - AlarmSettingsScreen実装（6つのダイアログ含む）

- **Phase 6 Stage 1-3: Navigation統合 & MainActivity移行** (2025-11-01～2025-11-02)
  - Navigation基盤構築（YtAlarmDestination, YtAlarmNavGraph, YtAlarmApp）
  - MainActivity Compose化（ModalNavigationDrawer実装）
  - サムネイル表示バグ修正
  - Playlist選択ダイアログ統合

---

## 🚨 未修正の問題（Critical）

### 5. AlarmSettings画面から戻って1秒以内にDrawer操作すると白画面

**問題詳細**:
- AlarmSettingsから戻るボタンで戻った直後（1秒以内）にDrawerを開いて別画面に遷移すると白画面
- 通常速度の操作では再現しない（タイミング依存）
- 関連ログ: "OnBackInvokedCallback is not enabled for the application"

**原因特定**:
1. **MainScreen.kt:76-88**: Drawer操作時のナビゲーション処理に問題
   ```kotlin
   scope.launch {
       drawerState.close()  // ← 非同期だが完了を待たない
       if (currentRoute != route) {
           navController.navigate(route) {
               popUpTo(route) { inclusive = true }  // ← 不適切なpopUp設定
               launchSingleTop = true
           }
       }
   }
   ```
2. **AndroidManifest.xml**: `android:enableOnBackInvokedCallback="true"` が未設定
3. **Navigation状態の不安定性**: popBackStack()直後のNavigation状態が安定していない

**修正方針**:
1. AndroidManifest.xmlに `android:enableOnBackInvokedCallback="true"` を追加
2. MainScreen.ktのナビゲーションロジックを修正:
   ```kotlin
   navController.navigate(route) {
       popUpTo(navController.graph.findStartDestination().id) {
           saveState = true
       }
       launchSingleTop = true
       restoreState = true
   }
   ```
3. popBackStack()完了後に安定化待機処理を追加（必要に応じて）

**影響ファイル**:
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/net/turtton/ytalarm/ui/MainScreen.kt:76-88`

---

### 6. VideoList（全動画モード）で新規プレイリスト作成画面が表示

**問題詳細**:
- Drawerの"VideoList"をタップすると動画一覧ではなく"New Playlist"作成画面が表示
- 期待: 全動画一覧画面（playlistId=0）
- 実際: "Add videos to create a new playlist."メッセージが表示

**原因特定**:
- **MainScreen.kt:178-179**: VideoListのルートが正しく設定されている
  ```kotlin
  selected = currentRoute == YtAlarmDestination.videoList(0L),  // "video_list/0"
  onClick = { onNavigate(YtAlarmDestination.videoList(0L)) },   // "video_list/0"
  ```
- **YtAlarmNavGraph.kt:110-134**: VideoListScreenのルート定義も正しい
- **VideoListScreen.kt**: playlistId=0で全動画モードのはずが、新規プレイリスト作成UIを表示

**修正方針**:
- VideoListScreen.ktのplaylistId=0処理ロジックを確認
- 全動画モードと新規プレイリストモードの条件分岐を修正

**影響ファイル**:
- `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/VideoListScreen.kt`

---

## 📋 次のステップ

### Phase 6 Stage 4: 統合テスト・Fragment/XML削除

1. **Critical bugの修正** ⬅️ **最優先**
   - [ ] 白画面バグの修正（AlarmSettings戻り→Drawer操作）
   - [ ] VideoList全動画モードの修正

2. **Fragment完全削除**
   - [ ] FragmentAlarmList削除
   - [ ] FragmentAlarmSettings削除
   - [ ] FragmentPlaylist削除
   - [ ] FragmentVideoList / FragmentAllVideoList削除
   - [ ] FragmentVideoPlayer削除
   - [ ] FragmentAboutPage削除

3. **XML layout削除**
   - [ ] activity_main.xml削除（ComposeView統合後）
   - [ ] content_main.xml削除
   - [ ] drawer_header.xml削除
   - [ ] fragment_list.xml削除
   - [ ] fragment_video_player.xml削除
   - [ ] fragment_about.xml削除
   - [ ] item_aram.xml, item_playlist.xml, item_video_list.xml等削除

4. **ViewBinding関連削除**
   - [ ] binding関連コードの削除
   - [ ] Adapter類の削除（AlarmListAdapter等）

5. **統合テスト**
   - [ ] 全画面遷移テスト
   - [ ] Drawer機能テスト
   - [ ] アラーム作成・編集・削除テスト
   - [ ] プレイリスト作成・編集・削除テスト
   - [ ] 動画追加・削除テスト
   - [ ] アラーム実行テスト
   - [ ] パフォーマンステスト

6. **最終動作確認**
   - [ ] エミュレータテスト（API 24, 30, 34）
   - [ ] 実機テスト
   - [ ] スクリーンショット更新

---

## 🛠️ 技術スタック（移行後）

- ✅ Compose BOM 2024.10.00
- ✅ Material3
- ✅ Kotlin Compose Plugin (Kotlin 2.0.21)
- ✅ Coil for Compose (画像読み込み)
- ✅ ViewModel Compose統合
- ✅ Navigation Compose (String-based routes)

---

## 📁 実装済みファイル

```
app/src/main/kotlin/net/turtton/ytalarm/ui/compose/
├── theme/
│   ├── Color.kt                ✅
│   └── Theme.kt                ✅
├── components/
│   ├── AlarmItem.kt            ✅
│   ├── PlaylistItem.kt         ✅
│   ├── VideoItem.kt            ✅
│   ├── AlarmSettingItem.kt     ✅
│   └── AboutPageItem.kt        ✅
├── dialogs/
│   ├── RemoveVideoDialog.kt           ✅
│   ├── UrlInputDialog.kt              ✅
│   ├── ExecuteProgressDialog.kt       ✅
│   ├── MultiChoiceVideoDialog.kt      ✅
│   ├── TimePickerDialog.kt            ✅
│   ├── DatePickerDialog.kt            ✅
│   ├── RepeatTypeDialog.kt            ✅
│   ├── DayOfWeekPickerDialog.kt       ✅
│   ├── SnoozeMinutePickerDialog.kt    ✅
│   └── VibrationWarningDialog.kt      ✅
├── screens/
│   ├── AboutPageScreen.kt      ✅
│   ├── VideoPlayerScreen.kt    ✅
│   ├── PlaylistScreen.kt       ✅
│   ├── VideoListScreen.kt      ✅
│   ├── AlarmListScreen.kt      ✅
│   └── AlarmSettingsScreen.kt  ✅
├── navigation/
│   ├── YtAlarmDestination.kt         ✅
│   ├── YtAlarmNavGraph.kt            ✅
│   ├── NavigationExtensions.kt       ✅
│   └── CompositionLocals.kt          ✅
├── YtAlarmApp.kt               ✅
└── MainScreen.kt               ✅
```

---

## 📚 参考資料

### 技術メモ

詳細は **[memo/README.md](memo/README.md)** を参照してください。

主なトピック：
- Compose環境構築
- VideoView/Selection API/フルスクリーン等の実装パターン
- Navigation/ViewModel/Flow等の統合方法
- 遭遇した問題と解決策

### 公式ドキュメント
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Migration Guide](https://developer.android.com/jetpack/compose/migrate)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Material 3](https://m3.material.io/)

---

## ✅ 成功基準

### 機能面
- [ ] すべての既存機能が動作
- [ ] UIの見た目が既存と同等以上
- [ ] パフォーマンスが既存と同等以上

### コード品質
- [ ] XMLファイルを90%以上削除
- [ ] ViewBindingコードを100%削除
- [ ] テストカバレッジを維持または向上

### 運用面
- [ ] ビルド時間が大幅に増加しない
- [ ] アプリサイズが大幅に増加しない（<10%増）
- [ ] クラッシュ率が増加しない
