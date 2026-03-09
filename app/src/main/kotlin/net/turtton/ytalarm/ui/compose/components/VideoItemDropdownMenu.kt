package net.turtton.ytalarm.ui.compose.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.turtton.ytalarm.R

@Composable
fun VideoItemDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSetThumbnail: () -> Unit,
    onDownload: () -> Unit,
    onReimport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // サムネイルに設定
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_video_list_item_option_set_thumbnail)) },
            onClick = {
                onSetThumbnail()
                onDismiss()
            }
        )

        // ダウンロード
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.menu_video_list_item_option_download),
                    color = if (isDownloaded) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            },
            onClick = {
                if (!isDownloaded) {
                    onDownload()
                    onDismiss()
                }
            },
            enabled = !isDownloaded
        )

        // 再インポート
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_video_list_item_option_reimport)) },
            onClick = {
                onReimport()
                onDismiss()
            }
        )

        HorizontalDivider()

        // 削除（赤文字で表示）
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.menu_video_list_item_option_delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}