package net.turtton.ytalarm.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * About画面のリストアイテム
 *
 * @param thumbnail サムネイル（URLまたはDrawableリソース）
 * @param title タイトルのリソースID
 * @param details 詳細テキストのリソースID（nullの場合は非表示）
 * @param onClick クリック時のアクション
 */
@Composable
fun AboutPageItem(
    thumbnail: AboutPageThumbnail,
    @StringRes title: Int,
    @StringRes details: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // サムネイル
        when (thumbnail) {
            is AboutPageThumbnail.Url -> {
                AsyncImage(
                    model = thumbnail.url,
                    contentDescription = stringResource(R.string.thumbnail_description),
                    modifier = Modifier.size(50.dp)
                )
            }

            is AboutPageThumbnail.Drawable -> {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = thumbnail.id),
                    contentDescription = stringResource(R.string.thumbnail_description),
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // テキスト情報
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = stringResource(id = title),
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            details?.let {
                Text(
                    text = stringResource(id = it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * AboutPageアイテムのサムネイル型
 */
sealed interface AboutPageThumbnail {
    /**
     * URL指定のサムネイル
     */
    data class Url(val url: String) : AboutPageThumbnail

    /**
     * Drawableリソース指定のサムネイル
     */
    data class Drawable(@DrawableRes val id: Int) : AboutPageThumbnail
}

@Preview(showBackground = true)
@Composable
private fun AboutPageItemPreview() {
    AppTheme {
        AboutPageItem(
            thumbnail = AboutPageThumbnail.Drawable(R.drawable.ic_menu_book),
            title = R.string.item_aboutpage_title_tirdpartylicenses,
            details = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPageItemWithDetailsPreview() {
    AppTheme {
        AboutPageItem(
            thumbnail = AboutPageThumbnail.Drawable(R.drawable.ic_menu_book),
            title = R.string.item_aboutpage_title_github,
            details = R.string.item_aboutpage_details_github,
            onClick = {}
        )
    }
}