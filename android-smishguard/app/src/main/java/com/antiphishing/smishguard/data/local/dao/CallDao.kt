package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE isBlocked = 1 ORDER BY timestamp DESC")
    fun getBlockedCalls(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE isBlocked = 1 OR action != 'ALLOW' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFlaggedCalls(limit: Int = 5): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE isBlocked = 1 OR action != 'ALLOW' OR threatScore > 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getFilteredCallRecords(limit: Int = 200): Flow<List<CallRecordEntity>>

    @Query("SELECT COUNT(*) FROM call_records")
    fun getTotalCallsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_records WHERE isBlocked = 1")
    fun getBlockedCallsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(record: CallRecordEntity): Long

    @Delete
    suspend fun deleteCallRecord(record: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAll()

    @Query("DELETE FROM call_records WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
