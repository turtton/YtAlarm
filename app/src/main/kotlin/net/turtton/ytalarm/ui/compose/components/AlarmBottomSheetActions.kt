package net.turtton.ytalarm.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * ボトムシート下部の削除・保存ボタン
 *
 * @param isNewAlarm 新規作成の場合true（削除ボタンを非表示にする）
 * @param onDelete 削除ボタンがクリックされたときのコールバック
 * @param onSave 保存ボタンがクリックされたときのコールバック
 * @param modifier Modifier
 */
@Composable
fun AlarmBottomSheetActions(
    isNewAlarm: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isNewAlarm) {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(R.string.button_delete))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onSave) {
            Text(text = stringResource(R.string.ok))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmBottomSheetActionsExistingPreview() {
    AppTheme {
        AlarmBottomSheetActions(
            isNewAlarm = false,
            onDelete = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmBottomSheetActionsNewPreview() {
    AppTheme {
        AlarmBottomSheetActions(
            isNewAlarm = true,
            onDelete = {},
            onSave = {}
        )
    }
}