package net.turtton.ytalarm.ui.compose.dialogs

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDatePickerDialog(
    initialDateMillis: Long? = null,
    onConfirm: (dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(millis)
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview
@Composable
fun AlarmDatePickerDialogPreview() {
    AppTheme {
        AlarmDatePickerDialog(
            initialDateMillis = System.currentTimeMillis(),
            onConfirm = {},
            onDismiss = {}
        )
    }
}