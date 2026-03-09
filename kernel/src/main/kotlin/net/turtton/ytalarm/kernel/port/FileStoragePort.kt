package net.turtton.ytalarm.kernel.port

import java.io.File

/**
 * ダウンロードファイルのストレージ操作を抽象化するPortインターフェース。
 * Kernel/UseCase層はこのインターフェースに依存し、実装はApp層（AndroidFileStorageAdapter）で提供する。
 */
interface FileStoragePort {
    fun getDownloadDir(): File

    fun deleteAllDownloads(): Long

    fun getTotalDownloadSize(): Long

    fun fileExists(relativePath: String): Boolean
}