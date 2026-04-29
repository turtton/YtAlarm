package net.turtton.ytalarm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
            try {
                useCaseContainer.validateAndUpdateThumbnails()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
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

    suspend fun update(playlist: Playlist): Result<Unit> = try {
        useCaseContainer.updatePlaylist(playlist)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to update playlist: ${playlist.id}", e)
        Result.failure(e)
    }

    suspend fun update(playlists: List<Playlist>): Result<Unit> = try {
        useCaseContainer.updateAllPlaylists(playlists)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to update playlists", e)
        Result.failure(e)
    }

    fun insertAsync(playlist: Playlist): Deferred<Long> = viewModelScope.async {
        useCaseContainer.insertPlaylist(playlist)
    }

    suspend fun delete(playlist: Playlist): Result<Unit> = try {
        useCaseContainer.deletePlaylist(playlist)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to delete playlist: ${playlist.id}", e)
        Result.failure(e)
    }

    suspend fun delete(playlists: List<Playlist>): Result<Unit> = try {
        useCaseContainer.deleteAllPlaylists(playlists)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Log.e(TAG, "Failed to delete playlists", e)
        Result.failure(e)
    }

    suspend fun removeVideosFromPlaylist(playlist: Playlist, videoIds: List<Long>): Result<Unit> =
        try {
            useCaseContainer.removeVideosFromPlaylist(playlist, videoIds)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Failed to remove videos from playlist: ${playlist.id}", e)
            Result.failure(e)
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