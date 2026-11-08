package net.turtton.ytalarm.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

class RetryableInitializationTest :
    FunSpec({
        test("failed startup is retried and both worker and playback see the latest success") {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                var attempts = 0
                var storageAvailable = false
                val initialization = RetryableInitialization(scope) {
                    attempts++
                    check(storageAvailable) { "disk full" }
                }
                val startup = initialization.get()
                startup.await().isFailure shouldBe true
                storageAvailable = true
                val worker = initialization.get()
                (worker === startup) shouldBe false
                worker.await().isSuccess shouldBe true
                val playback = initialization.get()
                (playback === worker) shouldBe true
                playback.await().isSuccess shouldBe true
                attempts shouldBe 2
            } finally {
                scope.cancel()
            }
        }
        test("concurrent callers share the same in-flight recovery") {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            try {
                var attempts = 0
                val initialization = RetryableInitialization(scope) {
                    attempts++
                    entered.complete(Unit)
                    release.await()
                }
                val worker = initialization.get()
                withTimeout(2000) { entered.await() }
                val playback = initialization.get()
                (worker === playback) shouldBe true
                release.complete(Unit)
                playback.await().isSuccess shouldBe true
                attempts shouldBe 1
            } finally {
                release.complete(Unit)
                scope.cancel()
            }
        }
        test("cancelling a caller does not cancel shared recovery") {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            try {
                val initialization = RetryableInitialization(scope) {
                    entered.complete(Unit)
                    release.await()
                }
                coroutineScope {
                    val waiter = async { initialization.get().await() }
                    withTimeout(2000) { entered.await() }
                    waiter.cancelAndJoin()
                }
                val next = initialization.get()
                next.isCancelled shouldBe false
                release.complete(Unit)
                next.await().isSuccess shouldBe true
            } finally {
                release.complete(Unit)
                scope.cancel()
            }
        }
    })