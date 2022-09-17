package net.turtton.ytalarm.util

import androidx.recyclerview.selection.SelectionTracker

interface SelectionTrackerContainer<T> {
    val selectionTracker: SelectionTracker<T>
}