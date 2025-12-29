package net.turtton.ytalarm.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 基本的な設定項目のComposable
 * 名前、説明、および任意のトレーリングコンテンツ（スイッチなど）を表示
 */
@Composable
fun SettingItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Column(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }

        if (bottomContent != null) {
            bottomContent()
        }
    }
}

/**
 * スイッチ付き設定項目
 */
@Composable
fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    SettingItem(
        title = title,
        description = description,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = modifier
    )
}

/**
 * スライダー付き設定項目
 */
@Composable
fun SliderSettingItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0
) {
    SettingItem(
        title = title,
        description = description,
        bottomContent = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        },
        modifier = modifier
    )
}

/**
 * クリック可能な設定項目（時刻選択、プレイリスト選択など）
 */
@Composable
fun ClickableSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingItem(
        title = title,
        description = description,
        onClick = onClick,
        modifier = modifier
    )
}

// プレビュー

@Preview(showBackground = true)
@Composable
private fun SwitchSettingItemPreview() {
    AppTheme {
        SwitchSettingItem(
            title = "Loop",
            description = "Repeat playlist when finished",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SliderSettingItemPreview() {
    AppTheme {
        SliderSettingItem(
            title = "Volume",
            description = "Adjust alarm volume",
            value = 50f,
            onValueChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClickableSettingItemPreview() {
    AppTheme {
        ClickableSettingItem(
            title = "Time",
            description = "07:30",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingItemWithoutDescriptionPreview() {
    AppTheme {
        SwitchSettingItem(
            title = "Vibration",
            description = null,
            checked = false,
            onCheckedChange = {}
        )
    }
}