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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.usecase.UseCaseContainer
import net.turtton.ytalarm.util.extensions.toDomain
import net.turtton.ytalarm.util.extensions.toLegacy
import net.turtton.ytalarm.util.extensions.updateDate

class PlaylistViewModel(private val useCaseContainer: UseCaseContainer<*, *, *, *>) : ViewModel() {
    val allPlaylists: LiveData<List<Playlist>> by lazy {
        useCaseContainer.getAllPlaylistsFlow()
            .map { list -> list.map { it.toLegacy() } }
            .asLiveData()
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
        useCaseContainer.getAllPlaylistsSync().map { it.toLegacy() }
    }

    fun getFromId(id: Long): LiveData<Playlist?> = useCaseContainer.getPlaylistByIdFlow(id)
        .map { it?.toLegacy() }
        .asLiveData()

    fun getFromIdAsync(id: Long): Deferred<Playlist?> = viewModelScope.async {
        useCaseContainer.getPlaylistByIdSync(id)?.toLegacy()
    }

    fun getFromIdsAsync(ids: List<Long>): Deferred<List<Playlist>> = viewModelScope.async {
        useCaseContainer.getPlaylistsByIdsSync(ids).map { it.toLegacy() }
    }

    fun update(playlist: Playlist) = viewModelScope.launch {
        useCaseContainer.updatePlaylist(playlist.updateDate().toDomain())
    }

    fun update(playlists: List<Playlist>) = viewModelScope.launch {
        useCaseContainer.updateAllPlaylists(playlists.map { it.updateDate().toDomain() })
    }

    fun insertAsync(playlist: Playlist): Deferred<Long> = viewModelScope.async {
        useCaseContainer.insertPlaylist(playlist.toDomain())
    }

    fun delete(playlist: Playlist) = viewModelScope.launch {
        useCaseContainer.deletePlaylist(playlist.toDomain())
    }

    fun delete(playlists: List<Playlist>) = viewModelScope.launch {
        useCaseContainer.deleteAllPlaylists(playlists.map { it.toDomain() })
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