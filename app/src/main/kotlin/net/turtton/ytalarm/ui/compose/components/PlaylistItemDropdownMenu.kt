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
import net.turtton.ytalarm.database.structure.Playlist

@Composable
fun PlaylistItemDropdownMenu(
    playlist: Playlist,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    isDeleteEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // 名称変更
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_playlist_option_rename)) },
            onClick = {
                onRename(playlist)
                onDismiss()
            }
        )

        HorizontalDivider()

        // 削除（有効時は赤文字、無効時は灰色で表示）
        // 無効時もクリック可能にして、呼び出し側でメッセージを表示
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.menu_playlist_option_delete),
                    color = if (isDeleteEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            },
            onClick = {
                onDelete(playlist)
                onDismiss()
            }
        )
    }
}