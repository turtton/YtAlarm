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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.ui.compose.theme.AppTheme

@Composable
fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    thumbnailUrl: Any?,
    videoCount: Int,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onClick: () -> Unit,
    menuExpanded: Boolean = false,
    onMenuClick: () -> Unit = {},
    onMenuDismiss: () -> Unit = {},
    menuContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
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
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl ?: R.drawable.ic_no_image)
                    .crossfade(true)
                    .build(),
                contentDescription = "Playlist thumbnail",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = playlist.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$videoCount videos",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Preview(showBackground = true)
@Composable
private fun PlaylistItemPreview() {
    AppTheme {
        PlaylistItem(
            playlist = Playlist(
                id = 1L,
                title = "My Favorite Songs"
            ),
            thumbnailUrl = null,
            videoCount = 15,
            onClick = {},
            onMenuClick = {}
        )
    }
}