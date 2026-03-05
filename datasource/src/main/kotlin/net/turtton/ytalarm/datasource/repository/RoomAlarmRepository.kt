package net.turtton.ytalarm.datasource.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.turtton.ytalarm.datasource.dao.AlarmDao
import net.turtton.ytalarm.datasource.mapper.toDomain
import net.turtton.ytalarm.datasource.mapper.toEntity
import net.turtton.ytalarm.kernel.entity.Alarm
import net.turtton.ytalarm.kernel.repository.AlarmRepository

/**
 * [AlarmRepository] の Room 実装。
 *
 * @param Executor DAO を保持する任意の型（例: AppDatabase）
 * @param daoProvider Executor から [AlarmDao] を取得するラムダ
 */
class RoomAlarmRepository<Executor>(private val daoProvider: (Executor) -> AlarmDao) :
    AlarmRepository<Executor> {

    override fun getAll(executor: Executor): Flow<List<Alarm>> =
        daoProvider(executor).getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSync(executor: Executor): List<Alarm> =
        daoProvider(executor).getAllSync().map { it.toDomain() }

    override suspend fun getFromId(executor: Executor, id: Long): Alarm? =
        daoProvider(executor).getFromIdSync(id)?.toDomain()

    override suspend fun getMatched(executor: Executor, repeatType: Alarm.RepeatType): List<Alarm> =
        daoProvider(executor).getMatchedSync(repeatType.toEntity()).map { it.toDomain() }

    override suspend fun insert(executor: Executor, alarm: Alarm): Long =
        daoProvider(executor).insert(alarm.toEntity())

    override suspend fun update(executor: Executor, alarm: Alarm) {
        daoProvider(executor).update(alarm.toEntity())
    }

    override suspend fun delete(executor: Executor, alarm: Alarm) {
        daoProvider(executor).delete(alarm.toEntity())
    }
}