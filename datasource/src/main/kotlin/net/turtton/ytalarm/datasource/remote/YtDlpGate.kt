package net.turtton.ytalarm.datasource.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

/** Process-wide: the merged app manifest must not introduce android:process components. */
internal class YtDlpGate {
    private val lock = ReentrantReadWriteLock(true)

    suspend fun <T> execute(shortWait: Boolean = false, block: () -> T): T =
        runInterruptible(Dispatchers.IO) {
            val read = lock.readLock()
            if (shortWait) {
                check(read.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) { "yt-dlp is busy updating" }
            } else {
                // Imports/downloads must not be permanently marked Failed just because of an update.
                read.lockInterruptibly()
            }
            try {
                block()
            } finally {
                read.unlock()
            }
        }

    // Never queue an updater behind a long download (or an alarm's stream request).
    fun <T> tryUpdate(block: () -> T): T? {
        val write = lock.writeLock()
        if (!write.tryLock()) return null
        return try {
            block()
        } finally {
            write.unlock()
        }
    }

    fun <T> initialize(block: () -> T): T {
        val write = lock.writeLock()
        write.lockInterruptibly()
        return try {
            block()
        } finally {
            write.unlock()
        }
    }

    private companion object {
        const val WAIT_SECONDS = 3L
    }
}