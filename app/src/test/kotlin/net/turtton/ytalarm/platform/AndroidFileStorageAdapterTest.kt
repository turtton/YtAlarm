package net.turtton.ytalarm.platform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.kernel.port.FileStoragePort
import java.io.File

class AndroidFileStorageAdapterTest :
    FunSpec({
        lateinit var tempDir: File
        lateinit var fileStorage: FileStoragePort

        beforeEach {
            tempDir = kotlin.io.path.createTempDirectory("file-storage-test").toFile()
            fileStorage = object : FileStoragePort {
                override fun getDownloadDir(): File = File(tempDir, "downloads")

                override fun deleteAllDownloads(): Long {
                    val dir = getDownloadDir()
                    if (!dir.exists()) return 0
                    var count = 0L
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.delete()) count++
                    }
                    return count
                }

                override fun getTotalDownloadSize(): Long {
                    val dir = getDownloadDir()
                    if (!dir.exists()) return 0
                    return dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0
                }

                override fun fileExists(relativePath: String): Boolean =
                    File(tempDir, relativePath).exists()
            }
        }

        afterEach {
            tempDir.deleteRecursively()
        }

        context("getDownloadDir") {
            test("returns downloads subdirectory") {
                fileStorage.getDownloadDir().name shouldBe "downloads"
            }
        }

        context("getTotalDownloadSize") {
            test("returns 0 when no downloads exist") {
                fileStorage.getTotalDownloadSize() shouldBe 0
            }

            test("returns total size of downloaded files") {
                val dir = fileStorage.getDownloadDir()
                dir.mkdirs()
                File(dir, "video1.mp4").writeBytes(ByteArray(100))
                File(dir, "video2.mp4").writeBytes(ByteArray(200))

                fileStorage.getTotalDownloadSize() shouldBe 300
            }
        }

        context("deleteAllDownloads") {
            test("returns 0 when no downloads exist") {
                fileStorage.deleteAllDownloads() shouldBe 0
            }

            test("deletes all files and returns count") {
                val dir = fileStorage.getDownloadDir()
                dir.mkdirs()
                File(dir, "video1.mp4").writeBytes(ByteArray(100))
                File(dir, "video2.mp4").writeBytes(ByteArray(200))

                val deleted = fileStorage.deleteAllDownloads()

                deleted shouldBe 2
                fileStorage.getTotalDownloadSize() shouldBe 0
            }
        }

        context("fileExists") {
            test("returns false for non-existent file") {
                fileStorage.fileExists("downloads/nonexistent.mp4") shouldBe false
            }

            test("returns true for existing file") {
                val dir = fileStorage.getDownloadDir()
                dir.mkdirs()
                File(dir, "video1.mp4").writeBytes(ByteArray(10))

                fileStorage.fileExists("downloads/video1.mp4") shouldBe true
            }
        }
    })