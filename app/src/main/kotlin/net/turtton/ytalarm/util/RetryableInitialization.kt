package net.turtton.ytalarm.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/** Shares only an in-flight or successful attempt; a failed attempt is never permanently cached. */
internal class RetryableInitialization(
    private val scope: CoroutineScope,
    private val onFailure: (Throwable) -> Unit = {},
    private val initialize: suspend () -> Unit
) {
    private val lock = Any()
    private var successful = false
    private var current: Deferred<Result<Unit>>? = null

    fun get(): Deferred<Result<Unit>> = synchronized(lock) {
        current?.takeIf { successful || !it.isCompleted } ?: scope.async(
            Dispatchers.IO,
            start = CoroutineStart.LAZY
        ) {
            attempt().also { result ->
                synchronized(lock) { successful = result.isSuccess }
            }
        }.also {
            current = it
            it.start()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun attempt(): Result<Unit> = try {
        initialize()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        onFailure(error)
        Result.failure(error)
    }
}