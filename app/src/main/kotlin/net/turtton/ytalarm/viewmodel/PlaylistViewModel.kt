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
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.util.extensions.updateDate

class PlaylistViewModel(private val repository: DataRepository) : ViewModel() {
    val allPlaylists: LiveData<List<Playlist>> by lazy { repository.allPlaylists.asLiveData() }

    val allPlaylistsAsync: Deferred<List<Playlist>> get() = viewModelScope.async {
        repository.getAllPlaylistsSync()
    }

    fun getFromId(id: Long): LiveData<Playlist> {
        return repository.getPlaylistFromId(id).asLiveData()
    }

    fun getFromIdAsync(id: Long): Deferred<Playlist?> = viewModelScope.async {
        repository.getPlaylistFromIdSync(id)
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Playlist>> = viewModelScope.async {
        repository.getPlaylistFromIdsSync(ids)
    }

    fun update(playlist: Playlist) = viewModelScope.launch {
        repository.update(playlist.updateDate())
    }

    fun update(playlists: List<Playlist>) = viewModelScope.launch {
        repository.update(playlists.updateDate())
    }

    fun insertAsync(playlist: Playlist): Deferred<Long> = viewModelScope.async {
        repository.insert(playlist)
    }

    fun delete(playlist: Playlist) = viewModelScope.launch {
        repository.delete(playlist)
    }

    fun delete(playlists: List<Playlist>) = viewModelScope.launch {
        repository.deletePlaylists(playlists)
    }
}

class PlaylistViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(repository) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}

interface PlaylistViewContainer {
    val playlistViewModel: PlaylistViewModel
}