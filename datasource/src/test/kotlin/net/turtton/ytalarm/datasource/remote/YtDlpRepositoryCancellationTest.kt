package net.turtton.ytalarm.datasource.remote

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CancellationException
import net.turtton.ytalarm.datasource.repository.YtDlpVideoDownloadRepository
import net.turtton.ytalarm.datasource.repository.YtDlpVideoInfoRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class YtDlpRepositoryCancellationTest :
    FunSpec({
        test("cancelled import is propagated instead of converted into a permanent NetworkError") {
            val executor = mock<YtDlpExecutor>()
            whenever(
                executor.execute(any(), anyOrNull())
            ).thenThrow(CancellationException("cancelled wait"))
            val repository = YtDlpVideoInfoRepository()
            shouldThrow<CancellationException> {
                repository.fetchVideoInfo(executor, "https://example.com/video")
            }
            shouldThrow<CancellationException> {
                repository.fetchPlaylistInfo(executor, "https://example.com/playlist")
            }
        }
        test(
            "cancelled download is propagated instead of converted into a permanent NetworkError"
        ) {
            val executor = mock<YtDlpExecutor>()
            whenever(
                executor.execute(any(), anyOrNull())
            ).thenThrow(CancellationException("cancelled wait"))
            shouldThrow<CancellationException> {
                YtDlpVideoDownloadRepository().downloadVideo(
                    executor,
                    "https://example.com/video",
                    "unused",
                    "b"
                ) {
                }
            }
        }
        test("cancelled stream request is propagated") {
            val executor = mock<YtDlpExecutor>()
            whenever(executor.getInfo(any())).thenThrow(CancellationException("cancelled wait"))
            shouldThrow<CancellationException> {
                YtDlpVideoInfoRepository().getStreamUrl(executor, "https://example.com/video", "b")
            }
        }
    })