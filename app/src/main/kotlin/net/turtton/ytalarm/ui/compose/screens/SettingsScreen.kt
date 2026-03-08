package net.turtton.ytalarm.ui.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication
import net.turtton.ytalarm.ui.compose.components.ClickableSettingItem
import net.turtton.ytalarm.ui.compose.components.SwitchSettingItem
import net.turtton.ytalarm.ui.compose.dialogs.UpdateChannelDialog
import net.turtton.ytalarm.ui.compose.dialogs.UpdateChannelSelection
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.appSettings
import net.turtton.ytalarm.util.extensions.downloadStorageLimitBytes
import net.turtton.ytalarm.util.extensions.downloadWifiOnly
import net.turtton.ytalarm.util.extensions.ytDlpUpdateChannel

/**
 * 設定画面
 *
 * yt-dlp更新チャンネルなどのアプリ設定を管理
 *
 * @param onNavigateBack 戻るボタン押下時のコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val preferences = context.appSettings

    // 現在の更新チャンネル
    var currentChannel by remember {
        val savedChannel = preferences.ytDlpUpdateChannel
        mutableStateOf(
            when (savedChannel) {
                "NIGHTLY" -> UpdateChannelSelection.NIGHTLY
                else -> UpdateChannelSelection.STABLE
            }
        )
    }

    // ダウンロード設定の状態
    var wifiOnly by remember { mutableStateOf(preferences.downloadWifiOnly) }
    var storageLimitBytes by remember { mutableLongStateOf(preferences.downloadStorageLimitBytes) }
    val scope = rememberCoroutineScope()

    val useCaseContainer = remember {
        (context.applicationContext as? YtApplication)?.dataContainerProvider?.getUseCaseContainer()
    }
    var totalDownloadSize by remember {
        mutableLongStateOf(useCaseContainer?.getTotalDownloadSize() ?: 0L)
    }

    // ダイアログ表示状態
    var showUpdateChannelDialog by remember { mutableStateOf(false) }
    var showStorageLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // yt-dlp セクションヘッダー
            item {
                SectionHeader(title = stringResource(R.string.settings_ytdlp_section))
            }

            // 更新チャンネル設定
            item {
                val channelDisplayName = when (currentChannel) {
                    UpdateChannelSelection.STABLE -> stringResource(
                        R.string.settings_ytdlp_channel_stable
                    )

                    UpdateChannelSelection.NIGHTLY -> stringResource(
                        R.string.settings_ytdlp_channel_nightly
                    )
                }

                ClickableSettingItem(
                    title = stringResource(R.string.settings_ytdlp_update_channel),
                    description = channelDisplayName,
                    onClick = { showUpdateChannelDialog = true }
                )
            }

            // ダウンロードセクションヘッダー
            item {
                SectionHeader(title = stringResource(R.string.settings_download_section))
            }

            // Wi-Fiのみトグル
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.settings_download_wifi_only),
                    description = stringResource(R.string.settings_download_wifi_only_description),
                    checked = wifiOnly,
                    onCheckedChange = {
                        wifiOnly = it
                        preferences.downloadWifiOnly = it
                    }
                )
            }

            // 容量上限
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.settings_download_storage_limit),
                    description = formatBytes(storageLimitBytes),
                    onClick = { showStorageLimitDialog = true }
                )
            }

            // 合計ダウンロード容量
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.settings_download_total_size),
                    description = formatBytes(totalDownloadSize),
                    onClick = {}
                )
            }

            // 一括削除
            item {
                ClickableSettingItem(
                    title = stringResource(R.string.settings_download_delete_all),
                    description = stringResource(
                        R.string.settings_download_delete_all_description
                    ),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            useCaseContainer?.deleteAllDownloads()
                            val newSize = useCaseContainer?.getTotalDownloadSize() ?: 0L
                            withContext(Dispatchers.Main) {
                                totalDownloadSize = newSize
                            }
                        }
                    }
                )
            }
        }

        // 更新チャンネル選択ダイアログ
        if (showUpdateChannelDialog) {
            UpdateChannelDialog(
                currentChannel = currentChannel,
                onChannelSelected = { selected ->
                    currentChannel = selected
                    preferences.ytDlpUpdateChannel = selected.name
                    showUpdateChannelDialog = false
                },
                onDismiss = { showUpdateChannelDialog = false }
            )
        }

        // ストレージ上限選択ダイアログ
        if (showStorageLimitDialog) {
            StorageLimitDialog(
                currentLimitBytes = storageLimitBytes,
                onLimitSelected = { selectedBytes ->
                    storageLimitBytes = selectedBytes
                    preferences.downloadStorageLimitBytes = selectedBytes
                    showStorageLimitDialog = false
                },
                onDismiss = { showStorageLimitDialog = false }
            )
        }
    }
}

/**
 * セクションヘッダー
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider()
    }
}

private val STORAGE_LIMIT_OPTIONS = listOf(
    512L * 1024 * 1024 to "512 MB",
    1L * 1024 * 1024 * 1024 to "1 GB",
    2L * 1024 * 1024 * 1024 to "2 GB",
    5L * 1024 * 1024 * 1024 to "5 GB",
    10L * 1024 * 1024 * 1024 to "10 GB"
)

@Composable
private fun StorageLimitDialog(
    currentLimitBytes: Long,
    onLimitSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_download_storage_limit)) },
        text = {
            Column {
                STORAGE_LIMIT_OPTIONS.forEach { (bytes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLimitSelected(bytes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentLimitBytes == bytes,
                            onClick = { onLimitSelected(bytes) }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.0f MB", mb)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen()
    }
}