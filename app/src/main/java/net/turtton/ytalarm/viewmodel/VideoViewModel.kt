package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.structure.Video

class VideoViewModel(private val repository: DataRepository) : ViewModel() {
    val allVideos: LiveData<List<Video>> by lazy { repository.allVideos.asLiveData() }

    fun getFromIds(ids: List<String>): LiveData<List<Video>> {
        return repository.getVideoFromIds(ids).asLiveData()
    }

    fun getFromIdAsync(id: String): Deferred<Video?> = viewModelScope.async {
        repository.getVideoFromIdSync(id)
    }

    fun getExceptIdsAsync(ids: List<String>): Deferred<List<Video>> = viewModelScope.async {
        repository.getVideoExceptIdsSync(ids)
    }

    fun insert(video: Video) = viewModelScope.launch {
        repository.insert(video)
    }

    fun delete(video: Video) = viewModelScope.launch {
        repository.delete(video)
    }
}

class VideoViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(repository) as T
        } else {
            throw IllegalStateException("Unknown ViewModel class")
        }
    }
}

interface VideoViewContainer {
    val videoViewModel: VideoViewModel
}