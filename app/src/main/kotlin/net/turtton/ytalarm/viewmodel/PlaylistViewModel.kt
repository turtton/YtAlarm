package net.turtton.ytalarm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.R
import net.turtton.ytalarm.database.structure.Playlist
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.util.extensions.updateDate

class PlaylistViewModel(private val repository: DataRepository) : ViewModel() {
    val allPlaylists: LiveData<List<Playlist>> by lazy { repository.allPlaylists.asLiveData() }

    init {
        // ViewModelインスタンス生成時にサムネイル検証を実行
        viewModelScope.launch {
            // アプリ起動直後のI/O負荷を軽減するため少し遅延
            kotlinx.coroutines.delay(THUMBNAIL_VALIDATION_DELAY_MS)
            validateAndUpdateThumbnails()
        }
    }

    val allPlaylistsAsync: Deferred<List<Playlist>> get() = viewModelScope.async {
        repository.getAllPlaylistsSync()
    }

    fun getFromId(id: Long): LiveData<Playlist> = repository.getPlaylistFromId(id).asLiveData()

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

    /**
     * プレイリスト一覧のサムネイルを検証し、無効なサムネイルを持つプレイリストを更新する。
     * サムネイルとして設定されている動画が存在しない場合、プレイリスト内の他の動画にフォールバックする。
     * 他の動画も存在しない場合は、デフォルト画像に設定する。
     */
    private suspend fun validateAndUpdateThumbnails() {
        withContext(Dispatchers.IO) {
            val playlists = repository.getAllPlaylistsSync()
            // 1件ずつ検証・更新することで部分的な失敗に対応
            playlists.forEach { playlist ->
                runCatching {
                    validateThumbnail(playlist)?.let { updated ->
                        repository.update(updated.updateDate())
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to validate/update thumbnail", e)
                }
            }
        }
    }

    private suspend fun validateThumbnail(playlist: Playlist): Playlist? {
        val thumbnail = playlist.thumbnail
        if (thumbnail !is Playlist.Thumbnail.Video) return null

        // 現在のサムネイル動画が存在し、有効な状態かチェック
        val originalVideo = runCatching {
            repository.getVideoFromIdSync(thumbnail.id)
        }.onFailure { e ->
            Log.e(TAG, "Failed to get video for thumbnail validation", e)
        }.getOrNull()

        // 動画が存在し、Information状態であれば有効
        if (originalVideo != null && originalVideo.stateData is Video.State.Information) {
            return null
        }

        // フォールバック: プレイリスト内の他の動画を一括取得して探す
        val candidateIds = playlist.videos.filter { it != thumbnail.id }
        if (candidateIds.isEmpty()) {
            return playlist.copy(thumbnail = Playlist.Thumbnail.Drawable(R.drawable.ic_no_image))
        }

        val fallbackVideoId = runCatching {
            val existingVideos = repository.getVideoFromIdsSync(candidateIds)
            // Information状態の動画のみを候補とする
            existingVideos.firstOrNull { it.stateData is Video.State.Information }?.id
        }.onFailure { e ->
            Log.e(TAG, "Failed to get fallback videos", e)
        }.getOrNull()

        val newThumbnail = if (fallbackVideoId != null) {
            Playlist.Thumbnail.Video(fallbackVideoId)
        } else {
            Playlist.Thumbnail.Drawable(R.drawable.ic_no_image)
        }

        return playlist.copy(thumbnail = newThumbnail)
    }

    companion object {
        private const val TAG = "PlaylistViewModel"
        private const val THUMBNAIL_VALIDATION_DELAY_MS = 500L
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