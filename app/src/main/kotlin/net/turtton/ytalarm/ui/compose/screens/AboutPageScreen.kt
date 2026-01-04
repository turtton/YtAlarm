package net.turtton.ytalarm.ui.compose.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import net.turtton.ytalarm.BuildConfig
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.components.AboutPageItem
import net.turtton.ytalarm.ui.compose.components.AboutPageThumbnail
import net.turtton.ytalarm.ui.compose.theme.AppTheme

// About情報のデータセット
private val AboutItems: List<AboutPageData> = listOf(
    AboutPageData.LinkData(
        thumbnail = AboutPageThumbnail.Url(
            "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
        ),
        title = R.string.item_aboutpage_title_github,
        details = R.string.item_aboutpage_details_github,
        url = "https://github.com/turtton/YtAlarm"
    ),
    AboutPageData.LinkData(
        thumbnail = AboutPageThumbnail.Url(
            "https://www.gnu.org/graphics/gplv3-with-text-136x68.png"
        ),
        title = R.string.item_aboutpage_title_license,
        details = R.string.item_aboutpage_details_license,
        url = "https://github.com/turtton/YtAlarm/blob/HEAD/LICENSE"
    ),
    AboutPageData.LinkData(
        thumbnail = AboutPageThumbnail.Drawable(R.drawable.ic_menu_book),
        title = R.string.item_aboutpage_title_tirdpartylicenses,
        details = null,
        url = "https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm"
    ),
    AboutPageData.LinkData(
        thumbnail = AboutPageThumbnail.Url(
            "https://en.liberapay.com/assets/liberapay/icon-v2_white-on-yellow.200.png?save_as=liberapay_logo_white-on-yellow_200px.png"
        ),
        title = R.string.item_aboutpage_liberapay_title,
        details = R.string.item_aboutpage_liberapay_details,
        url = "https://liberapay.com/turtton/donate"
    ),
    AboutPageData.CopyableData(
        thumbnail = AboutPageThumbnail.Url(
            "https://camo.githubusercontent.com/d8f6d0e0aeafda23077ad6fdccd927fff87e1fff516534465269ebd9cffdaf4b/68747470733a2f2f656e2e626974636f696e2e69742f772f696d616765732f656e2f322f32392f42435f4c6f676f5f2e706e67"
        ),
        title = R.string.item_aboutpage_bitcoin_title,
        details = R.string.item_aboutpage_bitcoin_address,
        clipData = "3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t"
    )
)

/**
 * About画面
 *
 * アプリの情報、ライセンス、寄付リンクなどを表示
 *
 * @param snackbarHostState Snackbar表示用のホスト状態
 */
@Composable
fun AboutPageScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pre-fetch string resources for use in lambdas
    val errorNoBrowser = stringResource(R.string.error_no_browser)
    val errorOpenLink = stringResource(R.string.error_open_link)
    val snackbarCopied = stringResource(R.string.snackbar_copied)
    val snackbarClipboardError = stringResource(R.string.snackbar_error_failed_to_access_clipboard)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // アプリアイコン
        item {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_logo_round),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(100.dp)
            )
        }

        // アプリ名
        item {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
        }

        // バージョン情報
        item {
            val versionText = stringResource(
                R.string.version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
            Text(
                text = versionText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // About情報リスト
        items(
            items = AboutItems,
            key = { it.title }
        ) { item ->
            AboutPageItem(
                thumbnail = item.thumbnail,
                title = item.title,
                details = item.details,
                onClick = {
                    when (item) {
                        is AboutPageData.LinkData -> {
                            // リンクを開く
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                android.util.Log.w("AboutPageScreen", "No browser found", e)
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorNoBrowser)
                                }
                            } catch (e: SecurityException) {
                                android.util.Log.e(
                                    "AboutPageScreen",
                                    "Security error opening link",
                                    e
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorOpenLink)
                                }
                            } catch (e: IllegalArgumentException) {
                                android.util.Log.e("AboutPageScreen", "Invalid URL", e)
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorOpenLink)
                                }
                            }
                        }

                        is AboutPageData.CopyableData -> {
                            // クリップボードにコピー
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as? ClipboardManager
                            if (clipboard != null) {
                                val clipData = ClipData.newPlainText(
                                    "YtAlarmClipData",
                                    item.clipData
                                )
                                clipboard.setPrimaryClip(clipData)
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(snackbarCopied)
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(snackbarClipboardError)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * About画面のデータ型
 */
private sealed interface AboutPageData {
    val thumbnail: AboutPageThumbnail

    @get:StringRes val title: Int

    @get:StringRes val details: Int?

    /**
     * リンク付きデータ
     */
    data class LinkData(
        override val thumbnail: AboutPageThumbnail,
        override val title: Int,
        override val details: Int?,
        val url: String
    ) : AboutPageData

    /**
     * コピー可能なデータ
     */
    data class CopyableData(
        override val thumbnail: AboutPageThumbnail,
        override val title: Int,
        override val details: Int?,
        val clipData: String
    ) : AboutPageData
}

@Preview(showBackground = true)
@Composable
private fun AboutPageScreenPreview() {
    AppTheme {
        AboutPageScreen()
    }
}