package net.turtton.ytalarm.util.extensions

import android.annotation.SuppressLint
import androidx.work.WorkManager
import androidx.work.await
import net.turtton.ytalarm.database.structure.Video

/**
 * Collects videos which finished downloading or importing except state is [Video.WorkerState.Failed].
 */
@SuppressLint("RestrictedApi")
suspend fun List<Video>.collectGarbage(workManager: WorkManager): List<Video> = filter {
    it.stateData.let { state ->
        when (state) {
            is Video.State.Importing ->
                state.state as? Video.WorkerState.Working
            is Video.State.Downloading ->
                state.state as? Video.WorkerState.Working
            else -> null
        }?.workerId
    }?.let { uuid ->
        workManager.getWorkInfoById(uuid)
            .await()
            .let { info ->
                info == null || (info.state.isFinished)
            }
    } ?: false
}