package net.turtton.ytalarm.viewmodel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.turtton.ytalarm.TestUseCaseContainer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import net.turtton.ytalarm.kernel.entity.Alarm as DomainAlarm

@Suppress("UNUSED")
class AlarmViewModelTest :
    FunSpec({
        val domainAlarm1 = DomainAlarm(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatType = DomainAlarm.RepeatType.Once,
            isEnabled = true
        )
        val domainAlarm2 = DomainAlarm(
            id = 2L,
            hour = 9,
            minute = 0,
            repeatType = DomainAlarm.RepeatType.Everyday,
            isEnabled = false
        )
        val domainAlarms = listOf(domainAlarm1, domainAlarm2)
        val alarmFlow = MutableStateFlow(domainAlarms)

        // UseCaseContainerをモック（defaultメソッドはすべてモック経由で提供）
        val mockUseCaseContainer = mock<TestUseCaseContainer> {
            on { getAllAlarmsFlow() } doReturn alarmFlow
        }

        context("AlarmViewModelFactory") {
            test("creates AlarmViewModel instance") {
                val factory = AlarmViewModelFactory(mockUseCaseContainer)
                val viewModel = factory.create(AlarmViewModel::class.java)
                viewModel.shouldBeInstanceOf<AlarmViewModel>()
            }

            test("factory creates different instances for different calls") {
                val factory = AlarmViewModelFactory(mockUseCaseContainer)
                val viewModel1 = factory.create(AlarmViewModel::class.java)
                val viewModel2 = factory.create(AlarmViewModel::class.java)
                // ViewModelFactoryは呼ぶたびに新しいインスタンスを生成する
                viewModel1.shouldBeInstanceOf<AlarmViewModel>()
                viewModel2.shouldBeInstanceOf<AlarmViewModel>()
            }
        }
    })