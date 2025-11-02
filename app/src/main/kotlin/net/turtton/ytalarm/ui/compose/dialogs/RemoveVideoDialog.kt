package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 動画削除確認ダイアログ
 *
 * @param onConfirm 確認ボタンが押された時の処理
 * @param onDismiss キャンセルまたはダイアログが閉じられた時の処理
 */
@Composable
fun RemoveVideoDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.dialog_remove_title))
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.dialog_remove_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RemoveVideoDialogPreview() {
    AppTheme {
        RemoveVideoDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}