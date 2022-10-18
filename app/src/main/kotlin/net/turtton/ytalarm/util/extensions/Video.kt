package net.turtton.ytalarm.util.extensions

import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import net.turtton.ytalarm.structure.Video

/**
 * Collects videos which finished downloading or import except Result is Failed.
 */
suspend fun List<Video>.collectGarbage(workManager: WorkManager): List<Video> = filter {
    it.stateData.workerId?.let { uuid ->
        workManager.getWorkInfoById(uuid)
            .await()
            .let { info ->
                info == null || (info.state.isFinished && info.state != WorkInfo.State.FAILED)
            }
    } ?: false
}