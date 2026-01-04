package net.turtton.ytalarm.viewmodel

import net.turtton.ytalarm.database.structure.Video

sealed class ReimportResult {
    data class Success(val video: Video) : ReimportResult()
    sealed class Error : ReimportResult() {
        data object Parse : Error()
        data object Network : Error()
        data object IO : Error()
        data object Downloader : Error()
        data object NoUrl : Error()
    }
}