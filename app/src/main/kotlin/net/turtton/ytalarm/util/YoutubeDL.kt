package net.turtton.ytalarm.util

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.MainActivity

fun AppCompatActivity.initYtDL(view: View) = lifecycleScope.launch {
    withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().init(applicationContext)
        }
    }.onFailure {
        launch(Dispatchers.Main) {
            Snackbar.make(
                view,
                "Internal error occurred.",
                Snackbar.LENGTH_LONG
            ).setAction("Action", null)
                .show()
            Log.e(MainActivity.APP_TAG, "YtDL initialization failed", it)
        }
    }
}