package net.turtton.ytalarm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("UNUSED")
class ExampleUnitTest : FunSpec({
    context("the base test code") {
        test("addition is correct") {
            2 + 2 shouldBeExactly 4
        }
    }
})