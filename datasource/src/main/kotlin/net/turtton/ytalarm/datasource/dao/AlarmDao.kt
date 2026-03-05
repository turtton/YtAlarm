package net.turtton.ytalarm.datasource.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.turtton.ytalarm.datasource.entity.AlarmEntity

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms")
    fun getAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun getAllSync(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getFromIdSync(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE repeatType = :repeatType")
    suspend fun getMatchedSync(repeatType: AlarmEntity.RepeatType): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Transaction
    @Query("DELETE FROM alarms")
    suspend fun deleteAll()
}