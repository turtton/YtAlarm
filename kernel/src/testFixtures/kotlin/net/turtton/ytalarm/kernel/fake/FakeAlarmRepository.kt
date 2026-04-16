package net.turtton.ytalarm.kernel.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.repository.AlarmRepository
import java.util.concurrent.atomic.AtomicLong

class FakeAlarmRepository : AlarmRepository<Unit> {
    private val store = MutableStateFlow<List<Alarm>>(emptyList())
    private val nextId = AtomicLong(1L)

    val currentData: List<Alarm> get() = store.value
    val updateHistory: MutableList<Alarm> = mutableListOf()

    fun resetWith(vararg alarms: Alarm) {
        store.value = alarms.toList()
        nextId.set((alarms.maxOfOrNull { it.id } ?: 0L) + 1)
        updateHistory.clear()
    }

    override fun getAll(executor: Unit): Flow<List<Alarm>> = store

    override suspend fun getAllSync(executor: Unit): List<Alarm> = store.value

    override suspend fun getFromId(executor: Unit, id: Long): Alarm? =
        store.value.find { it.id == id }

    override suspend fun getMatched(executor: Unit, repeatType: Alarm.RepeatType): List<Alarm> =
        store.value.filter { it.repeatType == repeatType }

    override suspend fun insert(executor: Unit, alarm: Alarm): Long {
        val id = nextId.getAndIncrement()
        val stored = alarm.copy(id = id)
        store.update { it + stored }
        return id
    }

    override suspend fun update(executor: Unit, alarm: Alarm) {
        updateHistory.add(alarm)
        store.update { list -> list.map { if (it.id == alarm.id) alarm else it } }
    }

    override suspend fun delete(executor: Unit, alarm: Alarm) {
        store.update { list -> list.filter { it.id != alarm.id } }
    }
}