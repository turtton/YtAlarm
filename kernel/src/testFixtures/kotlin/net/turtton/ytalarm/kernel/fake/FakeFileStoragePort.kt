package net.turtton.ytalarm.kernel.fake

import net.turtton.ytalarm.kernel.port.FileStoragePort
import java.io.File

class FakeFileStoragePort(
    private val downloadDir: File = File(System.getProperty("java.io.tmpdir"), "fake-downloads")
) : FileStoragePort {
    val existingFiles: MutableSet<String> = mutableSetOf()
    var totalSize: Long = 0L
    var deleteCount = 0

    override fun getDownloadDir(): File = downloadDir

    override fun deleteAllDownloads(): Long {
        deleteCount++
        val deleted = existingFiles.size.toLong()
        existingFiles.clear()
        totalSize = 0L
        return deleted
    }

    override fun getTotalDownloadSize(): Long = totalSize

    override fun fileExists(relativePath: String): Boolean = relativePath in existingFiles
}