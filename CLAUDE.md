# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

YtAlarmは、YouTubeや他の動画サービスの音声/動画をアラーム音として使用できるAndroidアプリです。Google Play Storeの利用規約に違反しないよう、YouTubeAPIではなくyoutubedl-androidライブラリを使用しています。

See @.github/CONTRIBUTING.md

## 開発環境とコマンド

### 必要環境
- JDK 17
- Android SDK (API 24-34)
- minSdk 24, targetSdk 34, compileSdk 34

### 主要開発コマンド

```bash
# ビルド
./gradlew assembleDebug          # デバッグビルド
./gradlew assembleRelease        # リリースビルド
./gradlew -PnoSplits assembleDebug  # ABI分割なしビルド

# テスト実行
./gradlew test                   # 単体テスト実行
./gradlew connectedAndroidTest   # UIテスト実行
./gradlew check                  # 全チェック実行 (lint, test, detekt含む)
./gradlew :rootCoverageReport    # カバレッジレポート生成

# コード品質チェック
./gradlew ktlintCheck           # Kotlinコードスタイルチェック
./gradlew ktlintFormat          # Kotlinコードフォーマット
./gradlew detekt               # 静的解析実行
./gradlew lintDebug            # Android lint実行
```

## アーキテクチャ

### MVVM + Repository パターン
- **ViewModel**: `AlarmViewModel`, `PlaylistViewModel`, `VideoViewModel`
- **Repository**: `DataRepository` でデータアクセスを抽象化
- **Database**: Room Database (`AppDatabase`) を使用
- **UI**: Fragment + Navigation Component

### 主要コンポーネント構造
```
app/src/main/kotlin/net/turtton/ytalarm/
├── activity/          # AlarmActivity, MainActivity
├── database/          # Room entities, DAOs, converters
├── ui/               # Fragments, Adapters, Dialogs
├── util/             # Extensions, Utils, Order classes
├── viewmodel/        # ViewModels
└── worker/           # Background workers (WorkManager)
```

### データベース設計
- **Entities**: `Alarm`, `Video`, `Playlist`
- **DAOs**: `AlarmDao`, `VideoDao`, `PlaylistDao`
- **Type Converters**: 複雑な型をDB用に変換 (List, Enum, Calendar等)

## 重要な技術仕様

### バージョニング
- セマンティックバージョニング: major.minor.patch
- ABI別ビルド対応 (armeabi-v7a, arm64-v8a, x86, x86_64)
- プロパティ `abiFilters` でABI指定可能

### 主要ライブラリ
- **youtubedl-android**: 動画情報取得
- **Room**: データベース
- **Navigation Component**: 画面遷移
- **WorkManager**: バックグラウンド処理
- **Glide**: 画像読み込み
- **Material Design Components**: UI

### テスト構成
- **Unit Tests**: Kotest (JUnit5スタイル) + Mockito
- **UI Tests**: Espresso + Robolectric
- **Coverage**: JaCoCo使用、カバレッジレポートは `:rootCoverageReport`

## 開発時の注意事項

### コード品質
- ktlint でKotlinコードスタイル統一
- detekt で静的解析
- Lint warning をエラー扱い (`warningsAsErrors = true`)

### テスト実行
- 単体テストは JUnit Platform (JUnit5) 使用
- UI テストは Espresso + screenshot テスト (Fastlane Screengrab)

### ビルド設定
- Java 17 必須
- `android.useAndroidX=true`
- `org.gradle.jvmargs=-Xmx2048m` (メモリ設定)

## 特殊な機能

### ABI分割ビルド
- デフォルトで4つのABI用APKを生成
- `-PnoSplits` オプションで単一APK生成可能
- バージョンコードにABI識別子を含む

### アラーム機能
- 繰り返し設定 (毎日、曜日指定、日付指定)
- 音量設定、ループ再生、スヌーズ機能
- WorkManager によるバックグラウンド処理

### メディア管理
- プレイリストインポート機能
- ストリーミング再生 (ダウンロード機能は開発中)
- 複数の動画サービス対応 (yt-dlp ベース)