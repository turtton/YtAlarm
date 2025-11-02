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

## 🚨 未実装機能（Phase 6で保留）

Phase 6完了後のテストで、以下の未実装機能が発見されました（2025-11-02）。
これらはCompose移行時にTODOコメントとして残されており、次のフェーズで実装が必要です。

### 1. 動画クリック→VideoPlayer遷移が未実装 ⚠️ **Critical**

**問題詳細**:
- VideoList画面で動画をクリックしても何も起こらない
- VideoPlayer画面に遷移しない
- 動画再生のコア機能が利用できない

**原因特定**:
- **VideoListScreen.kt:248-254**: `onClick`ハンドラー内の処理が未実装
  ```kotlin
  onClick = {
      if (selectedItems.isEmpty()) {
          // TODO: 動画プレーヤーへ遷移
      } else {
          onItemSelect(video.id, !selectedItems.contains(video.id))
      }
  },
  ```

**修正方針**:
1. VideoPlayer画面へのナビゲーション処理を実装
2. `onNavigateToVideoPlayer`コールバックを追加
3. 動画IDとプレイリスト情報を渡す
4. YtAlarmNavGraphにVideoPlayer画面のルートを追加

**影響範囲**:
- VideoList画面（全動画モード）
- VideoList画面（プレイリスト内）

---

### 2. 縦3点ボタンのメニューが未実装 ⚠️ **High**

**問題詳細**:
- VideoList画面の動画アイテム縦3点ボタン（⋮）をクリックしても何も起こらない
- Playlist画面のプレイリスト縦3点ボタンをクリックしても何も起こらない
- 削除・編集などの重要機能が利用できない

**原因特定**:
- **VideoListScreen.kt:255-257**: `onMenuClick`ハンドラーが空実装
  ```kotlin
  onMenuClick = {
      // 個別メニューアクション（今後実装）
  }
  ```
- **PlaylistScreen.kt:204-206**: 同様に空実装

**修正方針**:
1. DropdownMenuまたはBottomSheetを実装
2. メニューアイテム（削除、編集、情報表示など）を追加
3. 各メニューアクションの処理を実装
4. ダイアログとの連携を実装

**影響範囲**:
- VideoList画面の個別動画メニュー
- Playlist画面の個別プレイリストメニュー

---

**優先度**:
1. **Priority 1 (Critical)**: 動画クリック→VideoPlayer遷移の実装
2. **Priority 2 (High)**: 縦3点メニューの実装（Video、Playlist）
3. **Priority 3 (Medium)**: メニュー内の各機能の詳細実装

---

## 📋 次のステップ

### Phase 7: 未実装機能の実装 ⬅️ **次のフェーズ**

1. **動画クリック→VideoPlayer遷移の実装** (Priority 1 - Critical)
   - [ ] VideoListScreen.ktのonClick処理実装
   - [ ] YtAlarmNavGraphにVideoPlayerルート追加
   - [ ] ナビゲーション処理の実装
   - [ ] 動作確認テスト

2. **縦3点メニューの実装** (Priority 2 - High)
   - [ ] VideoList画面のメニュー実装
   - [ ] Playlist画面のメニュー実装
   - [ ] 削除・編集ダイアログ連携
   - [ ] 動作確認テスト

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
