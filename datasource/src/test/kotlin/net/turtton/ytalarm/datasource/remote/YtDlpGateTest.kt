package net.turtton.ytalarm.datasource.remote

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
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
            val worker = thread {
                gate.execute {
                    entered.countDown()
                    release.await()
                }
            }
            try {
                entered.await(5, TimeUnit.SECONDS) shouldBe true
                gate.tryUpdate { error("must not update") } shouldBe null
                gate.execute { "parallel stream" } shouldBe "parallel stream"
            } finally {
                release.countDown()
                worker.join()
            }
            gate.tryUpdate { "updated" } shouldBe "updated"
        }
        test("stream fails after three seconds instead of waiting for update completion") {
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
                shouldThrow<IllegalStateException> { gate.execute { error("must not execute") } }
                (start.elapsedNow() >= 3.seconds) shouldBe true
                (start.elapsedNow() < 5.seconds) shouldBe true
            } finally {
                release.countDown()
                worker.join()
            }
            gate.execute { "ok" } shouldBe "ok"
        }
        test("exceptions release both gates") {
            val gate = YtDlpGate()
            shouldThrow<IllegalStateException> { gate.tryUpdate { error("bad update") } }
            shouldThrow<IllegalStateException> { gate.execute { error("bad import") } }
            gate.tryUpdate { true } shouldBe true
        }
    })