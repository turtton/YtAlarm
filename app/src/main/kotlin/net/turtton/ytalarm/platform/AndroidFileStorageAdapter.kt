package net.turtton.ytalarm.platform

import android.content.Context
import android.os.Environment
import net.turtton.ytalarm.kernel.port.FileStoragePort
import java.io.File

class AndroidFileStorageAdapter(private val context: Context) : FileStoragePort {
    override fun getDownloadDir(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(moviesDir, "downloads")
    }

    override fun deleteAllDownloads(): Long {
        val dir = getDownloadDir()
        if (!dir.exists()) return 0
        var count = 0L
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                count++
            }
        }
        return count
    }

    override fun getTotalDownloadSize(): Long {
        val dir = getDownloadDir()
        if (!dir.exists()) return 0
        return dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0
    }

    override fun fileExists(relativePath: String): Boolean {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(moviesDir, relativePath).exists()
    }
}