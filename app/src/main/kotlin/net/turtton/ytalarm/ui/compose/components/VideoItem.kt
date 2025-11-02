package net.turtton.ytalarm.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.ui.compose.theme.AppTheme
import java.util.Calendar

@Composable
fun VideoItem(
    video: Video,
    domainOrSize: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onClick: () -> Unit,
    menuExpanded: Boolean = false,
    onMenuClick: () -> Unit = {},
    onMenuDismiss: () -> Unit = {},
    menuContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.thumbnailUrl.ifEmpty { R.drawable.ic_no_image },
                contentDescription = "Video thumbnail",
                modifier = Modifier.size(width = 132.dp, height = 64.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = domainOrSize,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
            }

            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                menuContent?.invoke()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoItemPreview() {
    AppTheme {
        VideoItem(
            video = Video(
                id = 1L,
                videoId = "test123",
                title = "Sample Video Title That Is Very Long And Should Be Truncated",
                thumbnailUrl = "",
                videoUrl = "https://example.com/video",
                domain = "youtube.com",
                stateData = Video.State.Information(isStreamable = true),
                creationDate = Calendar.getInstance()
            ),
            domainOrSize = "youtube.com",
            isSelected = false,
            showCheckbox = false,
            onClick = {},
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoItemSelectedPreview() {
    AppTheme {
        VideoItem(
            video = Video(
                id = 1L,
                videoId = "test123",
                title = "Selected Video",
                thumbnailUrl = "",
                videoUrl = "https://example.com/video",
                domain = "youtube.com",
                stateData = Video.State.Information(isStreamable = true),
                creationDate = Calendar.getInstance()
            ),
            domainOrSize = "youtube.com",
            isSelected = true,
            showCheckbox = true,
            onClick = {},
            onMenuClick = {}
        )
    }
}