package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    @Query("SELECT * FROM sms_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SmsRecordEntity>>

    @Query("SELECT * FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0 ORDER BY timestamp DESC")
    fun getQuarantinedRecords(): Flow<List<SmsRecordEntity>>

    @Query(
        "SELECT * FROM sms_records " +
            "WHERE isQuarantined = 1 AND wasRestored = 0 " +
            "ORDER BY timestamp DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getQuarantinedPage(limit: Int, offset: Int): List<SmsRecordEntity>

    @Query("SELECT * FROM sms_records WHERE id = :id")
    suspend fun getRecordById(id: Long): SmsRecordEntity?

    @Query("SELECT COUNT(*) FROM sms_records")
    fun getTotalAnalyzedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0")
    fun getActiveQuarantinedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_records WHERE threatLevel = 'PHISHING'")
    fun getPhishingDetectedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: SmsRecordEntity): Long

    @Update
    suspend fun updateRecord(record: SmsRecordEntity)

    @Delete
    suspend fun deleteRecord(record: SmsRecordEntity)

    @Query("DELETE FROM sms_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sms_records")
    suspend fun clearAll()

    @Query("DELETE FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0")
    suspend fun clearAllQuarantined()

    @Query("DELETE FROM sms_records WHERE isQuarantined = 0 OR wasRestored = 1")
    suspend fun clearNonQuarantined()

    @Query("DELETE FROM sms_records WHERE (isQuarantined = 0 OR wasRestored = 1) AND timestamp < :cutoff")
    suspend fun deleteNonQuarantinedOlderThan(cutoff: Long): Int

    @Query("DELETE FROM sms_records WHERE timestamp < :cutoff")
    suspend fun deleteAllOlderThan(cutoff: Long): Int
}
