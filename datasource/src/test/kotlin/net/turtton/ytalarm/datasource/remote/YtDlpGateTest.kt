package net.turtton.ytalarm.datasource.remote

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class YtDlpGateTest :
    FunSpec({
        test("update never queues behind imports or downloads; streams can run concurrently") {
            val gate = YtDlpGate()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val worker =
                thread {
                    runBlocking {
                        gate.execute {
                            entered.countDown()
                            release.await()
                        }
                    }
                }
            try {
                entered.await(5, TimeUnit.SECONDS) shouldBe true
                gate.tryUpdate { error("must not update") } shouldBe null
                gate.execute(shortWait = true) { "parallel stream" } shouldBe "parallel stream"
            } finally {
                release.countDown()
                worker.join()
            }
            gate.tryUpdate { "updated" } shouldBe "updated"
        }
        test("alarm stream fails after three seconds instead of waiting for update completion") {
            val gate = YtDlpGate()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val worker = thread {
                gate.tryUpdate {
                    entered.countDown()
                    release.await()
                }
            }
            try {
                entered.await(5, TimeUnit.SECONDS) shouldBe true
                val start = TimeSource.Monotonic.markNow()
                shouldThrow<IllegalStateException> {
                    gate.execute(shortWait = true) { error("must not execute") }
                }
                (start.elapsedNow() >= 3.seconds) shouldBe true
                (start.elapsedNow() < 5.seconds) shouldBe true
            } finally {
                release.countDown()
                worker.join()
            }
            gate.execute { "ok" } shouldBe "ok"
        }
        test("normal import or download waits beyond the alarm timeout and then succeeds") {
            coroutineScope {
                val gate = YtDlpGate()
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val worker = thread {
                    gate.tryUpdate {
                        entered.countDown()
                        release.await()
                    }
                }
                try {
                    entered.await(5, TimeUnit.SECONDS) shouldBe true
                    val normal = async(Dispatchers.IO) { gate.execute { "imported" } }
                    delay(3250)
                    normal.isCompleted shouldBe false
                    release.countDown()
                    withTimeout(2000) { normal.await() } shouldBe "imported"
                } finally {
                    release.countDown()
                    worker.join()
                }
            }
        }
        test("normal wait can be cancelled without waiting for the updater to finish") {
            coroutineScope {
                val gate = YtDlpGate()
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val worker = thread {
                    gate.tryUpdate {
                        entered.countDown()
                        release.await()
                    }
                }
                try {
                    entered.await(5, TimeUnit.SECONDS) shouldBe true
                    val waiting =
                        async(Dispatchers.IO) {
                            gate.execute { error("cancelled import must not execute") }
                        }
                    delay(50)
                    withTimeout(2000) { waiting.cancelAndJoin() }
                    waiting.isCancelled shouldBe true
                } finally {
                    release.countDown()
                    worker.join()
                }
                gate.tryUpdate { true } shouldBe true
            }
        }
        test("exceptions release both gates") {
            val gate = YtDlpGate()
            shouldThrow<IllegalStateException> { gate.tryUpdate { error("bad update") } }
            shouldThrow<IllegalStateException> { gate.execute { error("bad import") } }
            gate.tryUpdate { true } shouldBe true
        }
    })