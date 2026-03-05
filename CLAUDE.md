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
./gradlew :koverXmlReport        # カバレッジレポート生成

# コード品質チェック
./gradlew ktlintCheck           # Kotlinコードスタイルチェック
./gradlew ktlintFormat          # Kotlinコードフォーマット
./gradlew detekt               # 静的解析実行
./gradlew lintDebug            # Android lint実行
```

## アーキテクチャ

### Clean Architecture（マルチモジュール）

4モジュール構成で、依存方向は Kernel → DataSource → UseCase → App:

```
:kernel       ← 依存なし（純粋Kotlin + kotlinx.coroutines, serialization, datetime, Arrow）
:datasource   ← :kernel（+ Room, YoutubeDL）
:usecase      ← :kernel
:app          ← :kernel, :usecase, :datasource
```

#### Kernel層 (:kernel)
- ドメインEntity（`Alarm`, `Video`, `Playlist`）— 全フィールドimmutable、kotlinx.datetime使用
- Repository<Executor>インターフェース（`AlarmRepository`, `VideoRepository`, `PlaylistRepository`, `VideoInfoRepository`）
- DI基盤（`DataSource<Executor>`, `DependsOn*`, `LocalDataSourceContainer`, `RemoteDataSourceContainer`）
- プラットフォームPort（`AlarmSchedulerPort`）
- ドメインロジック（`AlarmScheduling.kt` — toNextFireTime, pickNearestTime等）

#### DataSource層 (:datasource)
- Room Entity（`AlarmEntity`, `VideoEntity`, `PlaylistEntity`）+ ドメイン↔Roomマッパー
- Room DAO実装
- Repository実装（`RoomAlarmRepository`等、Executor = `AppDatabase`）
- `YtDlpVideoInfoRepository`（Executor = `YtDlpExecutor`）
- Room Migration v1→v2（BLOB列のCBOR構造変更）

#### UseCase層 (:usecase)
- 4つのUseCaseインターフェース（where句 + defaultメソッド）:
  - `AlarmUseCase` — アラーム操作・スケジュール管理
  - `PlaylistUseCase` — プレイリスト操作・サムネイル管理
  - `VideoUseCase` — 動画操作・GC
  - `ImportUseCase` — 動画/プレイリストインポート・同期
- `UseCaseContainer` — 全UseCase合成インターフェース

#### App層 (:app)
- `DataContainerProvider` + `DefaultDataContainerProvider`（DI配線、`YtApplication`に保持）
- ViewModel（`UseCaseContainer<*, *, *, *>`で型消去して受け取り）
- Worker（`CoroutineWorker`直接継承、`DataContainerProvider`経由でUseCase取得）
- `AndroidAlarmScheduler`（`AlarmSchedulerPort`実装）
- UI表示マッピング（`RepeatType.getDisplay()`, `DayOfWeek.getDisplay()`, `Thumbnail.toDrawableRes()`）

### DIパターン（Executorパターン）
- Repositoryメソッドに毎回Executor型パラメータを渡す
- ローカルExecutor: `AppDatabase`、リモートExecutor: `YtDlpExecutor`
- `DependsOn*`インターフェースで依存を型として表現
- `DataContainerProvider`だけが具象型を知る（単一配線点）

### 移行状態の注意
- 旧`database.structure.*`、旧`database.dao.*`、旧`database.AppDatabase`はUI層から多数参照されており残存
- 新コードは`kernel.entity.*`を使用。旧型は段階的に除去予定

### モジュール別テスト・ビルドコマンド
```bash
./gradlew :kernel:build        # Kernel層ビルド+テスト
./gradlew :datasource:build    # DataSource層ビルド+テスト
./gradlew :usecase:build       # UseCase層ビルド+テスト
```

### 主要コンポーネント構造
```
kernel/src/main/kotlin/net/turtton/ytalarm/kernel/
├── entity/        # ドメインEntity (Alarm, Video, Playlist, AlarmScheduling)
├── dto/           # VideoInformation DTO
├── error/         # VideoInfoError, StreamError
├── repository/    # Repository<Executor>インターフェース
├── di/            # DataSource, DependsOn*, Containers
└── port/          # AlarmSchedulerPort

datasource/src/main/kotlin/net/turtton/ytalarm/datasource/
├── entity/        # Room Entity (AlarmEntity, VideoEntity, PlaylistEntity)
├── mapper/        # ドメイン↔Roomマッパー
├── dao/           # Room DAO
├── repository/    # Repository実装
├── converter/     # TypeConverters
├── serializer/    # CBOR/JSON Serializer
├── local/         # RoomDataSource, AppDatabase, Migration
└── remote/        # YtDlpExecutor, YtDlpDataSource

usecase/src/main/kotlin/net/turtton/ytalarm/usecase/
├── AlarmUseCase.kt, PlaylistUseCase.kt, VideoUseCase.kt, ImportUseCase.kt
└── UseCaseContainer.kt

app/src/main/kotlin/net/turtton/ytalarm/
├── activity/      # AlarmActivity, MainActivity
├── di/            # DataContainerProvider
├── platform/      # AndroidAlarmScheduler
├── database/      # [旧] Room entities, DAOs, converters (段階的に除去予定)
├── ui/            # Compose Screens, Dialogs
├── util/          # Extensions (RepeatTypeDisplay, ThumbnailDisplay等), Order classes
├── viewmodel/     # ViewModels (UseCaseContainer経由)
└── worker/        # Workers (DataContainerProvider経由)
```

### データベース設計
- **ドメインEntity**: `kernel.entity.Alarm`, `kernel.entity.Video`, `kernel.entity.Playlist`
- **Room Entity**: `datasource.entity.AlarmEntity`, `VideoEntity`, `PlaylistEntity`
- **DAOs**: `datasource.dao.AlarmDao`, `VideoDao`, `PlaylistDao`
- **Room DB Version**: 2（Migration v1→v2でBLOB列のCBOR構造変更）

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
- **kotlinx.datetime**: 日時処理（Kernel層）
- **kotlinx.serialization**: シリアライゼーション（CBOR/JSON）
- **Arrow**: 関数型エラーハンドリング（Either）

### テスト構成
- **Unit Tests**: Kotest (JUnit5スタイル) + Mockito
- **UI Tests**: Espresso + Robolectric
- **Coverage**: Kover使用、カバレッジレポートは `:koverXmlReport`

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