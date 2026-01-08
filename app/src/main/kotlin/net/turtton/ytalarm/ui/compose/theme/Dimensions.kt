package net.turtton.ytalarm.ui.compose.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Common dimension values used across the app.
 */
object Dimensions {
    /**
     * Bottom padding to prevent content from being overlapped by FAB.
     * Calculated as: FAB size (56dp) + margin (16dp) = 72dp
     */
    val FabBottomPadding = 72.dp

    /**
     * Returns PaddingValues with bottom padding to prevent FAB overlap.
     * Use this for LazyColumn contentPadding.
     */
    fun fabContentPadding() = PaddingValues(bottom = FabBottomPadding)
}