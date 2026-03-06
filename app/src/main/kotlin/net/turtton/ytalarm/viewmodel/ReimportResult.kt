package net.turtton.ytalarm.viewmodel

import net.turtton.ytalarm.kernel.entity.Video

sealed class ReimportResult {
    data class Success(val video: Video) : ReimportResult()
    sealed class Error : ReimportResult() {
        data object Parse : Error()
        data object Network : Error()
        data object NoUrl : Error()
    }
}