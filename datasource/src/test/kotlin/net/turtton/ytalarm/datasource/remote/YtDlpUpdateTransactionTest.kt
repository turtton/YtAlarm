package net.turtton.ytalarm.datasource.remote

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YtDlpUpdateTransactionTest :
    FunSpec({
        test("success and already-current status are verified before clearing marker") {
            val events = mutableListOf<String>()
            val transaction = YtDlpUpdateTransaction(
                { events += "pending" },
                { events += "clear" },
                { events += "restore" },
                { events += "verify" }
            )
            transaction.update {
                events += "install"
                "current"
            } shouldBe "current"
            events shouldBe listOf("pending", "install", "verify", "clear")
        }
        test("failed validation restores bundled copy and verifies it") {
            val events = mutableListOf<String>()
            var bundled = false
            val transaction = YtDlpUpdateTransaction(
                { events += "pending" },
                { events += "clear" },
                {
                    events += "restore"
                    bundled = true
                },
                {
                    events += "verify"
                    check(bundled)
                }
            )
            shouldThrow<IllegalStateException> { transaction.update { events += "install" } }
            events shouldBe listOf("pending", "install", "verify", "restore", "verify", "clear")
        }
        test("download failure restores and still reports failure") {
            val events = mutableListOf<String>()
            val transaction = YtDlpUpdateTransaction(
                { events += "pending" },
                { events += "clear" },
                { events += "restore" },
                { events += "verify" }
            )
            shouldThrow<IllegalStateException> { transaction.update { error("offline") } }
            events shouldBe listOf("pending", "restore", "verify", "clear")
        }
        test("failed rollback retains marker and original error") {
            var pending = false
            val transaction = YtDlpUpdateTransaction(
                { pending = true },
                { pending = false },
                { error("disk full") },
                {}
            )
            val error =
                shouldThrow<IllegalStateException> { transaction.update { error("bad update") } }
            pending shouldBe true
            error.message shouldBe "bad update"
            error.suppressed.single().message shouldBe "disk full"
        }
        test("marker persistence failure prevents installation") {
            var installed = false
            val transaction = YtDlpUpdateTransaction({ error("disk full") }, {}, {}, {})
            shouldThrow<IllegalStateException> { transaction.update { installed = true } }
            installed shouldBe false
        }
        test("failed recovery verification does not clear the marker") {
            var cleared = false
            val transaction =
                YtDlpUpdateTransaction({}, { cleared = true }, {}, { error("bad bundled copy") })
            shouldThrow<IllegalStateException> { transaction.recover() }
            cleared shouldBe false
        }
        test("interrupted update recovery verifies before clearing marker") {
            val events = mutableListOf<String>()
            YtDlpUpdateTransaction(
                {},
                { events += "clear" },
                { events += "restore" },
                { events += "verify" }
            ).recover()
            events shouldBe listOf("restore", "verify", "clear")
        }
    })