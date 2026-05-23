package net.turtton.ytalarm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.turtton.ytalarm.kernel.entity.Video
import net.turtton.ytalarm.kernel.error.StreamError
import net.turtton.ytalarm.usecase.UseCaseContainer
import net.turtton.ytalarm.usecase.ReimportResult as UseCaseReimportResult

class VideoViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allVideos: LiveData<List<Video>> by lazy {
        useCaseContainer.getAllVideosFlow().asLiveData()
    }

    fun getFromIdAsync(id: Long): Deferred<Video?> = viewModelScope.async {
        useCaseContainer.getVideoByIdSync(id)
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosByIdsSync(ids)
    }

    fun getFromIds(ids: List<Long>): LiveData<List<Video>> =
        useCaseContainer.getVideosByIdsFlow(ids).asLiveData()

    fun getExceptIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosExceptIdsSync(ids)
    }

    fun getFromVideoIds(ids: List<String>): LiveData<List<Video>> =
        useCaseContainer.getVideosByVideoIdsFlow(ids).asLiveData()

    fun getFromVideoIdAsync(id: String): Deferred<Video?> = viewModelScope.async {
        useCaseContainer.getVideoByVideoIdSync(id)
    }

    fun getFromVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosByVideoIdsSync(ids)
    }

    fun getExceptVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosExceptVideoIdsSync(ids)
    }

    suspend fun update(video: Video): Result<Unit> = try {
        useCaseContainer.updateVideo(video)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to update video: ${video.id}", e)
        Result.failure(e)
    }

    fun insertAsync(video: Video): Deferred<Long> = viewModelScope.async {
        useCaseContainer.insertVideo(video)
    }

    suspend fun insert(videos: List<Video>): Result<Unit> = try {
        useCaseContainer.insertAllVideos(videos)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to insert videos", e)
        Result.failure(e)
    }

    suspend fun delete(video: Video): Result<Unit> = try {
        useCaseContainer.deleteVideo(video)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to delete video: ${video.id}", e)
        Result.failure(e)
    }

    suspend fun delete(videos: List<Video>): Result<Unit> = try {
        useCaseContainer.deleteAllVideos(videos)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to delete videos", e)
        Result.failure(e)
    }

    /**
     * 動画情報を再取得してDBを更新する。
     * UseCaseの[UseCaseReimportResult]をUI向けの[ReimportResult]に変換して返す。
     */
    suspend fun reimportVideo(video: Video): ReimportResult =
        when (useCaseContainer.reimportVideo(video)) {
            is UseCaseReimportResult.Success -> {
                val updated = getFromIdAsync(video.id).await() ?: video
                ReimportResult.Success(updated)
            }

            is UseCaseReimportResult.Error.NoUrl -> ReimportResult.Error.NoUrl

            is UseCaseReimportResult.Error.Network -> ReimportResult.Error.Network

            is UseCaseReimportResult.Error.Parse -> ReimportResult.Error.Parse
        }

    suspend fun getStreamUrl(pageUrl: String, formatSelector: String): Either<StreamError, String> =
        useCaseContainer.getStreamUrl(pageUrl, formatSelector)

    companion object {
        private const val TAG = "VideoViewModel"
    }
}

class VideoViewModelFactory(private val useCaseContainer: UseCaseContainer<*, *, *, *>) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(useCaseContainer) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}