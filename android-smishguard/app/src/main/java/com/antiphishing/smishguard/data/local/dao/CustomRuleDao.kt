package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {

    @Query("SELECT * FROM custom_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<CustomRuleEntity>>

    @Query("SELECT * FROM custom_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<CustomRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CustomRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CustomRuleEntity)
}
