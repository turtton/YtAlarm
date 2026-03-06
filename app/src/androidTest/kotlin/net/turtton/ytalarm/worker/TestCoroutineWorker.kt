package net.turtton.ytalarm.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import net.turtton.ytalarm.YtApplication
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestCoroutineWorker {

    class Impl(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val useCaseContainer = (applicationContext as YtApplication).dataContainerProvider
                .getUseCaseContainer()
            val playlists = useCaseContainer.getAllPlaylistsSync().map { it.id }.toTypedArray()
            return Result.success(workDataOf("playlist" to playlists))
        }
    }

    @Test
    fun testDatabaseAccess() {
        val activity = ApplicationProvider.getApplicationContext<Context>()

        val worker = TestListenableWorkerBuilder<Impl>(activity).build()
        runBlocking {
            val result = worker.doWork()
            (result is Success) shouldBe true
        }
    }
}