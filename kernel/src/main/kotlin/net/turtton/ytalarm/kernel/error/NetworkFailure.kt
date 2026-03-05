package net.turtton.ytalarm.kernel.error

/**
 * ネットワーク接続エラーを表す共通インターフェース。
 * [VideoInfoError.NetworkError] と [StreamError.NetworkError] が実装する。
 */
interface NetworkFailure {
    val cause: Throwable
}