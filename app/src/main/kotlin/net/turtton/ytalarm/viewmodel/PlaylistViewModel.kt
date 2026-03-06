package net.turtton.ytalarm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.turtton.ytalarm.kernel.entity.Playlist
import net.turtton.ytalarm.usecase.UseCaseContainer

class PlaylistViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allPlaylists: LiveData<List<Playlist>> by lazy {
        useCaseContainer.getAllPlaylistsFlow().asLiveData()
    }

    init {
        // ViewModelインスタンス生成時にサムネイル検証を実行
        viewModelScope.launch {
            // アプリ起動直後のI/O負荷を軽減するため少し遅延
            delay(THUMBNAIL_VALIDATION_DELAY_MS)
            runCatching {
                useCaseContainer.validateAndUpdateThumbnails()
            }.onFailure { e ->
                Log.e(TAG, "Failed to validate thumbnails", e)
            }
        }
    }

    val allPlaylistsAsync: Deferred<List<Playlist>> get() = viewModelScope.async {
        useCaseContainer.getAllPlaylistsSync()
    }

    fun getFromId(id: Long): LiveData<Playlist?> = useCaseContainer.getPlaylistByIdFlow(id)
        .asLiveData()

    fun getFromIdAsync(id: Long): Deferred<Playlist?> = viewModelScope.async {
        useCaseContainer.getPlaylistByIdSync(id)
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Playlist>> = viewModelScope.async {
        useCaseContainer.getPlaylistsByIdsSync(ids)
    }

    fun update(playlist: Playlist) = viewModelScope.launch {
        useCaseContainer.updatePlaylist(playlist)
    }

    fun update(playlists: List<Playlist>) = viewModelScope.launch {
        useCaseContainer.updateAllPlaylists(playlists)
    }

    fun insertAsync(playlist: Playlist): Deferred<Long> = viewModelScope.async {
        useCaseContainer.insertPlaylist(playlist)
    }

    fun delete(playlist: Playlist) = viewModelScope.launch {
        useCaseContainer.deletePlaylist(playlist)
    }

    fun delete(playlists: List<Playlist>) = viewModelScope.launch {
        useCaseContainer.deleteAllPlaylists(playlists)
    }

    companion object {
        private const val TAG = "PlaylistViewModel"
        private const val THUMBNAIL_VALIDATION_DELAY_MS = 500L
    }
}

class PlaylistViewModelFactory(private val useCaseContainer: UseCaseContainer<*, *, *, *>) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(useCaseContainer) as T
        } else {
            error("Unknown ViewModel class")
        }
    }
}