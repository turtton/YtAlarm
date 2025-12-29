package net.turtton.ytalarm.ui.compose.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 実行進行状況表示ダイアログ
 *
 * @param titleRes タイトルの文字列リソースID（デフォルトは"Importing…"）
 * @param progress 進行状況（0.0～1.0、nullの場合は不定進行表示）
 * @param eta 推定残り時間のテキスト（nullの場合は非表示）
 * @param onDismissRequest ダイアログを閉じる処理（通常は空実装でOK）
 */
@Composable
fun ExecuteProgressDialog(
    @StringRes titleRes: Int = R.string.dialog_execute_progress_title,
    progress: Float? = null,
    eta: String? = null,
    onDismissRequest: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 進行状況インジケーター
                if (progress == null) {
                    // 不定進行（円形）
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    // 確定進行（水平）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ETA表示
                        if (eta != null) {
                            Text(
                                text = eta,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 進行状況バー
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            // 確認ボタンなし（プログレスダイアログのため）
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ExecuteProgressDialogIndeterminatePreview() {
    AppTheme {
        ExecuteProgressDialog(
            titleRes = R.string.dialog_execute_progress_title,
            progress = null,
            eta = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExecuteProgressDialogDeterminatePreview() {
    AppTheme {
        ExecuteProgressDialog(
            titleRes = R.string.dialog_execute_progress_title,
            progress = 0.6f,
            eta = "2:30 remaining"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExecuteProgressDialogCalculatingPreview() {
    AppTheme {
        ExecuteProgressDialog(
            titleRes = R.string.dialog_execute_progress_title,
            progress = 0.1f,
            eta = stringResource(id = R.string.dialog_execute_progress_eta_init)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExecuteProgressDialogLoadingPreview() {
    AppTheme {
        ExecuteProgressDialog(
            titleRes = R.string.dialog_execute_progress_title_loading,
            progress = null
        )
    }
}