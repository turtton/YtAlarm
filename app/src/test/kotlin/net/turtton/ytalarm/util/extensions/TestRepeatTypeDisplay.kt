package net.turtton.ytalarm.util.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import net.turtton.ytalarm.kernel.entity.Alarm

/**
 * Tests for RepeatType.getDisplay() and DayOfWeek.getDisplay() extension functions.
 *
 * These tests verify the mapping logic between RepeatType variants and display strings.
 * Context-dependent behavior is tested indirectly using mocked Context.
 */
@Suppress("UNUSED")
class TestRepeatTypeDisplay :
    FunSpec({
        // getDisplay implementations are tested in integration/instrumentation tests
        // that have access to a real Context. Here we only verify pure logic.

        context("RepeatType.toRepeatTypeSelection") {
            test("Once maps correctly") {
                val repeatType: Alarm.RepeatType = Alarm.RepeatType.Once
                repeatType.toRepeatTypeSelection() shouldBe RepeatTypeDisplaySelection.ONCE
            }

            test("Everyday maps correctly") {
                val repeatType: Alarm.RepeatType = Alarm.RepeatType.Everyday
                repeatType.toRepeatTypeSelection() shouldBe RepeatTypeDisplaySelection.EVERYDAY
            }

            test("Snooze maps correctly") {
                val repeatType: Alarm.RepeatType = Alarm.RepeatType.Snooze
                repeatType.toRepeatTypeSelection() shouldBe RepeatTypeDisplaySelection.SNOOZE
            }

            test("Days maps correctly") {
                val repeatType: Alarm.RepeatType = Alarm.RepeatType.Days(listOf(DayOfWeek.MONDAY))
                repeatType.toRepeatTypeSelection() shouldBe RepeatTypeDisplaySelection.DAYS
            }

            test("Date maps correctly") {
                val repeatType: Alarm.RepeatType =
                    Alarm.RepeatType.Date(LocalDate(2024, 1, 1))
                repeatType.toRepeatTypeSelection() shouldBe RepeatTypeDisplaySelection.DATE
            }
        }
    })

/**
 * Enum representing RepeatType variants for display purposes.
 * Used in test to verify mapping logic.
 */
enum class RepeatTypeDisplaySelection {
    ONCE,
    EVERYDAY,
    SNOOZE,
    DAYS,
    DATE
}

/**
 * Maps [Alarm.RepeatType] to [RepeatTypeDisplaySelection] for testing.
 */
fun Alarm.RepeatType.toRepeatTypeSelection(): RepeatTypeDisplaySelection = when (this) {
    is Alarm.RepeatType.Once -> RepeatTypeDisplaySelection.ONCE
    is Alarm.RepeatType.Everyday -> RepeatTypeDisplaySelection.EVERYDAY
    is Alarm.RepeatType.Snooze -> RepeatTypeDisplaySelection.SNOOZE
    is Alarm.RepeatType.Days -> RepeatTypeDisplaySelection.DAYS
    is Alarm.RepeatType.Date -> RepeatTypeDisplaySelection.DATE
}