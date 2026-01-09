package net.turtton.ytalarm.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 時刻を大きく表示するコンポーネント
 *
 * @param hour 時（0-23）
 * @param minute 分（0-59）
 * @param onTimeClick 時刻部分がクリックされたときのコールバック
 * @param modifier Modifier
 */
@Composable
fun AlarmTimeDisplay(
    hour: Int,
    minute: Int,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTimeClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d:%02d".format(hour, minute),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onTimeClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.setting_time),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmTimeDisplayPreview() {
    AppTheme {
        AlarmTimeDisplay(
            hour = 7,
            minute = 30,
            onTimeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmTimeDisplayAfternoonPreview() {
    AppTheme {
        AlarmTimeDisplay(
            hour = 14,
            minute = 5,
            onTimeClick = {}
        )
    }
}