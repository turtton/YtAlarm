package net.turtton.ytalarm.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.turtton.ytalarm.R

class DialogRemoveVideo(private val onConfirm: DialogInterface.OnClickListener) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
        .setTitle(R.string.dialog_remove_title)
        .setPositiveButton(R.string.dialog_remove_ok, onConfirm)
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()
}