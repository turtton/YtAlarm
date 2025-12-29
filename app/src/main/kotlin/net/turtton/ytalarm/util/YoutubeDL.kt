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
import kotlinx.serialization.Serializable
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.util.serializer.VideoInformationSerializer

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
            ).show()
            Log.e(MainActivity.APP_TAG, "YtDL initialization failed", it)
        }
    }
}

@Serializable(with = VideoInformationSerializer::class)
data class VideoInformation(
    val id: String,
    val title: String? = null,
    val url: String,
    val domain: String,
    val typeData: Type
) {
    fun toVideo(): Video {
        check(typeData is Type.Video) { "failed to convert video due to typeData mismatch" }
        return Video(
            0,
            id,
            typeData.fullTitle,
            typeData.thumbnailUrl,
            url,
            domain,
            Video.State.Information(typeData.videoUrl.startsWith("http"))
        )
    }

    sealed interface Type {
        data class Video(val fullTitle: String, val thumbnailUrl: String, val videoUrl: String) :
            Type

        data class Playlist(val entries: List<VideoInformation>) : Type
    }
}