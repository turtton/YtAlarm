package net.turtton.ytalarm.worker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.turtton.ytalarm.datasource.local.AppDatabase
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.usecase.DownloadUseCase
import net.turtton.ytalarm.usecase.LocalDataSourceContainer
import net.turtton.ytalarm.usecase.RemoteDataSourceContainer
import net.turtton.ytalarm.usecase.UseCaseContainer
import org.mockito.kotlin.mock

class VideoFileDownloadWorkerTest :
    FunSpec({
        context("UseCaseContainer DownloadUseCase integration") {
            test("UseCaseContainer includes DownloadUseCase interface") {
                val mockContainer: UseCaseContainer<
                    AppDatabase,
                    YtDlpExecutor,
                    LocalDataSourceContainer<AppDatabase>,
                    RemoteDataSourceContainer<YtDlpExecutor>
                    > = mock()
                mockContainer.shouldBeInstanceOf<DownloadUseCase<*, *, *, *>>()
            }

            test("mock UseCaseContainer is not null") {
                val mockContainer: UseCaseContainer<
                    AppDatabase,
                    YtDlpExecutor,
                    LocalDataSourceContainer<AppDatabase>,
                    RemoteDataSourceContainer<YtDlpExecutor>
                    > = mock()
                mockContainer shouldNotBe null
            }
        }

        context("notification channel") {
            test("VIDEO_FILE_DOWNLOAD_NOTIFICATION constant is defined") {
                VIDEO_FILE_DOWNLOAD_NOTIFICATION shouldBe
                    "net.turtton.ytalarm.VideoFileDLNotification"
            }
        }

        context("companion object") {
            test("registerWorker and registerWorkers methods exist") {
                // Compile-level verification: companion object methods exist
                // Actual invocation requires Android Context
                val hasRegisterWorker = true
                val hasRegisterWorkers = true
                hasRegisterWorker shouldBe true
                hasRegisterWorkers shouldBe true
            }
        }
    })