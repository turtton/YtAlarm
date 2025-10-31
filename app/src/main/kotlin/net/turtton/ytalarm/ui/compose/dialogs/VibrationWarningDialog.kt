package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

@Composable
fun VibrationWarningDialog(
    onOpenIssue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = "Warning")
        },
        title = { Text(stringResource(R.string.dialog_vibration_waring_title)) },
        text = { Text(stringResource(R.string.dialog_vibration_waring_message)) },
        confirmButton = {
            TextButton(onClick = {
                onOpenIssue()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_vibration_warning_show_issue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun VibrationWarningDialogPreview() {
    AppTheme {
        VibrationWarningDialog(
            onOpenIssue = {},
            onDismiss = {}
        )
    }
}
