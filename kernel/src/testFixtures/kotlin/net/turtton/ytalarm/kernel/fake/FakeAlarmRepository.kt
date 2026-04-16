package net.turtton.ytalarm.kernel.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.repository.AlarmRepository

class FakeAlarmRepository : AlarmRepository<Unit> {
    private val store = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    val currentData: List<Alarm> get() = store.value

    fun seed(vararg alarms: Alarm) {
        store.value = alarms.toList()
        nextId = (alarms.maxOfOrNull { it.id } ?: 0L) + 1
    }

    override fun getAll(executor: Unit): Flow<List<Alarm>> = store

    override suspend fun getAllSync(executor: Unit): List<Alarm> = store.value

    override suspend fun getFromId(executor: Unit, id: Long): Alarm? =
        store.value.find { it.id == id }

    override suspend fun getMatched(executor: Unit, repeatType: Alarm.RepeatType): List<Alarm> =
        store.value.filter { it.repeatType == repeatType }

    override suspend fun insert(executor: Unit, alarm: Alarm): Long {
        val id = nextId++
        val stored = alarm.copy(id = id)
        store.update { it + stored }
        return id
    }

    override suspend fun update(executor: Unit, alarm: Alarm) {
        store.update { list -> list.map { if (it.id == alarm.id) alarm else it } }
    }

    override suspend fun delete(executor: Unit, alarm: Alarm) {
        store.update { list -> list.filter { it.id != alarm.id } }
    }
}