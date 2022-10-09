package net.turtton.ytalarm.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import net.turtton.ytalarm.R
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import net.turtton.ytalarm.worker.VideoInfoDownloadWorker

class DialogUrlInput(
    private val editText: EditText,
    val onConfirmImportDialog: (dialog: DialogUrlInput) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        editText.hint = getString(R.string.dialog_video_input_url_hint)
        return AlertDialog.Builder(context)
            .setView(editText)
            .setPositiveButton(R.string.dialog_video_input_ok) { _, _ ->
                onConfirmImportDialog(this)
            }.setNegativeButton(R.string.dialog_video_input_cancel) { _, _ -> }
            .create()
    }

    companion object {
        fun <F> F.showVideoImportDialog(
            view: View,
            playlistId: Long? = null
        ) where F : Fragment, F : VideoViewContainer {
            val editText = EditText(context)
            DialogUrlInput(editText) {
                val url = editText.text.toString()
                val targetId = playlistId?.let { longArrayOf(it) } ?: longArrayOf()
                VideoInfoDownloadWorker
                    .registerWorker(view.context, url, targetId)
                Snackbar.make(view, R.string.snackbar_start_download, 600).show()
            }.show(childFragmentManager, "UrlInput")
        }
    }
}