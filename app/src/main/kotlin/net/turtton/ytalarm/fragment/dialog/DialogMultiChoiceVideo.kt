package net.turtton.ytalarm.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.turtton.ytalarm.R
import net.turtton.ytalarm.adapter.MultiChoiceVideoListAdapter

class DialogMultiChoiceVideo<T>(
    private val displayDataList: List<MultiChoiceVideoListAdapter.DisplayData<T>>,
    private val chosenTargets: List<Boolean> = displayDataList.map { false },
    val confirmImportDialog: (DialogMultiChoiceVideo<T>, selectedId: Set<T>) -> Unit = { _, _ -> }
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val recyclerView = RecyclerView(requireParentFragment().requireContext())

        val adapter = MultiChoiceVideoListAdapter(displayDataList, chosenTargets)
        recyclerView.layoutManager = LinearLayoutManager(requireParentFragment().context)
        recyclerView.adapter = adapter

        return AlertDialog.Builder(requireParentFragment().context)
            .setView(recyclerView)
            .setPositiveButton(R.string.dialog_multichoice_ok) { _, _ ->
                confirmImportDialog(this, adapter.selectedId)
            }.setNegativeButton(R.string.dialog_multichoice_cancel) { _, _ -> }
            .create()
    }
}