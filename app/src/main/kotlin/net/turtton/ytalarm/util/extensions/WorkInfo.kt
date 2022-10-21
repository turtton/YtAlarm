package net.turtton.ytalarm.util.extensions

import android.annotation.SuppressLint
import androidx.work.WorkInfo
import androidx.work.await
import com.google.common.util.concurrent.ListenableFuture

@SuppressLint("RestrictedApi")
suspend fun ListenableFuture<WorkInfo?>.takeStateIfNotFinished(): WorkInfo.State? = await()
    ?.state
    ?.takeUnless { it.isFinished }