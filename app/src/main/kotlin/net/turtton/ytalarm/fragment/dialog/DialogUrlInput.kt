package net.turtton.ytalarm.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.R
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.VideoInformation
import net.turtton.ytalarm.viewmodel.VideoViewContainer
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

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

        private val json = Json { ignoreUnknownKeys = true }

        fun <F> F.showVideoImportDialog(
            view: View,
            defaultInput: String = "",
            onSuccess: (Video) -> Unit = {}
        ) where F : Fragment, F : VideoViewContainer {
            val editText = EditText(context)
            editText.setText(defaultInput, TextView.BufferType.EDITABLE)
            DialogUrlInput(editText) {
                val url = editText.text.toString()
                val processDialog = DialogExecuteProgress()
                processDialog.isCancelable = false
                processDialog.showNow(childFragmentManager, "ImportProgress")
                val request = YoutubeDLRequest(url)
                    .addOption("--dump-json")
                    .addOption("-f", "b")
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, _ ->
                                val binding = processDialog.binding
                                binding.dialogExecuteProgressBarRound.visibility = View.GONE
                                val progressBar = binding.dialogExecuteProgressBar
                                progressBar.visibility = View.VISIBLE
                                progressBar.progress = (progress * 100).roundToInt()
                                val duration = etaInSeconds.seconds
                                val etaText = if (duration.inWholeHours > 0) {
                                    val minutes = duration.inWholeMinutes
                                    resources.getQuantityString(
                                        R.plurals.dialog_execute_progress_eta_minutes,
                                        minutes.toInt(),
                                        minutes
                                    )
                                } else {
                                    getString(
                                        R.string.dialog_execute_progress_eta,
                                        duration.inWholeMinutes,
                                        duration.inWholeSeconds
                                    )
                                }
                                val progressEta = binding.dialogExecuteProgressEta
                                progressEta.visibility = View.VISIBLE
                                progressEta.text = etaText
                            }
                        }
                    }.andThen {
                        val output = it.out
                        runCatching {
                            json.decodeFromString<VideoInformation>(output)
                        }
                    }.onSuccess {
                        launch(Dispatchers.Main) {
                            processDialog.dismiss()
                            var video = Video(
                                it.id,
                                it.fullTitle,
                                it.thumbnailUrl,
                                it.url,
                                it.videoUrl,
                                it.domain
                            )
                            if (it.videoUrl.startsWith("http")) {
                                videoViewModel.insert(video)
                            } else {
                                @Suppress("ktlint:")
                                Snackbar.make(
                                    view,
                                    "This Video supports only downloading",
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction("Action", null).show()
                                // TODO("Create download process")
                                video = video.copy(internalLink = "localstorage", fileSize = 0)
                            }
                            onSuccess(video)
                        }
                    }.onFailure { throwable ->
                        launch(Dispatchers.Main) {
                            Snackbar.make(view, "Failed to import", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show()
                            Log.e("FragmentAllVideo", "Failed to import video data", throwable)
                            processDialog.dismissNow()
                            showVideoImportDialog(view, url, onSuccess)
                        }
                    }
                }
            }.show(childFragmentManager, "UrlInput")
        }
    }
}