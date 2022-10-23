package net.turtton.ytalarm.ui.menu

import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import net.turtton.ytalarm.ui.selection.SelectionTrackerContainer

abstract class SelectionMenuObserver<T, F>(
    protected val fragment: F,
    private val provider: MenuProvider
) : SelectionObserver<T>() where F : Fragment, F : SelectionTrackerContainer<T> {
    private var isAdded = false

    override fun onSelectionChanged() {
        val activity = fragment.requireActivity()
        if (fragment.selectionTracker.hasSelection()) {
            if (!isAdded) {
                if (fragment is MenuProviderContainer) {
                    activity.removeMenuProvider(fragment.menuProvider)
                }
                activity.addMenuProvider(provider, fragment.viewLifecycleOwner)
                isAdded = true
            }
        } else {
            if (fragment is MenuProviderContainer) {
                activity.addMenuProvider(fragment.menuProvider)
            }
            activity.removeMenuProvider(provider)
            isAdded = false
        }
    }

    override fun onSelectionRestored() {
        onSelectionChanged()
    }
}