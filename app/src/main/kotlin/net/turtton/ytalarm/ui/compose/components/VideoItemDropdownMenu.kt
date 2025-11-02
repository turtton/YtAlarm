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
import net.turtton.ytalarm.database.structure.Video

@Composable
fun VideoItemDropdownMenu(
    video: Video,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSetThumbnail: (Video) -> Unit,
    onDownload: (Video) -> Unit,
    onReimport: (Video) -> Unit,
    onDelete: (Video) -> Unit,
    modifier: Modifier = Modifier
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
                onSetThumbnail(video)
                onDismiss()
            }
        )

        // ダウンロード
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_video_list_item_option_download)) },
            onClick = {
                onDownload(video)
                onDismiss()
            }
        )

        // 再インポート
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_video_list_item_option_reimport)) },
            onClick = {
                onReimport(video)
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
                onDelete(video)
                onDismiss()
            }
        )
    }
}
