package net.turtton.ytalarm.util.extensions

import android.annotation.SuppressLint
import androidx.work.WorkInfo
import kotlinx.coroutines.guava.await

@SuppressLint("RestrictedApi")
suspend fun com.google.common.util.concurrent.ListenableFuture<WorkInfo?>
    .takeStateIfNotFinished(): WorkInfo.State? =
    await()?.state?.takeUnless { it.isFinished }