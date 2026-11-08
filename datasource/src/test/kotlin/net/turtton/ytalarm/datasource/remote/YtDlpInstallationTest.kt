package net.turtton.ytalarm.datasource.remote

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

private class InstallationFixture : AutoCloseable {
    val directory: File = Files.createTempDirectory("yt-dlp-installation-test").toFile()
    val binary = File(directory, "youtubedl-android/yt-dlp/yt-dlp")
    val pending = File(directory, "yt-dlp-update-pending")
    val bundled = "bundled-test-copy"
    val metadata = mutableMapOf(
        "dlpVersion" to "new-version",
        "dlpVersionName" to "new-name",
        "pythonLibVersion" to "python-version"
    )
    val events = mutableListOf<String>()
    var runtimeFails = false
    var probeFails = false
    var metadataCommitSucceeds = true
    private val context = mock<Context>()

    init {
        val resources = mock<Resources>()
        val preferences = mock<SharedPreferences>()
        val editor = mock<SharedPreferences.Editor>()
        val removed = mutableSetOf<String>()
        whenever(context.noBackupFilesDir).thenReturn(directory)
        whenever(context.resources).thenReturn(resources)
        whenever(resources.openRawResource(com.yausername.youtubedl_android.R.raw.ytdlp))
            .thenAnswer { ByteArrayInputStream(bundled.toByteArray()) }
        whenever(
            context.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
        ).thenReturn(preferences)
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.remove(any())).thenAnswer {
            removed += it.getArgument<String>(0)
            editor
        }
        whenever(editor.commit()).thenAnswer {
            if (metadataCommitSucceeds) {
                removed.forEach { metadata.remove(it) }
                removed.clear()
            }
            metadataCommitSucceeds
        }
    }

    fun installation(): YtDlpInstallation = YtDlpInstallation(
        context,
        initializeRuntime = {
            events += "init"
            check(!runtimeFails) { "runtime temporarily unavailable" }
        },
        verify = {
            events += "verify"
            check(!probeFails && binary.readText() == bundled) { "invalid binary" }
        }
    )

    fun interruptedInstall() {
        check(binary.parentFile!!.mkdirs())
        binary.writeText("partially copied")
        pending.writeText("pending")
    }

    override fun close() {
        directory.deleteRecursively()
    }
}

class YtDlpInstallationTest :
    FunSpec({
        test(
            "startup restores a partially installed file and clears exactly the two version keys"
        ) {
            InstallationFixture().use { fixture ->
                fixture.interruptedInstall()
                fixture.installation().initialize()
                fixture.binary.readText() shouldBe fixture.bundled
                fixture.metadata shouldBe mapOf("pythonLibVersion" to "python-version")
                fixture.pending.exists() shouldBe false
                fixture.events shouldBe listOf("init", "verify")
            }
        }
        test(
            "failed startup runtime initialization can be retried in the same process under the gate"
        ) {
            InstallationFixture().use { fixture ->
                val gate = YtDlpGate()
                fixture.interruptedInstall()
                fixture.runtimeFails = true
                shouldThrow<IllegalStateException> {
                    gate.initialize { fixture.installation().initialize() }
                }
                fixture.pending.exists() shouldBe true
                fixture.runtimeFails = false
                gate.initialize { fixture.installation().initialize() }
                fixture.pending.exists() shouldBe false
                fixture.events shouldBe listOf("init", "init", "verify")
                gate.execute { fixture.binary.readText() } shouldBe fixture.bundled
            }
        }
        test(
            "failed startup probe leaves a marker so a nonempty binary is verified again on retry"
        ) {
            InstallationFixture().use { fixture ->
                fixture.probeFails = true
                shouldThrow<IllegalStateException> { fixture.installation().initialize() }
                fixture.binary.isFile shouldBe true
                fixture.pending.exists() shouldBe true
                fixture.probeFails = false
                fixture.installation().initialize()
                fixture.pending.exists() shouldBe false
                fixture.events shouldBe listOf("init", "verify", "init", "verify")
            }
        }
        test("bad updated binary is rolled back using the real file and metadata adapter") {
            InstallationFixture().use { fixture ->
                val installation = fixture.installation()
                installation.initialize()
                fixture.metadata["dlpVersion"] = "updated-version"
                fixture.metadata["dlpVersionName"] = "updated-name"
                shouldThrow<IllegalStateException> {
                    installation.transaction.update { fixture.binary.writeText("broken update") }
                }
                fixture.binary.readText() shouldBe fixture.bundled
                fixture.metadata shouldBe mapOf("pythonLibVersion" to "python-version")
                fixture.pending.exists() shouldBe false
            }
        }
        test("failed metadata commit keeps the marker and can recover after storage is available") {
            InstallationFixture().use { fixture ->
                fixture.metadataCommitSucceeds = false
                shouldThrow<IllegalStateException> { fixture.installation().initialize() }
                fixture.pending.exists() shouldBe true
                fixture.metadataCommitSucceeds = true
                fixture.installation().initialize()
                fixture.pending.exists() shouldBe false
                fixture.metadata shouldBe mapOf("pythonLibVersion" to "python-version")
            }
        }
    })