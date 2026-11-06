package net.turtton.ytalarm.util

/**
 * アラーム再生キューの進行状態を管理する純粋なステートマシン。
 *
 * 再生の副作用（ExoPlayerの操作やデフォルトアラーム音の再生）は持たず、
 * 「次に何をすべきか」を [Step] として返すだけにしている。
 *
 * 状態遷移:
 * - Idle --[start]--> Loading（先頭の動画を試行）
 * - Loading --[onPlaying]--> Playing（連続失敗回数をリセット）
 * - Loading / Playing --[onFailed]--> Loading（次の動画を試行。キュー末尾ならループ設定に関わらず先頭へ戻る）
 * - Playing --[onEnded]--> Loading（次の動画へ。末尾ではループ設定なら先頭へ、そうでなければ終了）
 * - Loading --[onEnded]--> [onFailed] と同じ（一度も再生されずに終了した場合は失敗扱い）
 * - 連続して全動画が失敗した場合 [Step.Exhausted]、ループ無しで末尾まで再生した場合 [Step.Finished] で停止
 *
 * 失敗時にループ設定を無視して先頭へ戻るのは、デフォルトアラーム音が鳴っている間は
 * 利用者を起こし続けるためにキュー全体を1周試す必要があるため。
 *
 * スレッドセーフではないため、メインスレッドからのみ使用すること。
 *
 * @param items 再生順に並んだ動画（空不可。シャッフルは呼び出し側で済ませておく）
 * @param loop キュー末尾まで正常再生した後に先頭から繰り返すか
 */
class AlarmPlaybackQueue<T>(private val items: List<T>, private val loop: Boolean) {
    /** 呼び出し側が次に行うべき操作 */
    sealed interface Step<out T> {
        /** [item] の再生を試行する */
        data class Play<T>(val item: T) : Step<T>

        /** ループ無しでキューを再生し終えた。アラームを終了してよい */
        data object Finished : Step<Nothing>

        /** キューの全動画を1周しても全て失敗した。これ以上試行しない */
        data object Exhausted : Step<Nothing>

        /** 現在の状態では無効なイベントだった（重複・停止後など）。何もしない */
        data object Ignored : Step<Nothing>
    }

    private enum class Phase { Idle, Loading, Playing, Stopped }

    private var phase = Phase.Idle
    private var index = -1
    private var consecutiveFailures = 0

    init {
        require(items.isNotEmpty()) { "items must not be empty" }
    }

    /** 現在の動画が実際に再生状態になっており、まだ失敗・終了していない場合 true */
    val isPlaying: Boolean get() = phase == Phase.Playing

    /** 先頭の動画の試行を開始する。開始済みの場合は [Step.Ignored] */
    fun start(): Step<T> = if (phase == Phase.Idle) playAt(0) else Step.Ignored

    /**
     * 現在試行中の動画が実際に再生状態になった。
     *
     * @return 試行中(Loading)から再生中へ遷移した場合 true。呼び出し側はデフォルトアラーム音を止めてよい
     */
    fun onPlaying(): Boolean {
        if (phase != Phase.Loading) return false
        phase = Phase.Playing
        consecutiveFailures = 0
        return true
    }

    /**
     * 現在の動画が最後まで再生された。
     *
     * 一度も再生状態（[onPlaying]）にならないまま終了した場合は再生できなかったものとみなし、
     * [onFailed] と同じ扱いにする（空のメディアなどで成功扱いにならないようにするため）。
     */
    fun onEnded(): Step<T> {
        if (phase == Phase.Idle || phase == Phase.Stopped) return Step.Ignored
        if (phase == Phase.Loading) return onFailed()
        consecutiveFailures = 0
        val next = index + 1
        return when {
            next < items.size -> playAt(next)
            loop -> playAt(0)
            else -> stop(Step.Finished)
        }
    }

    /** 現在の動画の取得・再生に失敗した（試行中・再生中のどちらでも可） */
    fun onFailed(): Step<T> {
        if (phase != Phase.Loading && phase != Phase.Playing) return Step.Ignored
        consecutiveFailures++
        return if (consecutiveFailures >= items.size) {
            stop(Step.Exhausted)
        } else {
            playAt((index + 1) % items.size)
        }
    }

    private fun playAt(position: Int): Step<T> {
        index = position
        phase = Phase.Loading
        return Step.Play(items[position])
    }

    private fun stop(step: Step<T>): Step<T> {
        phase = Phase.Stopped
        return step
    }
}