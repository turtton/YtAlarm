package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * URL入力ダイアログ
 *
 * @param initialUrl 初期URL文字列（デフォルトは空文字）
 * @param onConfirm 確認ボタンが押された時の処理（入力されたURLが渡される）
 * @param onDismiss キャンセルまたはダイアログが閉じられた時の処理
 */
@Composable
fun UrlInputDialog(initialUrl: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf(initialUrl) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.dialog_video_input_ok))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(text = stringResource(id = R.string.dialog_video_input_url_hint))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (url.isNotBlank()) {
                                onConfirm(url)
                                onDismiss()
                            }
                        }
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(url)
                        onDismiss()
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text(text = stringResource(id = R.string.dialog_video_input_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )

    // ダイアログ表示時にフォーカスを当てる
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
private fun UrlInputDialogPreview() {
    AppTheme {
        UrlInputDialog(
            initialUrl = "",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UrlInputDialogWithTextPreview() {
    AppTheme {
        UrlInputDialog(
            initialUrl = "https://www.youtube.com/watch?v=example",
            onConfirm = {},
            onDismiss = {}
        )
    }
}