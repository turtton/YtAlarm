package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.structure.Playlist

class PlaylistViewModel(private val repository: DataRepository) : ViewModel() {
    val allPlaylists: LiveData<List<Playlist>> by lazy { repository.allPlaylists.asLiveData() }

    fun getFromId(id: Int): LiveData<Playlist> {
        return repository.getPlaylistFromId(id).asLiveData()
    }

    fun getFromIdAsync(id: Int): Deferred<Playlist>  = viewModelScope.async {
        repository.getPlaylistFromIdSync(id)
    }

    fun update(playlist: Playlist) = viewModelScope.launch {
        repository.update(playlist)
    }

    fun insert(playlist: Playlist) = viewModelScope.launch {
        repository.insert(playlist)
    }

    fun delete(playlist: Playlist) = viewModelScope.launch {
        repository.delete(playlist)
    }
}

class PlaylistViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(repository) as T
        } else throw IllegalStateException("Unknown ViewModel class")
    }
}