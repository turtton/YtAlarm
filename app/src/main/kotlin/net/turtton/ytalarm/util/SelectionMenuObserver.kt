package net.turtton.ytalarm.util

import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver

abstract class SelectionMenuObserver<T, F>(
    protected val fragment: F,
    private val provider: MenuProvider
) : SelectionObserver<T>() where F : Fragment, F : SelectionTrackerContainer<T> {
    private var isAdded = false

    override fun onSelectionChanged() {
        if (fragment.selectionTracker.hasSelection()) {
            if (!isAdded) {
                fragment.requireActivity()
                    .addMenuProvider(provider, fragment.viewLifecycleOwner)
                isAdded = true
            }
        } else {
            fragment.requireActivity().removeMenuProvider(provider)
            isAdded = false
        }
    }

    override fun onSelectionRestored() {
        onSelectionChanged()
    }
}