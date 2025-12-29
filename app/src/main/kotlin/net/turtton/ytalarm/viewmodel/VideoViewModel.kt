package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.database.structure.Video

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