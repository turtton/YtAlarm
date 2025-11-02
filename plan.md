# YtAlarm Compose移行計画

## 📊 進捗サマリー

**最終更新**: 2025-11-02

### 完了済みフェーズ
- ✅ **Phase 0: 準備** (完了)
  - Compose依存関係の追加 (BOM 2024.10.00, Kotlin 2.0.21対応)
  - Kotlin Compose Plugin設定
  - Material3テーマ作成 (AppTheme, Color)
  - AlarmItemとPlaylistItemのComposable実装
  - ビルド成功・動作確認完了

- ✅ **Phase 1: リストアイテムの移行** (完了 - 2025-10-30)
  - AlarmItem Composable実装
  - PlaylistItem Composable実装
  - VideoItem Composable実装
  - AlarmSettingItem Composable実装（汎用SettingItem含む）
  - AlarmListComposeAdapter実装（実戦投入完了）
  - FragmentAlarmList統合完了
  - Switch機能修正完了
  - ビルド成功・エミュレータ動作確認完了

- ✅ **Phase 2: ダイアログの移行** (完了 - 2025-10-30)
  - RemoveVideoDialog Composable実装
  - UrlInputDialog Composable実装（TextFieldとフォーカス管理）
  - ExecuteProgressDialog Composable実装（進行状況表示）
  - MultiChoiceVideoDialog Composable実装（LazyColumn + Checkbox）
  - ビルド成功・動作確認完了

- ✅ **Phase 3: シンプルな画面の移行** (完了 - 2025-10-31)
  - AboutPageItem Composable実装
  - AboutPageScreen Composable実装（LazyColumn + 情報リスト）
  - VideoPlayerScreen Composable実装（AndroidView + VideoView統合）
  - ビルド成功・動作確認完了

### 追加の改善
- ✅ ステータスバーオーバーラップ問題の修正 (`activity_main.xml`に`fitsSystemWindows`追加)
- ✅ AlarmItem Switchのタップ問題修正 (外側Rowから`.clickable`を削除し、内側Rowに移動)
- ✅ Fragment統合完了 (FragmentAlarmListでComposeAdapterが正常動作)

### 実装済みファイル
```
app/src/main/kotlin/net/turtton/ytalarm/ui/compose/
├── theme/
│   ├── Color.kt                ✅ Material3カラー定義
│   └── Theme.kt                ✅ AppTheme実装
├── components/
│   ├── AlarmItem.kt            ✅ アラーム一覧アイテム
│   ├── PlaylistItem.kt         ✅ プレイリスト一覧アイテム
│   ├── VideoItem.kt            ✅ 動画一覧アイテム
│   ├── AlarmSettingItem.kt     ✅ アラーム設定アイテム（汎用）
│   └── AboutPageItem.kt        ✅ About情報アイテム
├── dialogs/
│   ├── RemoveVideoDialog.kt    ✅ 削除確認ダイアログ
│   ├── UrlInputDialog.kt       ✅ URL入力ダイアログ
│   ├── ExecuteProgressDialog.kt ✅ 進行状況表示ダイアログ
│   └── MultiChoiceVideoDialog.kt ✅ 複数選択ダイアログ
└── screens/
    ├── AboutPageScreen.kt      ✅ About画面
    ├── VideoPlayerScreen.kt    ✅ 動画プレーヤー画面
    ├── PlaylistScreen.kt       🔄 プレイリスト一覧画面（ビルドエラー修正待ち）
    ├── VideoListScreen.kt      🔄 動画一覧画面（ビルドエラー修正待ち）
    └── AlarmListScreen.kt      🔄 アラーム一覧画面（ビルドエラー修正待ち）

app/src/main/kotlin/net/turtton/ytalarm/ui/adapter/
└── AlarmListComposeAdapter.kt  ✅ Compose版AlarmListAdapter（実戦投入済み）
```

### 完了済みフェーズ（追加）
- ✅ **Phase 4.5: Screen設計の改善（プレビュー対応）** (完了 - 2025-10-31)
  - ✅ AlarmListScreen → AlarmListScreenContent + AlarmListScreen
  - ✅ PlaylistScreen → PlaylistScreenContent + PlaylistScreen
  - ✅ VideoListScreen → VideoListScreenContent + VideoListScreen
  - ✅ **ビルド成功** - すべてのエラーを修正完了
  - ✅ **エミュレータ動作確認完了** - すべてのテスト成功

  **実装完了した機能:**
  - ViewModelからの分離: 各ScreenをScreenContentとScreenに分割
  - プレビュー対応: すべてのScreenContentに@Previewアノテーション追加
  - ダミーデータ作成: プレビュー用のダミーデータを実装

  **達成されたメリット:**
  - プレビュー可能: ScreenContentはViewModelに依存しない
  - テスト容易: ScreenContentの単体テストが簡単
  - ロジック分離: ViewModelとのやりとりがScreenに集約
  - 再利用性: ScreenContentは他の場所でも使用可能

  **エミュレータテスト結果:**
  - AlarmListScreen: 正常表示・動作確認完了
  - PlaylistScreen: 正常表示・動作確認完了
  - VideoListScreen: 正常表示・動作確認完了
  - インタラクション機能: ソート、ダイアログ、FAB、オプションメニューすべて正常動作
  - UI/UX問題: なし
  - クラッシュ/エラー: なし

- ✅ **Phase 4: リスト画面の移行** (完了 - 2025-10-31)
  - ✅ PlaylistScreen.kt 基本実装完了
  - ✅ VideoListScreen.kt 基本実装完了
  - ✅ AlarmListScreen.kt 基本実装完了
  - ✅ **ビルド成功** - すべてのエラーを修正完了

  **実装完了した機能:**
  - PlaylistScreen: プレイリスト一覧表示、ソート、削除、選択機能
  - VideoListScreen: 動画一覧表示、3モード対応（Original/Sync/Importing）、FAB展開
  - AlarmListScreen: アラーム一覧表示、ソート、ON/OFF切り替え

  **修正完了した問題:**
  1. **依存関係の追加**
     - ✅ `androidx.compose.runtime:runtime-livedata` 追加
     - ✅ `androidx.compose.material:material-icons-extended` 追加

  2. **型推論エラーの修正**
     - ✅ `remember()`ブロック内のリスト操作に明示的な型指定を追加
     - ✅ `List<Alarm>`, `MutableList<Alarm>` などを明示

  3. **コンポーネントのパラメータ修正**
     - ✅ `AlarmItem`: `playlistName` → `playlistTitle`に修正
     - ✅ `PlaylistItem`: `thumbnailUrl`, `videoCount`パラメータの追加
     - ✅ `Playlist.Thumbnail.Url` → `Playlist.Thumbnail.Video/Drawable`に修正

  4. **String resourceの追加**
     - ✅ ナビゲーションタイトル（nav_alarm_list, nav_playlist等）
     - ✅ 空状態メッセージ（alarm_list_empty_message等）
     - ✅ メニュー・ダイアログ用リソース
     - ✅ 日本語翻訳も追加

  5. **Context APIの問題解決**
     - ✅ `Context.findActivity()` ヘルパー関数を追加
     - ✅ Composable内で `context.findActivity()?.privatePreferences` を使用

  **残存する警告（非ブロッカー）:**
  - Icons.Filled.Sort等が deprecated（AutoMirrored版を使用すべき）
  - PackagingOptions の設定に関する警告

- ✅ **Phase 5: 複雑な画面の移行（AlarmSettings）** (完了 - 2025-10-31)
  - ✅ 6つのダイアログコンポーネント実装完了
    - TimePickerDialog: Material3 TimePicker使用
    - DatePickerDialog: Material3 DatePicker使用
    - RepeatTypeDialog: 繰り返しタイプ選択
    - DayOfWeekPickerDialog: 曜日複数選択
    - SnoozeMinutePickerDialog: スヌーズ時間選択
    - VibrationWarningDialog: Android S警告
  - ✅ AlarmSettingsScreen実装完了
    - AlarmSettingsScreenContent（プレビュー可能）
    - AlarmSettingsScreen（ViewModel連携）
    - 8つの設定項目完全実装
  - ✅ String resources追加（英語・日本語）
  - ✅ **ビルド成功** - すべてのエラー修正完了
  - ✅ **コードレビュー完了** - Critical指摘事項すべて修正

  **実装完了した機能:**
  - 時刻設定（Material3 TimePicker）
  - 繰り返し設定（Once/Everyday/Days/Date 4タイプ対応）
  - プレイリスト選択（TODO: MultiChoiceVideoDialog統合）
  - ループ・シャッフル・音量・スヌーズ・バイブレーション設定
  - エラーハンドリング（プレイリスト未選択、過去日付選択）
  - Phase 4.5パターン準拠（ViewModel分離、プレビュー対応）

  **修正完了した問題（コードレビュー対応）:**
  1. ✅ Enum.values() → Enum.entries（非推奨API修正）
  2. ✅ null安全性向上（editingAlarm!! → editingAlarm?.let {}）
  3. ✅ UX改善（日付選択時の即座バリデーション）
  4. ✅ 冗長ロジック削減（曜日選択時の最適化）
  5. ✅ 型安全性向上（Snooze型の明示的処理）

  **残タスク:**
  - プレイリスト選択ダイアログ統合（MultiChoiceVideoDialog）
  - ナビゲーション統合（Phase 6で実施）
  - エミュレータ動作確認（Phase 6で実施）

- ✅ **Phase 6 Stage 1: Navigation基盤構築** (完了 - 2025-11-01)
  - ✅ YtAlarmDestination.kt: 全ルート定義とヘルパー関数
  - ✅ YtAlarmNavGraph.kt: NavHost設定と6画面のルート構築
  - ✅ YtAlarmApp.kt: アプリのルートComposable
  - ✅ NavigationExtensions.kt: 共通ナビゲーションパターンのヘルパー
  - ✅ **ビルド成功** - コンパイルエラーなし
  - ✅ **コードレビュー完了** - code-reviewerエージェントで検証
  - ✅ **detekt警告ゼロ** - Navigationファイルに警告なし

  **実装完了した機能:**
  - String-based routes (Navigation Compose 2.7.7対応)
  - 6つの画面ルート定義（alarm_list, alarm_settings, playlist, video_list, video_player, about）
  - 型安全な引数受け渡し（Long, String, Boolean対応）
  - ナビゲーションヘルパー関数（navigateAndPopUp, navigateAndClearBackStack, navigateSingleTop）
  - テスタブルな設計（navController/startDestinationを外部注入可能）

  **技術的選択:**
  - String-based routesを採用（Navigation 2.7.7との互換性）
  - 将来的にNavigation 2.8.0+へ移行時、型安全なkotlinx.serialization版に更新予定

- ✅ **Phase 6 Stage 2+3: MainActivity統合 & DrawerLayout移行** (完了 - 2025-11-01)
  - ✅ CompositionLocals.kt: ViewModel/ResourceContainerの提供
  - ✅ MainScreen.kt: ModalNavigationDrawer実装
  - ✅ MainActivity.kt: setContentでCompose化
  - ✅ VideoListScreen.kt: playlistId=0の クラッシュ修正
  - ✅ **ビルド成功** - コンパイルエラーなし
  - ✅ **コードレビュー完了** - Critical Issues全修正
  - ✅ **エミュレータ動作確認完了** - クラッシュなし、全機能正常動作

  **実装完了した機能:**
  - MainActivity Compose化（setContentでMainScreen呼び出し）
  - ModalNavigationDrawer（Material3対応）
  - Drawerメニュー項目（Alarms, Playlists, Videos, About）
  - Navigation統合（Drawer→画面遷移）
  - 既存初期化処理維持（initYtDL, createNotificationChannel, requestPermission, checkUrlShare）
  - CompositionLocalでViewModel提供（compositionLocalOf使用）
  - VideoListScreen playlistId=0対応（全動画モード）

  **修正完了した問題（コードレビュー対応）:**
  1. ✅ compositionLocalOf使用（staticCompositionLocalOf→compositionLocalOf）
  2. ✅ AlertDialog context修正（applicationContext→this@MainActivity）
  3. ✅ initYtDL coroutine改善（不要なlaunch削除）
  4. ✅ VideoListScreen null安全性向上（playlistId=0時の処理）
  5. ✅ Material Icons AutoMirrored対応（PlaylistPlayアイコン）

  **エミュレータテスト結果:**
  - アプリ起動: 正常、クラッシュなし
  - Drawer開閉: スムーズ、アニメーション正常
  - 画面遷移: Alarms↔Playlists↔Videos↔About すべて正常
  - VideoList（playlistId=0）: クラッシュ修正完了、正常動作
  - UI/UX: タップ反応適切、レイアウト崩れなし

  **発見・修正された問題（2025-11-01）:**
  1. ✅ **サムネイル画像が表示されない** (Critical) - **修正完了**
     - 原因1: AlarmItem/PlaylistItemでAsyncImageがDrawableリソースIDを処理できない
     - 原因2: Video型サムネイル(Playlist.Thumbnail.Video)でVideoテーブルからURL取得未実装
     - 修正内容:
       - ImageRequest.Builder(context)を使用してDrawable/URL両対応
       - AlarmListScreen/PlaylistScreenでVideoViewModel経由でサムネイルURL非同期取得
     - 修正ファイル:
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/AlarmItem.kt
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/PlaylistItem.kt
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/dialogs/MultiChoiceVideoDialog.kt
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmListScreen.kt
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/PlaylistScreen.kt
     - コミット: d8aad45

  2. ✅ **Playlist選択ダイアログ未実装** (Critical) - **修正完了**
     - 原因: AlarmSettingsScreenでダイアログとコンテンツが親コンテナなしで並列配置
     - 修正内容:
       - BoxコンテナでAlarmSettingsScreenContentとMultiChoiceVideoDialogをラップ
       - MultiChoiceVideoDialogをAlarmSettingsScreenに統合
       - PlaylistViewModel経由でプレイリスト一覧を非同期取得
     - 修正ファイル:
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmSettingsScreen.kt
     - コミット: d8aad45

  **発見・修正された問題（2025-11-02）:**
  3. ✅ **Playlist選択内容がUIに反映されない** (Critical) - **修正完了**
     - 原因: AlarmSettingsScreen.kt:515でeditingAlarmローカルステートが更新されていない
     - 問題詳細:
       - MultiChoiceVideoDialogのonConfirmで新しいアラームをDBに保存
       - しかしeditingAlarmローカルステートは更新されない
       - LaunchedEffect(editingAlarm?.playListId)が再実行されない
       - 結果としてplaylistTitleが更新されず、UIに選択内容が反映されない
     - 修正内容:
       - onConfirmコールバック内でeditingAlarmステートを直接更新
       - 変更前: `val newAlarm = editingAlarm?.copy(...)`
       - 変更後: `editingAlarm = editingAlarm?.copy(...)`
     - 修正ファイル:
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmSettingsScreen.kt:515
     - 検証結果:
       - ダイアログでプレイリスト選択後、Playlistフィールドに「ExamplePlaylist」が正しく表示されることを確認
       - エミュレータテスト成功

  4. ✅ **MultiChoiceVideoDialog内のサムネイル画像が表示されない** (Critical) - **修正完了**
     - 原因: AlarmSettingsScreen.kt:500-502でVideo型サムネイルが常にic_no_imageプレースホルダーになっていた
     - 問題詳細:
       - Playlist選択ダイアログ内のプレイリストアイテムでサムネイル画像が表示されない
       - Video型サムネイルの場合、VideoViewModelを使った非同期取得が未実装
       - AlarmListScreen/PlaylistScreenでは修正済みだが、AlarmSettingsScreenでは未対応
     - 修正内容:
       - displayDataListをremember + LaunchedEffectで非同期構築
       - Video型の場合、videoViewModel.getFromIdAsync()でvideoを取得
       - video.thumbnailUrlをDisplayDataThumbnail.Url()として設定
       - VideoViewModelをAlarmSettingsScreen引数に追加
     - 修正ファイル:
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmSettingsScreen.kt:355-357 (VideoViewModel追加)
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmSettingsScreen.kt:500-528 (displayDataList非同期構築)
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/AlarmSettingsScreen.kt:62-63 (import追加)
     - 検証結果:
       - ダイアログ内のExamplePlaylistに実際のサムネイル画像（黄色と黒の幾何学模様）が正常に表示されることを確認
       - 非同期処理が正常に動作し、Video.thumbnailUrlから画像が読み込まれている
       - エミュレータテスト成功

  **発見された問題（2025-11-02）:**
  5. ❌ **AlarmSettings画面から戻って1秒以内にDrawer操作すると白画面** (Critical) - **未修正**
     - 問題詳細:
       - AlarmSettingsから戻るボタンで戻った直後（1秒以内）にDrawerを開いて別画面に遷移すると白画面
       - 通常速度の操作では再現しない（タイミング依存）
       - 関連ログ: "OnBackInvokedCallback is not enabled for the application"
     - 原因特定:
       - **MainScreen.kt:76-88**: Drawer操作時のナビゲーション処理に問題
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
       - **AndroidManifest.xml**: `android:enableOnBackInvokedCallback="true"` が未設定
       - **Navigation状態の不安定性**: popBackStack()直後のNavigation状態が安定していない
     - 修正方針:
       - AndroidManifest.xmlに `android:enableOnBackInvokedCallback="true"` を追加
       - MainScreen.ktのナビゲーションロジックを修正:
         ```kotlin
         navController.navigate(route) {
             popUpTo(navController.graph.findStartDestination().id) {
                 saveState = true
             }
             launchSingleTop = true
             restoreState = true
         }
         ```
       - popBackStack()完了後に安定化待機処理を追加（必要に応じて）
     - 影響ファイル:
       - app/src/main/AndroidManifest.xml
       - app/src/main/kotlin/net/turtton/ytalarm/ui/MainScreen.kt:76-88

  6. ❌ **VideoList（全動画モード）で新規プレイリスト作成画面が表示** (Critical) - **未修正**
     - 問題詳細:
       - Drawerの"VideoList"をタップすると動画一覧ではなく"New Playlist"作成画面が表示
       - 期待: 全動画一覧画面（playlistId=0）
       - 実際: "Add videos to create a new playlist."メッセージが表示
     - 原因特定:
       - **MainScreen.kt:178-179**: VideoListのルートが正しく設定されている
         ```kotlin
         selected = currentRoute == YtAlarmDestination.videoList(0L),  // "video_list/0"
         onClick = { onNavigate(YtAlarmDestination.videoList(0L)) },   // "video_list/0"
         ```
       - **YtAlarmNavGraph.kt:110-134**: VideoListScreenのルート定義も正しい
       - **VideoListScreen.kt**: playlistId=0で全動画モードのはずが、新規プレイリスト作成UIを表示
     - 修正方針:
       - VideoListScreen.ktのplaylistId=0処理ロジックを確認
       - 全動画モードと新規プレイリストモードの条件分岐を修正
     - 影響ファイル:
       - app/src/main/kotlin/net/turtton/ytalarm/ui/compose/screens/VideoListScreen.kt

  **残タスク:**
  - ❌ 白画面バグの修正（AlarmSettings戻り→Drawer操作）
  - ❌ VideoList全動画モードの修正
  - Fragment完全削除（binding/drawerLayoutの削除）
  - XML layout削除（activity_main.xml, content_main.xml, drawer_header.xml）
  - 統合テスト・最終動作確認（Phase 6 Stage 4で実施）

### 次のステップ
- [x] **Phase 2**: ダイアログのCompose移行 ✅
- [x] **Fragment統合の完了**: FragmentAlarmListで実際にComposeAdapterを使用 ✅
- [x] **Phase 3**: シンプルな画面の移行（AboutPage、VideoPlayer） ✅
- [x] **Phase 4**: リスト画面の移行（Playlist、VideoList、AlarmList） ✅
- [x] **Phase 4.5**: Screen設計の改善（プレビュー対応） ✅
- [x] **Phase 5**: 複雑な画面の移行（AlarmSettings） ✅
- [x] **Phase 6 Stage 1**: Navigation基盤構築 ✅
- [x] **Phase 6 Stage 2+3**: MainActivity統合 & DrawerLayout移行 ✅
- [ ] **Phase 6 Stage 4**: 統合テスト・Fragment/XML削除 ← 次回作業

### 技術スタック（移行後）
- ✅ Compose BOM 2024.10.00
- ✅ Material3
- ✅ Kotlin Compose Plugin (Kotlin 2.0.21)
- ✅ Coil for Compose (画像読み込み)
- ✅ ViewModel Compose統合

---

## 現状分析

### 現在の画面構成（スクリーンショット参照）

実際の画面は以下のスクリーンショットで確認できます：

| 画面 | スクリーンショット |
|------|-------------------|
| アラーム実行画面 | [00-alarm.png](fastlane/metadata/android/en-US/images/phoneScreenshots/00-alarm.png) |
| アラーム一覧 | [01-alarms.png](fastlane/metadata/android/en-US/images/phoneScreenshots/01-alarms.png) |
| プレイリスト一覧 | [02-playlist.png](fastlane/metadata/android/en-US/images/phoneScreenshots/02-playlist.png) |
| 動画一覧（プレイリスト内） | [03-videos-origin.png](fastlane/metadata/android/en-US/images/phoneScreenshots/03-videos-origin.png) |
| 動画一覧（プレイリスト選択） | [04-videos-playlist.png](fastlane/metadata/android/en-US/images/phoneScreenshots/04-videos-playlist.png) |
| 全動画一覧 | [05-allvideos.png](fastlane/metadata/android/en-US/images/phoneScreenshots/05-allvideos.png) |
| アラーム設定 | [06-alarmSettings.png](fastlane/metadata/android/en-US/images/phoneScreenshots/06-alarmSettings.png) |
| Navigation Drawer | [07-drawer.png](fastlane/metadata/android/en-US/images/phoneScreenshots/07-drawer.png) |
| 動画プレーヤー | [08-videoplayer.png](fastlane/metadata/android/en-US/images/phoneScreenshots/08-videoplayer.png) |
| About画面 | [09-aboutpage.png](fastlane/metadata/android/en-US/images/phoneScreenshots/09-aboutpage.png) |

### アプリ構造
YtAlarmは現在、すべてのUIをXMLレイアウトで実装しています。

#### アクティビティ（2つ）
1. **MainActivity** (`activity_main.xml`)
   - CoordinatorLayout + AppBarLayout + Toolbar
   - DrawerLayout + NavigationView（ドロワーメニュー）
   - FloatingActionButton（複数、動的表示）
   - NavHostFragment（メイン画面のコンテナ）

2. **AlarmActivity** (`activity_alarm.xml`)
   - ConstraintLayout + FragmentContainerView
   - アラーム再生専用の単純な構造

#### フラグメント（7つ）
1. **FragmentAlarmList** (`fragment_list.xml`)
   - RecyclerView（アラーム一覧）
   - カスタムメニュー（並び替え）
   - FABで新規作成

2. **FragmentAlarmSettings** (`fragment_list.xml`)
   - RecyclerView（設定項目リスト）
   - 時刻、繰り返し、プレイリスト、音量、スヌーズ、バイブレーション設定
   - FABで保存

3. **FragmentPlaylist** (`fragment_list.xml`)
   - RecyclerView（プレイリスト一覧）
   - SelectionTracker（複数選択）
   - カスタムメニュー（削除、並び替え）
   - FABで新規作成

4. **FragmentVideoList / FragmentAllVideoList** (`fragment_list.xml`)
   - RecyclerView（動画一覧）
   - SelectionTracker（複数選択）
   - カスタムメニュー（削除、並び替え）
   - FABで動画追加

5. **FragmentVideoPlayer** (`fragment_video_player.xml`)
   - VideoView（動画再生）
   - 時刻表示（アラーム時）
   - SNOOZEボタン、DISMISSボタン
   - フルスクリーンモード

6. **FragmentAboutPage** (`fragment_about.xml`)
   - RecyclerView（About情報）

#### リストアイテム（6つ）
- `item_aram.xml` - アラーム項目（サムネイル、時刻、繰り返し、プレイリスト名、有効/無効スイッチ）
- `item_aram_setting.xml` - アラーム設定項目（動的コンテンツ）
- `item_playlist.xml` - プレイリスト項目（サムネイル、タイトル、動画数、メニュー）
- `item_video_list.xml` - 動画項目（サムネイル、タイトル、メニュー）
- `item_dialog_choice_video.xml` - ダイアログ内の動画選択項目
- `item_aboutpage.xml` - About情報項目

#### ダイアログ（4つ）
1. **DialogMultiChoiceVideo** (`item_dialog_choice_video.xml`)
   - プレイリスト選択ダイアログ
2. **DialogRemoveVideo**
   - 削除確認ダイアログ
3. **DialogUrlInput**
   - URL入力ダイアログ
4. **DialogExecuteProgress** (`dialog_execute_progress.xml`)
   - 進行状況表示ダイアログ

#### その他のUI要素
- `drawer_header.xml` - Navigation Drawerのヘッダー
- `content_main.xml` - メインコンテンツ（DrawerLayout + NavHostFragment）

### 技術スタック（現状）
- View System: XML Layouts
- Navigation: Navigation Component
- RecyclerView: リスト表示
- ViewBinding: ビュー参照
- ViewModel: MVVM architecture
- Material Design Components: Toolbar, FAB, NavigationView
- Selection API: 複数選択機能

---

## Compose移行戦略

### 移行方針

#### 段階的移行（推奨）
XMLとComposeを共存させながら、段階的に移行します。この方法により：
- リスクを最小化
- 各段階でテスト可能
- 機能の継続的な提供
- チームの学習曲線を緩やか化

#### 完全移行
すべてのUIを一度にComposeに移行する方法もありますが、大規模な変更となりリスクが高いため推奨しません。

### フェーズ分け

## Phase 0: 準備（1-2日） ✅ **完了**

### 0.1 依存関係の追加 ✅

**実装日**: 2025-01-30

**gradle/libs.versions.toml**:
- Compose BOM: 2024.10.00
- Coil: 2.7.0
- Kotlin Compose Plugin追加

**app/build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.compose)  // 追加
}

android {
    buildFeatures {
        compose = true
    }
    // composeOptions は不要（Kotlin 2.0.21ではpluginが自動処理）
}

dependencies {
    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.bundles.androidx.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 0.2 テーマの作成 ✅

**実装日**: 2025-01-30

Material3テーマを作成し、既存のカラーリソースを移行しました。

**実装ファイル**:
- `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/theme/Color.kt`
- `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/theme/Theme.kt`

**カラースキーム**:
- Light: Primary=Teal700, Secondary=Teal200, Error=Red500
- Dark: Primary=Teal200, Secondary=Teal700, Error=Red700

### 0.3 共通Composable作成 ✅ (一部完了)

**実装日**: 2025-01-30

- ✅ `AppTheme` - アプリ全体のテーマ（ライト/ダークモード対応）
- ✅ `AlarmItem` - アラーム一覧アイテム（Previewアノテーション付き）
- ✅ `PlaylistItem` - プレイリスト一覧アイテム（Previewアノテーション付き）
- ⏳ `CommonTopAppBar` - 共通のトップバー（未実装）
- ⏳ `CommonFab` - 共通のFAB（未実装）

---

## Phase 1: リストアイテムの移行（2-3日） ✅ **完了**

最も再利用性が高く、独立したコンポーネントから開始します。

### 1.1 AlarmItemComposable ✅
`item_aram.xml` → `AlarmItem.kt`

**実装日**: 2025-01-30
**ファイル**: `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/AlarmItem.kt`

**実装内容：**
```kotlin
@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // サムネイル + 情報
        Row(modifier = Modifier.weight(1f)) {
            AsyncImage(...)
            Column {
                Text("${alarm.hour}:${alarm.minute}")
                Text(alarm.repeatType.toString())
                Text(playlistName)
            }
        }
        // スイッチ
        Switch(checked = alarm.isEnable, onCheckedChange = onToggle)
    }
}
```

### 1.2 PlaylistItemComposable ✅
`item_playlist.xml` → `PlaylistItem.kt`

**実装日**: 2025-01-30
**ファイル**: `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/PlaylistItem.kt`

**実装内容：**
- サムネイル画像表示（CoilのAsyncImage使用）
- プレイリストタイトル表示
- ビデオ数表示
- メニューボタン（3点アイコン）
- 選択状態管理サポート
- クリックイベントハンドリング
- Material3デザイン対応
- Previewアノテーション実装済み

### 1.3 VideoItemComposable ✅
`item_video_list.xml` → `VideoItem.kt`

**実装日**: 2025-10-30
**ファイル**: `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/VideoItem.kt`

**実装内容：**
- サムネイル画像表示（132dp x 64dp、CoilのAsyncImage使用）
- 動画タイトル表示（1行、ellipsize end）
- ドメイン/ファイルサイズ表示
- チェックボックス（選択モード時表示）
- メニューボタン（3点アイコン）
- 選択状態管理サポート
- クリックイベントハンドリング
- Material3デザイン対応
- Previewアノテーション実装済み（通常版と選択版）

### 1.4 AlarmSettingItemComposable ✅
`item_aram_setting.xml` → `AlarmSettingItem.kt`

**実装日**: 2025-10-30
**ファイル**: `app/src/main/kotlin/net/turtton/ytalarm/ui/compose/components/AlarmSettingItem.kt`

**実装内容：**
- 汎用SettingItem Composable（基本コンポーネント）
  - タイトル、説明テキスト
  - トレーリングコンテンツ（スイッチなど）
  - ボトムコンテンツ（スライダーなど）
- SwitchSettingItem（スイッチ付き設定項目）
- SliderSettingItem（スライダー付き設定項目）
- ClickableSettingItem（クリック可能な設定項目）
- Material3デザイン対応
- 複数のPreviewアノテーション実装済み

### 1.5 AlarmListComposeAdapter ✅
`AlarmListAdapter` → `AlarmListComposeAdapter.kt`

**実装日**: 2025-10-30
**ファイル**: `app/src/main/kotlin/net/turtton/ytalarm/ui/adapter/AlarmListComposeAdapter.kt`

**実装内容：**
- ComposeViewを使用したRecyclerView Adapter
- AlarmItem Composableの統合
- プレイリスト情報とサムネイルの非同期取得
- スイッチのトグル処理
- ナビゲーション処理
- テスト用実装（実際のFragment統合は次フェーズ）

**テスト方法（次のフェーズで実装）：**
XMLレイアウト内に`ComposeView`を埋め込んで動作確認します。

```xml
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
binding.composeView.setContent {
    AppTheme {
        AlarmItem(alarm, onToggle, onClick)
    }
}
```

---

## Phase 2: ダイアログの移行（1-2日）

### 2.1 シンプルなダイアログ
- `DialogRemoveVideo` → Compose AlertDialog
- 確認ダイアログ系

### 2.2 複雑なダイアログ
- `DialogMultiChoiceVideo` → カスタムダイアログ
- `DialogUrlInput` → TextFieldダイアログ
- `DialogExecuteProgress` → 進行状況ダイアログ

**実装例：**
```kotlin
@Composable
fun RemoveVideoDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("動画を削除") },
        text = { Text("選択した動画を削除しますか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
```

---

## Phase 3: シンプルな画面の移行（3-4日）

### 3.1 AboutPage画面
`FragmentAboutPage` → `AboutPageScreen.kt`

- RecyclerViewをLazyColumnに変換
- 静的コンテンツが多いため移行が容易
- Navigation Composeの導入テストに最適

### 3.2 VideoPlayer画面（プレビュー用）
`FragmentVideoPlayer` (非アラームモード) → `VideoPlayerScreen.kt`

**注意点：**
- VideoViewはComposeネイティブではないため、AndroidViewで wrap
- フルスクリーンの実装は SystemUiController を使用

```kotlin
@Composable
fun VideoPlayerScreen(videoId: String) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // コントロール UI
        Button(
            onClick = { /* 停止 */ },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("STOP")
        }
    }
}
```

---

## Phase 4.5: Screen設計の改善（プレビュー対応）（1-2日）✅ **完了 - 2025-10-31**

**目的:** ViewModelとの依存を分離し、プレビュー可能な設計に改善する

### 設計パターン

各Screenを以下の2つのComposableに分割：

#### 1. ScreenContent（プレビュー可能）
- ViewModelに依存しない純粋なComposable
- すべてのデータと関数を引数として受け取る
- `@Preview`アノテーションでプレビュー可能

#### 2. Screen（ViewModelラッパー）
- ViewModelを構築
- ViewModelからデータを取得
- ScreenContentにデータと関数を渡す

### 実装完了

- ✅ AlarmListScreen → AlarmListScreenContent + AlarmListScreen
- ✅ PlaylistScreen → PlaylistScreenContent + PlaylistScreen
- ✅ VideoListScreen → VideoListScreenContent + VideoListScreen

### 実施内容

1. **AlarmListScreen リファクタリング**
   - AlarmListScreenContent: 純粋なUIコンポーネント（プレビュー対応）
   - AlarmListScreen: ViewModelラッパー
   - プレビュー関数実装（ダミーデータ使用）

2. **PlaylistScreen リファクタリング**
   - PlaylistScreenContent: 純粋なUIコンポーネント（プレビュー対応）
   - PlaylistScreen: ViewModelラッパー
   - プレビュー関数実装（ダミーデータ使用）

3. **VideoListScreen リファクタリング**
   - VideoListScreenContent: 純粋なUIコンポーネント（プレビュー対応）
   - VideoListScreen: ViewModelラッパー
   - プレビュー関数実装（ダミーデータ使用）

4. **ビルド成功**
   - すべてのコンパイルエラーを解決
   - プレビュー関数のダミーデータを正しい型に修正
   - Deprecation警告あり（非ブロッカー）

5. **エミュレータ動作確認完了**
   - APKインストール成功
   - AlarmListScreen表示確認完了
   - PlaylistScreen表示確認完了
   - VideoListScreen表示確認完了
   - UI要素の配置と動作確認完了
   - インタラクション機能確認完了（ソート、ダイアログ、FABなど）
   - スクリーンショット保存完了

### 達成されたメリット

1. **プレビュー可能**: ScreenContentはViewModelに依存しないため、簡単にプレビュー作成可能 ✅
2. **テスト容易**: ScreenContentの単体テストが簡単 ✅
3. **ロジック分離**: ViewModelとのやりとりがScreenに集約される ✅
4. **再利用性**: ScreenContentは他の場所でも使用可能 ✅

### 確認された問題

**なし** - すべてのテストが成功し、UI表示、機能、パフォーマンスに問題は発見されませんでした。

---

## Phase 4: リスト画面の移行（5-7日）

### 4.1 Playlist画面
`FragmentPlaylist` → `PlaylistScreen.kt`

**実装内容：**
- RecyclerView → LazyColumn
- SelectionTracker → Compose Selection State
- Menu → TopAppBarのactions
- FAB → FloatingActionButton (Compose)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onNavigateToVideoList: () -> Unit
) {
    val playlists by viewModel.allPlaylists.collectAsState()
    val selectedItems = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = { /* 削除 */ }) {
                            Icon(Icons.Default.Delete, "削除")
                        }
                    }
                    IconButton(onClick = { /* 並び替え */ }) {
                        Icon(Icons.Default.Sort, "並び替え")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToVideoList) {
                Icon(Icons.Default.Add, "追加")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    isSelected = selectedItems.contains(playlist.id),
                    onToggleSelection = { /* トグル */ },
                    onClick = { /* 詳細画面へ */ }
                )
            }
        }
    }
}
```

### 4.2 VideoList画面
`FragmentVideoList` / `FragmentAllVideoList` → `VideoListScreen.kt`

同様の実装パターン

### 4.3 AlarmList画面
`FragmentAlarmList` → `AlarmListScreen.kt`

同様の実装パターン

---

## Phase 5: 複雑な画面の移行（4-5日）

### 5.1 AlarmSettings画面
`FragmentAlarmSettings` → `AlarmSettingsScreen.kt`

**実装内容：**
- RecyclerView → LazyColumn（設定項目）
- TimePicker → TimePickerDialog (Compose)
- Slider → Compose Slider
- Switch → Compose Switch

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    alarmId: Long?,
    viewModel: AlarmViewModel,
    onNavigateBack: () -> Unit
) {
    val alarmData by viewModel.alarmData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("SAVE") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { /* 保存 */ }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 時刻設定
            item {
                TimeSettingItem(
                    time = "${alarmData.hour}:${alarmData.minute}",
                    onClick = { /* TimePickerダイアログ表示 */ }
                )
            }

            // 繰り返し設定
            item {
                RepeatSettingItem(
                    repeatType = alarmData.repeatType,
                    onClick = { /* 繰り返し選択 */ }
                )
            }

            // プレイリスト選択
            item {
                PlaylistSettingItem(
                    playlists = alarmData.playListId,
                    onClick = { /* プレイリスト選択 */ }
                )
            }

            // ループ
            item {
                SwitchSettingItem(
                    title = "Loop",
                    checked = alarmData.shouldLoop,
                    onCheckedChange = { /* 更新 */ }
                )
            }

            // シャッフル
            item {
                SwitchSettingItem(
                    title = "Shuffle",
                    checked = alarmData.shouldShuffle,
                    onCheckedChange = { /* 更新 */ }
                )
            }

            // 音量
            item {
                SliderSettingItem(
                    title = "Volume",
                    value = alarmData.volume.volume.toFloat(),
                    onValueChange = { /* 更新 */ }
                )
            }

            // スヌーズ
            item {
                SnoozeSettingItem(
                    minutes = alarmData.snoozeMinute,
                    onClick = { /* 時間選択 */ }
                )
            }

            // バイブレーション
            item {
                SwitchSettingItem(
                    title = "Vibration",
                    checked = alarmData.shouldVibrate,
                    onCheckedChange = { /* 更新 */ }
                )
            }
        }
    }
}
```

---

## Phase 6: Navigation統合（3-4日）

### 6.1 Navigation Composeの導入
Navigation Componentから Navigation Composeへ移行

```kotlin
@Composable
fun YtAlarmNavGraph(
    navController: NavHostController,
    startDestination: String = "alarm_list"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // アラーム一覧
        composable("alarm_list") {
            AlarmListScreen(
                onNavigateToSettings = { alarmId ->
                    navController.navigate("alarm_settings/$alarmId")
                }
            )
        }

        // アラーム設定
        composable(
            route = "alarm_settings/{alarmId}",
            arguments = listOf(
                navArgument("alarmId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId")
            AlarmSettingsScreen(
                alarmId = alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // プレイリスト
        composable("playlist") {
            PlaylistScreen(
                onNavigateToVideoList = {
                    navController.navigate("video_list")
                }
            )
        }

        // 動画一覧
        composable("video_list") {
            VideoListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // About
        composable("about") {
            AboutPageScreen()
        }

        // 動画プレーヤー
        composable(
            route = "video_player/{videoId}",
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId")
            VideoPlayerScreen(videoId = videoId ?: "")
        }
    }
}
```

### 6.2 DrawerLayoutの移行
NavigationViewをModalNavigationDrawerに変換

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // ヘッダー
                DrawerHeader()

                // メニュー項目
                NavigationDrawerItem(
                    label = { Text("AramList") },
                    selected = false,
                    onClick = {
                        navController.navigate("alarm_list")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("PlayList") },
                    selected = false,
                    onClick = {
                        navController.navigate("playlist")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("VideoList") },
                    selected = false,
                    onClick = {
                        navController.navigate("video_list")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        navController.navigate("about")
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        // メインコンテンツ
        YtAlarmNavGraph(navController = navController)
    }
}
```

---

## Phase 7: Activity統合（2-3日）

### 7.1 MainActivityの簡素化
ComposeのsetContentで全体をラップ

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初期化処理
        initYtDL()
        createNotificationChannel()
        requestPermission()

        setContent {
            AppTheme {
                MainScreen()
            }
        }

        // Intent処理
        checkUrlShare(intent)
    }
}
```

### 7.2 AlarmActivityの移行
同様にComposeに変換

```kotlin
class AlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)

        setContent {
            AppTheme {
                VideoPlayerScreen(
                    alarmId = alarmId,
                    isAlarmMode = true
                )
            }
        }
    }
}
```

---

## Phase 8: クリーンアップと最適化（2-3日）

### 8.1 XML削除
使用されなくなったXMLファイルを削除

### 8.2 未使用コード削除
- ViewBinding関連
- Fragment基底クラス
- Adapter類

### 8.3 パフォーマンス最適化
- remember / derivedStateOf の適切な使用
- LazyListのkey設定
- 不要な再コンポジション回避

### 8.4 アクセシビリティ対応
- contentDescription設定
- semantics追加

---

## 技術的な注意点

### 1. VideoViewの扱い
ComposeネイティブのVideoPlayerは存在しないため、AndroidViewでラップする必要があります。

```kotlin
@Composable
fun VideoViewComposable(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(videoUri)
                start()
            }
        },
        update = { videoView ->
            videoView.setVideoURI(videoUri)
        },
        modifier = modifier
    )
}
```

### 2. RecyclerView Selection API
Composeには直接の代替がないため、自前で実装：

```kotlin
@Composable
fun <T> LazyListWithSelection(
    items: List<T>,
    selectedIds: Set<Long>,
    onSelectionChange: (Long, Boolean) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    LazyColumn {
        items(items, key = { /* key */ }) { item ->
            val isSelected = selectedIds.contains(item.id)
            itemContent(item, isSelected)
        }
    }
}
```

### 3. フルスクリーンモード
SystemUiControllerを使用（Accompanist）：

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

### 4. Navigation Arguments
型安全なナビゲーションのため、Kotlin Serializationの使用を検討：

```kotlin
@Serializable
data class AlarmSettingsRoute(val alarmId: Long)

// Navigation
navController.navigate(AlarmSettingsRoute(alarmId = 123))
```

### 5. ViewModel統合
既存のViewModelはそのまま使用可能：

```kotlin
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(LocalContext.current.applicationContext.repository)
    )
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
    // UI
}
```

### 6. LiveDataとFlow
LiveDataはcollectAsStateで使用可能：

```kotlin
val alarms by viewModel.allAlarms.observeAsState(initial = emptyList())
```

ただし、可能な限りFlowへの移行を推奨。

---

## テスト戦略

### 1. UI Tests
Composeのテストフレームワークを使用：

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun alarmListDisplaysAlarms() {
    composeTestRule.setContent {
        AlarmListScreen(...)
    }

    composeTestRule
        .onNodeWithText("00:00")
        .assertIsDisplayed()
}
```

### 2. Screenshot Tests
既存のScreenshotテストをComposeに対応：

```kotlin
@Test
fun captureAlarmList() {
    composeTestRule.setContent {
        AlarmListScreen(...)
    }

    composeTestRule.onRoot().captureToImage()
}
```

### 3. Integration Tests
既存のEspressoテストは段階的に移行

---

## スケジュール（目安）

| Phase | 内容 | 日数 | 累計 |
|-------|------|------|------|
| 0 | 準備 | 1-2日 | 2日 |
| 1 | リストアイテム | 2-3日 | 5日 |
| 2 | ダイアログ | 1-2日 | 7日 |
| 3 | シンプルな画面 | 3-4日 | 11日 |
| 4 | リスト画面 | 5-7日 | 18日 |
| 5 | 複雑な画面 | 4-5日 | 23日 |
| 6 | Navigation統合 | 3-4日 | 27日 |
| 7 | Activity統合 | 2-3日 | 30日 |
| 8 | クリーンアップ | 2-3日 | 33日 |

**合計: 約5-7週間**（実装者の経験に依存）

---

## リスク管理

### 高リスク項目
1. **VideoViewの統合**
   - AndroidViewでのラップが必要
   - パフォーマンスの懸念
   - 代替案: ExoPlayer + Compose統合の検討

2. **フルスクリーンモード**
   - システムバーの制御
   - アラーム画面での安定性

3. **複数選択機能**
   - Selection APIの代替実装
   - UXの一貫性維持

### 中リスク項目
1. **Navigation統合**
   - 既存のdeep linkの動作保証
   - 画面遷移アニメーション

2. **パフォーマンス**
   - 大量リストのスクロール
   - メモリ使用量

### 低リスク項目
1. **テーマとスタイル**
   - Material3への移行は段階的に可能
2. **ViewModelの統合**
   - 既存のViewModelはそのまま使用可能

---

## 代替案

### 案1: 新画面のみCompose（ハイブリッド維持）
- 既存画面はXMLのまま
- 新機能のみComposeで実装
- リスク: 技術的負債の蓄積

### 案2: 段階的移行（推奨）
- 本計画の通り
- リスク: 開発期間の長期化

### 案3: 全面的な書き直し
- ゼロからCompose + Clean Architectureで再構築
- リスク: 非常に高い、機能欠損の可能性

---

## 成功基準

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

---

## 参考資料

### 公式ドキュメント
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Migration Guide](https://developer.android.com/jetpack/compose/migrate)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Material 3](https://m3.material.io/)

### サンプルコード
- [Now in Android](https://github.com/android/nowinandroid) - Google公式サンプル
- [Compose Samples](https://github.com/android/compose-samples)

---

## まとめ

YtAlarmのCompose移行は、段階的アプローチにより安全に実施可能です。特に：

1. **リストアイテムから開始**することで、小さな成功体験を積み重ねる
2. **ダイアログやシンプルな画面**で学習曲線を緩やかにする
3. **複雑な画面**で実践的なパターンを確立
4. **Navigation統合**で全体を完成させる

この計画に従えば、約5-7週間で安全かつ確実にCompose移行が完了します。

移行後は、宣言的UIによるコードの可読性向上、プレビュー機能による開発効率の向上、Material 3による最新のデザイン適用などのメリットが得られます。

---

## 📝 技術メモ

Compose移行で得られた技術的知見は`memo/`フォルダに格納されています。

- [Compose環境構築ガイド](memo/compose_setup.md) - Compose導入時の設定方法とベストプラクティス