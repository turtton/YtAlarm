package net.turtton.ytalarm.util.extensions

import android.annotation.SuppressLint
import androidx.work.WorkInfo
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.await

@SuppressLint("RestrictedApi")
suspend fun ListenableFuture<WorkInfo?>.takeStateIfNotFinished(): WorkInfo.State? =
    await()?.state?.takeUnless { it.isFinished }