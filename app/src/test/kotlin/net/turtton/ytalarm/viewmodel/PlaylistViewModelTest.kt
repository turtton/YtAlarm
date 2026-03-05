package net.turtton.ytalarm.viewmodel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.turtton.ytalarm.TestUseCaseContainer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import net.turtton.ytalarm.kernel.entity.Playlist as DomainPlaylist

@Suppress("UNUSED")
class PlaylistViewModelTest :
    FunSpec({
        val domainPlaylist1 = DomainPlaylist(
            id = 1L,
            title = "Test Playlist 1",
            thumbnail = DomainPlaylist.Thumbnail.None,
            videos = emptyList(),
            type = DomainPlaylist.Type.Original
        )
        val domainPlaylist2 = DomainPlaylist(
            id = 2L,
            title = "Test Playlist 2",
            thumbnail = DomainPlaylist.Thumbnail.None,
            videos = listOf(1L, 2L),
            type = DomainPlaylist.Type.CloudPlaylist(
                url = "https://example.com/playlist",
                syncRule = DomainPlaylist.SyncRule.ALWAYS_ADD
            )
        )
        val domainPlaylists = listOf(domainPlaylist1, domainPlaylist2)
        val playlistFlow = MutableStateFlow(domainPlaylists)

        val mockUseCaseContainer = mock<TestUseCaseContainer> {
            on { getAllPlaylistsFlow() } doReturn playlistFlow
        }

        context("PlaylistViewModelFactory") {
            test("creates PlaylistViewModel instance") {
                val factory = PlaylistViewModelFactory(mockUseCaseContainer)
                val viewModel = factory.create(PlaylistViewModel::class.java)
                viewModel.shouldBeInstanceOf<PlaylistViewModel>()
            }

            test("factory creates different instances for different calls") {
                val factory = PlaylistViewModelFactory(mockUseCaseContainer)
                val viewModel1 = factory.create(PlaylistViewModel::class.java)
                val viewModel2 = factory.create(PlaylistViewModel::class.java)
                viewModel1.shouldBeInstanceOf<PlaylistViewModel>()
                viewModel2.shouldBeInstanceOf<PlaylistViewModel>()
            }
        }
    })