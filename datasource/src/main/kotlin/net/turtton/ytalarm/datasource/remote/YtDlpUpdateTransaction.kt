package net.turtton.ytalarm.datasource.remote

/** The durable marker is cleared only after a verified installation (including rollback). */
internal class YtDlpUpdateTransaction(
    private val markPending: () -> Unit,
    private val clearPending: () -> Unit,
    private val restoreBundled: () -> Unit,
    private val verify: () -> Unit
) {
    fun recover() {
        restoreBundled()
        verify()
        clearPending()
    }

    @Suppress("TooGenericExceptionCaught")
    fun <T> update(install: () -> T): T {
        markPending()
        return try {
            val result = install()
            verify()
            clearPending()
            result
        } catch (error: Exception) {
            try {
                recover()
            } catch (recoveryError: Exception) {
                error.addSuppressed(recoveryError)
            }
            throw error
        }
    }
}