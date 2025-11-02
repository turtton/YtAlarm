# 技術メモ

Compose移行で得られた技術的知見と、今後の開発で参考になる情報をまとめています。

## 📋 このフォルダのルール

1. **汎用的な名前でファイルを作成しない**
   - ❌ `troubleshooting.md`, `technical_notes.md`, `tips.md`
   - ✅ `asyncimage-drawable-not-showing.md`, `videoview-with-compose.md`

2. **各トピックごとにファイルを追加する**
   - 1ファイル = 1トピック
   - ファイル名から内容が推測できること
   - 検索・編集・削除がしやすいこと

3. **フォルダ階層はネストさせない**
   - すべてのファイルは `memo/` 直下に配置
   - サブフォルダは作成しない

## 📁 ドキュメント一覧

### 環境構築

- **[compose_setup.md](compose_setup.md)** - Compose環境構築ガイド
  - Kotlin 2.0.21 + Compose Compiler設定
  - Compose BOM設定
  - Material3テーマ実装
  - Coil画像読み込み
  - ViewBindingとの共存パターン

### 技術的パターン

- **[videoview-with-compose.md](videoview-with-compose.md)** - VideoViewの扱い
- **[recyclerview-selection-alternative.md](recyclerview-selection-alternative.md)** - RecyclerView Selection APIの代替
- **[fullscreen-mode-compose.md](fullscreen-mode-compose.md)** - フルスクリーンモード
- **[navigation-arguments.md](navigation-arguments.md)** - Navigation Arguments
- **[viewmodel-integration.md](viewmodel-integration.md)** - ViewModel統合
- **[livedata-to-flow.md](livedata-to-flow.md)** - LiveDataとFlow

### トラブルシューティング

- **[asyncimage-drawable-not-showing.md](asyncimage-drawable-not-showing.md)** - AsyncImageでDrawableリソースIDが表示されない
- **[video-thumbnail-placeholder.md](video-thumbnail-placeholder.md)** - Video型サムネイルがプレースホルダーのまま
- **[dialog-selection-not-reflected.md](dialog-selection-not-reflected.md)** - ダイアログ選択内容がUIに反映されない
- **[dialog-content-layout-issue.md](dialog-content-layout-issue.md)** - ダイアログとコンテンツが並列配置でレイアウト崩れ
- **[context-find-activity-null.md](context-find-activity-null.md)** - Context.findActivity()がnullを返す
- **[statusbar-overlapping-toolbar.md](statusbar-overlapping-toolbar.md)** - ステータスバーがToolbarに重なる
- **[switch-tap-area-too-wide.md](switch-tap-area-too-wide.md)** - Switchのタップ範囲が広すぎて誤動作

## 🔗 関連ドキュメント

- [../plan.md](../plan.md) - Compose移行計画全体
- [../CLAUDE.md](../CLAUDE.md) - プロジェクト概要
