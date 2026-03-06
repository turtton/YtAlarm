package net.turtton.ytalarm.kernel.repository

import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.kernel.entity.Alarm

interface AlarmRepository<Executor> {
    fun getAll(executor: Executor): Flow<List<Alarm>>

    suspend fun getAllSync(executor: Executor): List<Alarm>

    suspend fun getFromId(executor: Executor, id: Long): Alarm?

    suspend fun getMatched(executor: Executor, repeatType: Alarm.RepeatType): List<Alarm>

    suspend fun insert(executor: Executor, alarm: Alarm): Long

    suspend fun update(executor: Executor, alarm: Alarm)

    suspend fun delete(executor: Executor, alarm: Alarm)
}