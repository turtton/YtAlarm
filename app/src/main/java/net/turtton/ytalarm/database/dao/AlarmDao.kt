package net.turtton.ytalarm.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.structure.Alarm

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms")
    fun getAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getFromId(id: Int): Flow<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm)

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()
}