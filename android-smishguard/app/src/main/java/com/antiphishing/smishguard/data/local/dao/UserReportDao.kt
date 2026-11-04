package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antiphishing.smishguard.data.local.entity.UserReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReportDao {

    @Query("SELECT * FROM user_reports ORDER BY timestamp DESC")
    fun getAll(): Flow<List<UserReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: UserReportEntity)

    @Query("DELETE FROM user_reports WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM user_reports")
    suspend fun deleteAll()
}
