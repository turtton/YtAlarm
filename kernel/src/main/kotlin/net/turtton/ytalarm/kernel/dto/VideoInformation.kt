package net.turtton.ytalarm.kernel.dto

/**
 * yt-dlpから取得した動画/プレイリスト情報を表すDTO。
 * 純粋Kotlinで定義し、Android依存を持たない。
 */
data class VideoInformation(
    val id: String,
    val title: String? = null,
    val url: String,
    val domain: String,
    val typeData: Type
) {
    sealed interface Type {
        data class Video(val fullTitle: String, val thumbnailUrl: String, val videoUrl: String) :
            Type

        data class Playlist(val entries: List<VideoInformation>) : Type
    }
}