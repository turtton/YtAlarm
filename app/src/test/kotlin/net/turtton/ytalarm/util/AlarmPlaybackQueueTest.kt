package net.turtton.ytalarm.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.util.AlarmPlaybackQueue.Step

/**
 * Tests for [AlarmPlaybackQueue].
 *
 * 再生失敗時にアラーム音を鳴らしたまま次の動画へ進む挙動の根幹となる、キュー進行ロジックを検証する。
 */
@Suppress("UNUSED")
class AlarmPlaybackQueueTest :
    FunSpec({
        fun queueOf(vararg items: String, loop: Boolean = false) =
            AlarmPlaybackQueue(items.toList(), loop)

        // 再生状態になった後に最後まで再生された、という正常系のイベント列
        fun AlarmPlaybackQueue<String>.playAndEnd(): Step<String> {
            onPlaying()
            return onEnded()
        }

        context("initialization") {
            test("empty items are rejected") {
                shouldThrow<IllegalArgumentException> {
                    AlarmPlaybackQueue(emptyList<String>(), loop = false)
                }
            }

            test("start returns the first item once") {
                val queue = queueOf("a", "b")
                queue.start() shouldBe Step.Play("a")
                queue.start() shouldBe Step.Ignored
            }

            test("events before start are ignored") {
                val queue = queueOf("a", "b")
                queue.onFailed() shouldBe Step.Ignored
                queue.onEnded() shouldBe Step.Ignored
                queue.onPlaying() shouldBe false
            }
        }

        context("normal playback") {
            test("ended advances in order and finishes without loop") {
                val queue = queueOf("a", "b")
                queue.start() shouldBe Step.Play("a")
                queue.onPlaying() shouldBe true
                queue.playAndEnd() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
                queue.playAndEnd() shouldBe Step.Finished
            }

            test("ended wraps to the first item with loop") {
                val queue = queueOf("a", "b", loop = true)
                queue.start()
                queue.playAndEnd() shouldBe Step.Play("b")
                queue.playAndEnd() shouldBe Step.Play("a")
                queue.playAndEnd() shouldBe Step.Play("b")
            }

            test("single item with loop replays the same item") {
                val queue = queueOf("a", loop = true)
                queue.start() shouldBe Step.Play("a")
                queue.onPlaying() shouldBe true
                queue.playAndEnd() shouldBe Step.Play("a")
            }

            test("onPlaying reports transition only once per attempt") {
                val queue = queueOf("a")
                queue.start()
                queue.onPlaying() shouldBe true
                queue.onPlaying() shouldBe false
            }

            test("events after finishing are ignored") {
                val queue = queueOf("a")
                queue.start()
                queue.playAndEnd() shouldBe Step.Finished
                queue.onEnded() shouldBe Step.Ignored
                queue.onFailed() shouldBe Step.Ignored
                queue.onPlaying() shouldBe false
            }
        }

        context("ended before playback started") {
            test("is treated as a failure and advances to the next item") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.onEnded() shouldBe Step.Play("b")
                queue.isPlaying shouldBe false
                queue.onPlaying() shouldBe true
            }

            test("does not finish the queue when the last item ends before playing") {
                val queue = queueOf("a", "b")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                // b が再生状態にならないまま終了した場合、Finished ではなく全滅(Exhausted)になる
                queue.onEnded() shouldBe Step.Exhausted
            }

            test("with loop, repeated unplayable endings exhaust instead of looping forever") {
                val queue = queueOf("a", "b", loop = true)
                queue.start()
                queue.onEnded() shouldBe Step.Play("b")
                queue.onEnded() shouldBe Step.Exhausted
                queue.onEnded() shouldBe Step.Ignored
            }

            test("with loop, a failure followed by unplayable ending still exhausts") {
                val queue = queueOf("a", "b", loop = true)
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onEnded() shouldBe Step.Exhausted
            }

            test("single item ending before playing exhausts even with loop") {
                val queue = queueOf("a", loop = true)
                queue.start()
                queue.onEnded() shouldBe Step.Exhausted
            }

            test("a played ending after an unplayable one is a normal ending") {
                val queue = queueOf("a", "b")
                queue.start()
                queue.onEnded() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
                queue.onEnded() shouldBe Step.Finished
            }

            test("isPlaying is true only between onPlaying and the next event") {
                val queue = queueOf("a", "b")
                queue.isPlaying shouldBe false
                queue.start()
                queue.isPlaying shouldBe false
                queue.onPlaying()
                queue.isPlaying shouldBe true
                queue.onEnded()
                queue.isPlaying shouldBe false
            }
        }

        context("failure handling") {
            test("failure advances to the next item") {
                val queue = queueOf("a", "b", "c")
                queue.start() shouldBe Step.Play("a")
                queue.onFailed() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
            }

            test("failure of the next item advances again") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onFailed() shouldBe Step.Play("c")
                queue.onPlaying() shouldBe true
            }

            test("failure after switching to a recovered item advances again") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
                queue.onFailed() shouldBe Step.Play("c")
            }

            test("all items failing exhausts the queue") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onFailed() shouldBe Step.Play("c")
                queue.onFailed() shouldBe Step.Exhausted
            }

            test("exhausted queue ignores further events") {
                val queue = queueOf("a", "b")
                queue.start()
                queue.onFailed()
                queue.onFailed() shouldBe Step.Exhausted
                queue.onFailed() shouldBe Step.Ignored
                queue.onEnded() shouldBe Step.Ignored
                queue.onPlaying() shouldBe false
            }

            test("single item failing exhausts immediately even with loop") {
                val queue = queueOf("a", loop = true)
                queue.start()
                queue.onFailed() shouldBe Step.Exhausted
            }

            test("failure at the end of the queue wraps around even without loop") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.playAndEnd() shouldBe Step.Play("b")
                queue.playAndEnd() shouldBe Step.Play("c")
                queue.onFailed() shouldBe Step.Play("a")
            }

            test("wrapping failures still stop after one full cycle of consecutive failures") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.playAndEnd() shouldBe Step.Play("b")
                queue.playAndEnd() shouldBe Step.Play("c")
                queue.onFailed() shouldBe Step.Play("a")
                queue.onFailed() shouldBe Step.Play("b")
                queue.onFailed() shouldBe Step.Exhausted
            }

            test("success resets the consecutive failure count") {
                val queue = queueOf("a", "b", "c")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onFailed() shouldBe Step.Play("c")
                queue.onPlaying() shouldBe true
                // c が再生できた後の失敗は新たな1周のカウントになる
                queue.onFailed() shouldBe Step.Play("a")
                queue.onFailed() shouldBe Step.Play("b")
                queue.onFailed() shouldBe Step.Exhausted
            }

            test("failure while playing in a looping queue keeps trying other items") {
                val queue = queueOf("a", "b", loop = true)
                queue.start()
                queue.onPlaying() shouldBe true
                queue.onFailed() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
                queue.playAndEnd() shouldBe Step.Play("a")
            }

            test("ended after recovering from failure finishes without loop") {
                val queue = queueOf("a", "b")
                queue.start()
                queue.onFailed() shouldBe Step.Play("b")
                queue.onPlaying() shouldBe true
                queue.playAndEnd() shouldBe Step.Finished
            }
        }
    })