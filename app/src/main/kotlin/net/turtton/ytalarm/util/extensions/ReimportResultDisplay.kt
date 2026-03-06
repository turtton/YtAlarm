package net.turtton.ytalarm.util.extensions

import android.content.Context
import net.turtton.ytalarm.R
import net.turtton.ytalarm.viewmodel.ReimportResult

/**
 * Returns a human-readable message string for this [ReimportResult] to be shown in the UI.
 *
 * This is an App-layer Extension function that maps the domain-level [ReimportResult]
 * to a user-facing message using Android [Context] resources.
 */
fun ReimportResult.toMessage(context: Context): String = when (this) {
    is ReimportResult.Success -> context.getString(R.string.message_reimport_success)
    is ReimportResult.Error.Parse -> context.getString(R.string.message_reimport_error_parse)
    is ReimportResult.Error.Network -> context.getString(R.string.message_reimport_error_network)
    is ReimportResult.Error.NoUrl -> context.getString(R.string.message_reimport_failed)
}