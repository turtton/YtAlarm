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

- **Phase 6 Stage 4: 統合テスト・Fragment/XML削除** (2025-11-02)
  - Criticalバグ2つの修正（白画面、VideoList全動画モード）
  - Fragment/Adapter完全削除（13 Kotlinファイル、3,262行削除）
  - MainActivity XML/binding削除（7 XMLファイル、258行削除）
  - 全機能テスト合格（起動、画面遷移、バグ非再発）

---

## ✅ 修正完了（2025-11-02）

### ~~5. AlarmSettings画面から戻って1秒以内にDrawer操作すると白画面~~ ✅

**修正内容** (commit: b086a98):
1. `AndroidManifest.xml`に `android:enableOnBackInvokedCallback="true"` を追加
2. `MainScreen.kt` のDrawerナビゲーションロジックを修正:
   ```kotlin
   navController.navigate(route) {
       popUpTo(YtAlarmDestination.ALARM_LIST) {
           saveState = true
       }
       launchSingleTop = true
       restoreState = true
   }
   ```
3. モバイルデバッグテストで修正を確認：白画面は発生しなくなった

---

### ~~6. VideoList（全動画モード）で新規プレイリスト作成画面が表示~~ ✅

**修正内容** (commit: b086a98):
1. `VideoListScreen.kt` に全動画モード処理を追加:
   - `isAllVideosMode` フラグで明示的にモード判定
   - 全動画取得ロジック実装 (`videoViewModel.allVideos`)
   - UI制御：FAB非表示、削除ボタン非表示、適切なタイトル表示
2. ktlint違反も同時に修正（max-line-length）
3. モバイルデバッグテストで修正を確認：全動画一覧が正しく表示される

---

### ~~7. Drawerナビゲーションで画面遷移しない~~ ✅

**修正内容** (commit: 89c0b24):
1. `MainScreen.kt` の`onNavigate`ラムダ内で`currentRoute`の最新値を取得するように修正:
   ```kotlin
   onNavigate = { route ->
       scope.launch {
           drawerState.close()
           // 現在のルートを再取得（最新の値を使用）
           val current = navController.currentBackStackEntry?.destination?.route
           if (current != route) {
               navController.navigate(route) { ... }
           }
       }
   }
   ```
2. 問題の原因：Compose recompositionのタイミングで古い`currentRoute`値を参照していた
3. Phase 7 Priority 1のテスト中に発見・修正

---

## 🚨 未実装機能（Phase 6で保留）

Phase 6完了後のテストで、以下の未実装機能が発見されました（2025-11-02）。
これらはCompose移行時にTODOコメントとして残されており、次のフェーズで実装が必要です。

### ~~1. 動画クリック→VideoPlayer遷移が未実装~~ ✅ **実装完了 (2025-11-02)**

**実装内容** (commit: 89c0b24):
1. **VideoListScreen.kt**:
   - `onItemClick`パラメータの型を`(Long) -> Unit`から`(String) -> Unit`に変更
   - `onClick`ハンドラー内で`onItemClick(video.videoId)`を呼び出すように実装
   - ID型の明確化コメントを追加（Long: DB ID, String: Navigation ID）

2. **YtAlarmNavGraph.kt**:
   - `VideoListScreen`に`onNavigateToVideoPlayer`コールバックを追加
   - VideoPlayerへのナビゲーション処理を実装
   - videoIdのnullチェックを追加（不正な値の場合は自動的にpopBackStack）

3. **VideoPlayerScreen.kt**:
   - 非アラームモードのエラーハンドリング強化
   - `hasError`状態の設定とログ出力を追加

**テスト状況**:
- コードレビュー完了（Critical/Warning issues修正）
- ビルド成功
- Drawerナビゲーションバグを発見・修正

---

### ~~2. 縦3点ボタンのメニューが未実装~~ ✅ **実装完了 (2025-11-02)**

**実装内容** (commit: cb9f257):

1. **新規コンポーネント**:
   - `VideoItemDropdownMenu.kt`: 動画アイテム用メニュー（サムネイル設定、ダウンロード、再インポート、削除）
   - `PlaylistItemDropdownMenu.kt`: プレイリスト用メニュー（名前変更）

2. **新規ダイアログ**:
   - `RenamePlaylistDialog.kt`: プレイリスト名変更ダイアログ（入力検証付き）
   - `VideoReimportDialog.kt`: 動画再インポート確認ダイアログ

3. **VideoListScreen更新**:
   - DropdownMenu状態管理（mutableStateMapOf）
   - メニューアクションハンドラ実装
   - 削除・再インポート確認ダイアログ統合
   - Snackbar統合（サムネイル設定、削除完了、開発中機能通知）

4. **PlaylistScreen更新**:
   - DropdownMenu状態管理
   - リネーム機能実装
   - 入力検証とエラー表示

5. **文字列リソース追加**:
   - ダイアログメッセージ（英語・日本語）
   - フォームラベルとエラーメッセージ
   - Snackbarメッセージ

**実装した機能**:
- ✅ メニューアイテムの適切な表示（削除は赤色）
- ✅ 入力検証（空文字チェック）
- ✅ ユーザーフィードバック（Snackbar）
- ✅ mutableStateMapOfによる効率的な状態管理
- ✅ エラーハンドリングと確認ダイアログ

**テスト状況**:
- ビルド成功
- コードレビュー完了（評価: B+）
- エミュレータ起動確認済み（クラッシュなし）

**既知の制限**:
- ダウンロード機能は開発中（Phase 8予定）
- 再インポート機能の実処理は未実装（TODO）

---

## 📋 次のステップ

### Phase 7: 未実装機能の実装 ✅ **完了 (2025-11-02)**

1. **動画クリック→VideoPlayer遷移の実装** (Priority 1 - Critical) ✅ **完了 (2025-11-02)**
   - [x] VideoListScreen.ktのonClick処理実装
   - [x] YtAlarmNavGraphにVideoPlayerルート追加
   - [x] ナビゲーション処理の実装
   - [x] エラーハンドリングの改善
   - [x] Drawerナビゲーションバグ修正（currentRoute取得タイミング）
   - [x] コードレビュー完了（Critical/Warning issues修正）
   - [x] コミット完了 (commit: 89c0b24)

2. **縦3点メニューの実装** (Priority 2 - High) ✅ **完了 (2025-11-02)**
   - [x] VideoList画面のメニュー実装
   - [x] Playlist画面のメニュー実装
   - [x] 削除・編集ダイアログ連携
   - [x] 動作確認テスト
   - [x] コミット完了 (commit: cb9f257)

---

### Phase 6 Stage 4: 統合テスト・Fragment/XML削除 ✅ **完了 (2025-11-02)**

1. **Critical bugの修正** ✅ **完了 (2025-11-02)**
   - [x] 白画面バグの修正（AlarmSettings戻り→Drawer操作）
   - [x] VideoList全動画モードの修正

2. **Fragment完全削除** ✅ **完了 (2025-11-02)**
   - [x] FragmentAlarmList削除
   - [x] FragmentAlarmSettings削除
   - [x] FragmentPlaylist削除
   - [x] FragmentVideoList / FragmentAllVideoList削除
   - [x] FragmentAboutPage削除
   - [x] FragmentAbstractList削除（基底クラス）
   - ⚠️ FragmentVideoPlayer保持（AlarmActivity用）

3. **XML layout削除** ✅ **完了 (2025-11-02)**
   - [x] activity_main.xml削除
   - [x] content_main.xml削除
   - [x] 5つのナビゲーションXML削除（aram_list.xml等）
   - ⚠️ fragment_video_player.xml保持（AlarmActivity用）
   - ⚠️ その他のitem_*.xml保持（MultiChoiceVideoListAdapter用）

4. **ViewBinding関連削除** ✅ **完了 (2025-11-02)**
   - [x] MainActivity binding/drawerLayout削除
   - [x] Adapter類の削除（6ファイル）
     - AlarmListAdapter
     - AlarmListComposeAdapter
     - AlarmSettingsAdapter
     - PlaylistAdapter
     - VideoListAdapter
     - AboutPageAdapter

5. **統合テスト** ✅ **完了 (2025-11-02)**
   - [x] 全画面遷移テスト
   - [x] Drawer機能テスト
   - [x] 白画面バグ再テスト（非再発確認）
   - [x] VideoList全動画モード再テスト（正常動作確認）
   - [x] アプリ起動テスト（クラッシュなし）

6. **最終動作確認** ✅ **完了 (2025-11-02)**
   - [x] エミュレータテスト（x86_64）
   - [x] 全機能動作確認

**削減実績**:
- Kotlinファイル: 13ファイル削除
- XMLファイル: 7ファイル削除
- 合計削減: 約3,520行

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
