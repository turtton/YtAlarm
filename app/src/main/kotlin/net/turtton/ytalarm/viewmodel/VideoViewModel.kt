package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.usecase.UseCaseContainer
import net.turtton.ytalarm.util.extensions.toDomain
import net.turtton.ytalarm.util.extensions.toLegacy
import net.turtton.ytalarm.usecase.ReimportResult as UseCaseReimportResult

class VideoViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allVideos: LiveData<List<Video>> by lazy {
        useCaseContainer.getAllVideosFlow()
            .map { list -> list.map { it.toLegacy() } }
            .asLiveData()
    }

    fun getFromIdAsync(id: Long): Deferred<Video?> = viewModelScope.async {
        useCaseContainer.getVideoByIdSync(id)?.toLegacy()
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosByIdsSync(ids).map { it.toLegacy() }
    }

    fun getFromIds(ids: List<Long>): LiveData<List<Video>> =
        useCaseContainer.getVideosByIdsFlow(ids)
            .map { list -> list.map { it.toLegacy() } }
            .asLiveData()

    fun getExceptIdsAsync(ids: List<Long>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosExceptIdsSync(ids).map { it.toLegacy() }
    }

    fun getFromVideoIds(ids: List<String>): LiveData<List<Video>> =
        useCaseContainer.getVideosByVideoIdsFlow(ids)
            .map { list -> list.map { it.toLegacy() } }
            .asLiveData()

    fun getFromVideoIdAsync(id: String): Deferred<Video?> = viewModelScope.async {
        useCaseContainer.getVideoByVideoIdSync(id)?.toLegacy()
    }

    fun getFromVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosByVideoIdsSync(ids).map { it.toLegacy() }
    }

    fun getExceptVideoIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        useCaseContainer.getVideosExceptVideoIdsSync(ids).map { it.toLegacy() }
    }

    fun update(video: Video) = viewModelScope.launch {
        useCaseContainer.updateVideo(video.toDomain())
    }

    fun insertAsync(video: Video): Deferred<Long> = viewModelScope.async {
        useCaseContainer.insertVideo(video.toDomain())
    }

    fun insert(videos: List<Video>) = viewModelScope.launch {
        useCaseContainer.insertAllVideos(videos.map { it.toDomain() })
    }

    fun delete(video: Video) = viewModelScope.launch {
        useCaseContainer.deleteVideo(video.toDomain())
    }

    fun delete(videos: List<Video>) = viewModelScope.launch {
        useCaseContainer.deleteAllVideos(videos.map { it.toDomain() })
    }

    /**
     * 動画情報を再取得してDBを更新する。
     * UseCaseの[UseCaseReimportResult]をUI向けの[ReimportResult]に変換して返す。
     */
    suspend fun reimportVideo(video: Video): ReimportResult =
        when (useCaseContainer.reimportVideo(video.toDomain())) {
            is UseCaseReimportResult.Success -> {
                val updated = getFromIdAsync(video.id).await() ?: video
                ReimportResult.Success(updated)
            }

            is UseCaseReimportResult.Error.NoUrl -> ReimportResult.Error.NoUrl

            is UseCaseReimportResult.Error.Network -> ReimportResult.Error.Network

            is UseCaseReimportResult.Error.Parse -> ReimportResult.Error.Parse
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