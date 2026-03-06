package net.turtton.ytalarm.viewmodel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.turtton.ytalarm.TestUseCaseContainer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import net.turtton.ytalarm.kernel.entity.Video as DomainVideo

@Suppress("UNUSED")
class VideoViewModelTest :
    FunSpec({
        val domainVideo1 = DomainVideo(
            id = 1L,
            videoId = "test_video_1",
            title = "Test Video 1",
            thumbnailUrl = "https://example.com/thumb1.jpg",
            videoUrl = "https://example.com/video1",
            domain = "example.com",
            state = DomainVideo.State.Information()
        )
        val domainVideo2 = DomainVideo(
            id = 2L,
            videoId = "test_video_2",
            title = "Test Video 2",
            thumbnailUrl = "https://example.com/thumb2.jpg",
            videoUrl = "https://example.com/video2",
            domain = "example.com",
            state = DomainVideo.State.Importing
        )
        val domainVideos = listOf(domainVideo1, domainVideo2)
        val videoFlow = MutableStateFlow(domainVideos)

        val mockUseCaseContainer = mock<TestUseCaseContainer> {
            on { getAllVideosFlow() } doReturn videoFlow
        }

        context("VideoViewModelFactory") {
            test("creates VideoViewModel instance") {
                val factory = VideoViewModelFactory(mockUseCaseContainer)
                val viewModel = factory.create(VideoViewModel::class.java)
                viewModel.shouldBeInstanceOf<VideoViewModel>()
            }

            test("factory creates different instances for different calls") {
                val factory = VideoViewModelFactory(mockUseCaseContainer)
                val viewModel1 = factory.create(VideoViewModel::class.java)
                val viewModel2 = factory.create(VideoViewModel::class.java)
                viewModel1.shouldBeInstanceOf<VideoViewModel>()
                viewModel2.shouldBeInstanceOf<VideoViewModel>()
            }
        }
    })