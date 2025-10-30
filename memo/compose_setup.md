# Compose環境構築ガイド

このドキュメントは、既存のAndroidプロジェクトにJetpack Composeを導入する際の設定方法と技術的知見をまとめたものです。

---

## Kotlin 2.0.21 + Compose Compiler

Kotlin 2.0.21では、Compose Compilerがプラグインとして統合されています。

### 設定方法

**gradle/libs.versions.toml**:
```toml
[versions]
kotlin = "2.0.21"

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

**app/build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.compose)
}

android {
    buildFeatures {
        compose = true
    }
    // composeOptions は不要（プラグインが自動処理）
}
```

### ポイント

- `composeOptions { kotlinCompilerExtensionVersion = "..." }` は不要
- プラグイン適用だけで自動的に最適化される
- 以前のバージョンとの互換性問題が解消されている

---

## Compose BOM

Compose BOMを使用することで、依存関係の管理が容易になります。

### 設定方法

**gradle/libs.versions.toml**:
```toml
[versions]
compose-bom = "2024.10.00"
coil = "2.7.0"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material = { group = "androidx.compose.material", name = "material" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

[bundles]
androidx-compose = [
    "androidx-compose-ui",
    "androidx-compose-ui-graphics",
    "androidx-compose-ui-tooling-preview",
    "androidx-compose-material3",
    "androidx-compose-material"
]
```

**app/build.gradle.kts**:
```kotlin
dependencies {
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

### メリット

- 個別のバージョン指定が不要
- 互換性が保証されている
- 依存関係の競合が最小限

---

## Material3テーマ

### 既存カラーリソースからの移行

**colors.xml → Color.kt**:
```kotlin
package net.turtton.ytalarm.ui.compose.theme

import androidx.compose.ui.graphics.Color

val Teal200 = Color(0xFF25E2C3)
val Teal700 = Color(0xFF009666)
val Red500 = Color(0xFFFF384D)
val Red700 = Color(0xFFE22545)
```

### テーマ定義

**Theme.kt**:
```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Teal200,
    secondary = Teal700,
    error = Red700
)

private val LightColorScheme = lightColorScheme(
    primary = Teal700,
    secondary = Teal200,
    error = Red500
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

### ポイント

- `isSystemInDarkTheme()`で自動的にライト/ダーク切り替え
- `lightColorScheme()`と`darkColorScheme()`で個別定義
- 既存のXMLテーマと共存可能

---

## 画像読み込み: Coil

ComposeではCoilを使用することを推奨します。

### 理由

- Compose専用の`AsyncImage`コンポーネント
- Jetpack Composeとの統合が優れている
- Kotlin Coroutines対応
- 軽量で高速

### 使用例

```kotlin
import coil.compose.AsyncImage

@Composable
fun ThumbnailImage(url: Any?) {
    AsyncImage(
        model = url ?: R.drawable.ic_no_image,
        contentDescription = "Thumbnail",
        modifier = Modifier.size(64.dp),
        contentScale = ContentScale.Crop
    )
}
```

### 既存GlideとThe共存

- XMLベースのUIではGlideを継続使用
- ComposeベースのUIではCoilを使用
- 段階的に移行可能

---

## Previewアノテーション

開発効率を大幅に向上させるため、必ず実装してください。

### 基本パターン

```kotlin
@Preview(showBackground = true)
@Composable
private fun AlarmItemPreview() {
    AppTheme {
        AlarmItem(
            alarm = Alarm(
                id = 1L,
                hour = 7,
                minute = 30,
                repeatType = Alarm.RepeatType.Everyday,
                isEnable = true
            ),
            playlistTitle = "Morning Playlist",
            thumbnailUrl = null,
            onToggle = {},
            onClick = {}
        )
    }
}
```

### ポイント

- Preview関数は`private`にする（公開APIにしない）
- `showBackground = true`で背景を表示
- サンプルデータを用意する必要がある
- Android Studioで即座にプレビュー表示可能

---

## ViewBindingとの共存

段階的移行のため、ViewBindingとComposeの共存パターンを確立します。

### ComposeViewの埋め込み

**XML (fragment_list.xml)**:
```xml
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**Kotlin (Fragment)**:
```kotlin
binding.composeView.setContent {
    AppTheme {
        AlarmItem(
            alarm = alarm,
            playlistTitle = playlistTitle,
            thumbnailUrl = thumbnailUrl,
            onToggle = { /* ... */ },
            onClick = { /* ... */ }
        )
    }
}
```

### 方針

- 既存のFragmentではViewBindingを継続使用
- ComposeViewを段階的に埋め込む
- 完全移行後にViewBindingを削除

---

## ViewModel統合

既存のViewModelはそのまま使用可能です。

### Flow購読パターン

```kotlin
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(
            LocalContext.current.applicationContext.repository
        )
    )
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

    LazyColumn {
        items(alarms, key = { it.id }) { alarm ->
            AlarmItem(/* ... */)
        }
    }
}
```

### LiveDataの場合

```kotlin
val alarms by viewModel.allAlarms.observeAsState(initial = emptyList())
```

ただし、可能な限りFlowへの移行を推奨します。

---

## よくある問題と解決策

### ステータスバーオーバーラップ

**問題**: ツールバーがステータスバーに埋もれる

**原因**: `fitsSystemWindows`属性が設定されていない

**解決策**:
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">
```

### ComposeとXMLのテーマ不一致

**問題**: Composeコンポーネントの色が既存UIと異なる

**解決策**: 既存の`colors.xml`から同じ色をComposeテーマに移行する

---

## パフォーマンス考慮事項

### remember と derivedStateOf

```kotlin
@Composable
fun ExpensiveComposable(items: List<Item>) {
    // ❌ 悪い例: 毎回計算される
    val filteredItems = items.filter { it.isActive }

    // ✅ 良い例: itemsが変更された時のみ計算
    val filteredItems = remember(items) {
        items.filter { it.isActive }
    }

    // ✅ さらに良い例: derivedStateOfを使用
    val filteredItems by remember {
        derivedStateOf { items.filter { it.isActive } }
    }
}
```

### LazyColumnのkey設定

```kotlin
LazyColumn {
    items(
        items = alarms,
        key = { it.id }  // 必ず設定する
    ) { alarm ->
        AlarmItem(alarm)
    }
}
```

keyを設定することで、リストの更新時にパフォーマンスが向上します。

---

## 推奨ディレクトリ構成

```
app/src/main/kotlin/[package]/ui/compose/
├── theme/
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt (必要に応じて)
├── components/
│   ├── AlarmItem.kt
│   ├── PlaylistItem.kt
│   └── ...
├── screens/
│   ├── AlarmListScreen.kt
│   ├── PlaylistScreen.kt
│   └── ...
└── util/
    └── Preview.kt (Preview用のサンプルデータ)
```
