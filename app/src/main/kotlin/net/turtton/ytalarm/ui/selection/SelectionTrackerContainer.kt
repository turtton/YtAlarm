package net.turtton.ytalarm.ui.selection

import androidx.recyclerview.selection.SelectionTracker

interface SelectionTrackerContainer<T> {
    val selectionTracker: SelectionTracker<T>?
}