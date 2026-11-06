package net.turtton.ytalarm.datasource.remote

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

/** Process-wide: the merged app manifest must not introduce android:process components. */
internal class YtDlpGate {
    private val lock = ReentrantReadWriteLock(true)

    fun <T> execute(block: () -> T): T {
        val read = lock.readLock()
        check(read.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) { "yt-dlp is busy updating" }
        return try {
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