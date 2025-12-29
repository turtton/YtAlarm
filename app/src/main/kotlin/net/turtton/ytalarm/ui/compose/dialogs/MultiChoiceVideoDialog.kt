package net.turtton.ytalarm.ui.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 複数選択可能なビデオ/プレイリスト選択ダイアログ
 *
 * @param T アイテムのID型（Long、String等）
 * @param displayDataList 表示するアイテムのリスト
 * @param initialSelectedIds 初期選択されているアイテムのIDセット
 * @param onConfirm 確認ボタンが押された時の処理（選択されたIDのセットが渡される）
 * @param onDismiss キャンセルまたはダイアログが閉じられた時の処理
 */
@Composable
fun <T> MultiChoiceVideoDialog(
    displayDataList: List<DisplayData<T>>,
    initialSelectedIds: Set<T> = emptySet(),
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIds = remember(displayDataList, initialSelectedIds) {
        val map = mutableStateMapOf<T, Boolean>()
        displayDataList.forEach { data ->
            map[data.id] = initialSelectedIds.contains(data.id)
        }
        map
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = displayDataList,
                    key = { it.id.hashCode() }
                ) { data ->
                    MultiChoiceVideoItem(
                        data = data,
                        isChecked = selectedIds[data.id] ?: false,
                        onCheckedChange = { checked ->
                            selectedIds[data.id] = checked
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = selectedIds
                        .filter { it.value }
                        .keys
                        .toSet()
                    onConfirm(selected)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.dialog_multichoice_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * 複数選択ダイアログ内のアイテム
 */
@Composable
private fun <T> MultiChoiceVideoItem(
    data: DisplayData<T>,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // サムネイル
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(
                        when (val thumbnail = data.thumbnailUrl) {
                            is DisplayDataThumbnail.Url -> thumbnail.url ?: R.drawable.ic_no_image
                            is DisplayDataThumbnail.Drawable -> thumbnail.id
                            null -> R.drawable.ic_no_image
                        }
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = "Item thumbnail",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Crop
            )

            // タイトル
            Text(
                text = data.title,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // チェックボックス
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 表示用データクラス
 *
 * @param T アイテムのID型
 * @param id アイテムのID
 * @param title 表示タイトル
 * @param thumbnailUrl サムネイル画像のURL or Drawable ID
 */
data class DisplayData<T>(val id: T, val title: String, val thumbnailUrl: DisplayDataThumbnail?)

/**
 * サムネイル画像の種類
 */
sealed interface DisplayDataThumbnail {
    /**
     * URLから読み込む画像
     */
    @JvmInline
    value class Url(val url: String?) : DisplayDataThumbnail

    /**
     * Drawableリソースから読み込む画像
     */
    @JvmInline
    value class Drawable(@DrawableRes val id: Int) : DisplayDataThumbnail
}

@Preview(showBackground = true)
@Composable
private fun MultiChoiceVideoDialogPreview() {
    AppTheme {
        MultiChoiceVideoDialog(
            displayDataList = listOf(
                DisplayData(
                    id = 1L,
                    title = "Playlist 1",
                    thumbnailUrl = null
                ),
                DisplayData(
                    id = 2L,
                    title = "Playlist 2",
                    thumbnailUrl = null
                ),
                DisplayData(
                    id = 0L,
                    title = "Create New Playlist",
                    thumbnailUrl = DisplayDataThumbnail.Drawable(R.drawable.ic_add_playlist)
                )
            ),
            initialSelectedIds = setOf(1L),
            onConfirm = {},
            onDismiss = {}
        )
    }
}