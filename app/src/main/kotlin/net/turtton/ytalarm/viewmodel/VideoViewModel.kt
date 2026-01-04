package net.turtton.ytalarm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.util.VideoInformation
import java.io.IOException
import java.net.UnknownHostException

class VideoViewModel(private val repository: DataRepository) : ViewModel() {
    val allVideos: LiveData<List<Video>> by lazy { repository.allVideos.asLiveData() }

    fun getFromIdAsync(id: Long): Deferred<Video?> = viewModelScope.async {
        repository.getVideoFromIdSync(id)
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        repository.getVideoFromIdsSync(ids)
    }

    fun getFromIds(ids: List<Long>): LiveData<List<Video>> =
        repository.getVideoFromIds(ids).asLiveData()

    fun getExceptIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        repository.getVideoExceptIdsSync(ids)
    }

    fun getFromVideoIds(ids: List<String>): LiveData<List<Video>> =
        repository.getVideoFromVideoIds(ids).asLiveData()

    fun getFromVideoIdAsync(id: String): Deferred<Video?> = viewModelScope.async {
        repository.getVideoFromVideoIdSync(id)
    }

    fun getFromVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        repository.getVideoFromVideoIdsSync(ids)
    }

    fun getExceptVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        repository.getVideoExceptVideoIdsSync(ids)
    }

    fun update(video: Video) = viewModelScope.launch {
        repository.update(video)
    }

    fun insertAsync(video: Video): Deferred<Long> = viewModelScope.async {
        repository.insert(video)
    }

    fun insert(videos: List<Video>) = viewModelScope.launch {
        repository.insert(videos)
    }

    fun delete(video: Video) = viewModelScope.launch {
        repository.delete(video)
    }

    fun delete(videos: List<Video>) = viewModelScope.launch {
        repository.deleteVideoLists(videos)
    }

    suspend fun reimportVideo(video: Video): ReimportResult = withContext(Dispatchers.IO) {
        try {
            val url = video.videoUrl.ifEmpty {
                (video.stateData as? Video.State.Importing)
                    ?.state
                    ?.let { it as? Video.WorkerState.Failed }
                    ?.url
                    ?: return@withContext ReimportResult.Error.NoUrl
            }
            val request = YoutubeDLRequest(url)
                .addOption("--dump-single-json")
                .addOption("-f", "b")
            val result = YoutubeDL.getInstance().execute(request) { _, _, _ -> }

            val json = Json { ignoreUnknownKeys = true }
            val videoInfo = json.decodeFromString<VideoInformation>(result.out)
            val newVideo = videoInfo.toVideo()

            val updatedVideo = newVideo.copy(
                id = video.id,
                creationDate = video.creationDate
            )
            repository.update(updatedVideo)
            ReimportResult.Success(updatedVideo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Log.e(TAG, "JSON parse error: ${video.videoId}", e)
            ReimportResult.Error.Parse
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error: ${video.videoId}", e)
            ReimportResult.Error.Network
        } catch (e: IOException) {
            Log.e(TAG, "IO error: ${video.videoId}", e)
            ReimportResult.Error.IO
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "YoutubeDL error: ${video.videoId}", e)
            ReimportResult.Error.Downloader
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Reimport failed: ${video.videoId}", e)
            ReimportResult.Error.NoUrl
        }
    }

    companion object {
        private const val TAG = "VideoViewModel"
    }
}

class VideoViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(repository) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}

interface VideoViewContainer {
    val videoViewModel: VideoViewModel
}