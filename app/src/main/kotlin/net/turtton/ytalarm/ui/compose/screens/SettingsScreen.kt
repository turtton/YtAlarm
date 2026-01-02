package net.turtton.ytalarm.ui.compose.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.components.ClickableSettingItem
import net.turtton.ytalarm.ui.compose.dialogs.UpdateChannelDialog
import net.turtton.ytalarm.ui.compose.dialogs.UpdateChannelSelection
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import net.turtton.ytalarm.util.extensions.appSettings
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

    // ダイアログ表示状態
    var showUpdateChannelDialog by remember { mutableStateOf(false) }

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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen()
    }
}