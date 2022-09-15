package net.turtton.ytalarm.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import net.turtton.ytalarm.R
import net.turtton.ytalarm.databinding.DialogExecuteProgressBinding

class DialogExecuteProgress(@StringRes val titleId: Int = R.string.dialog_execute_progress_title) : DialogFragment() {
    private var _binding: DialogExecuteProgressBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExecuteProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireParentFragment().context)
            .setTitle(titleId)
            .setView(R.layout.dialog_execute_progress)
            .create()
    }
}