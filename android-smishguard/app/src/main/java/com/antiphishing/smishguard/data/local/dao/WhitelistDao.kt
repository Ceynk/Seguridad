package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Query("SELECT * FROM whitelist_entries ORDER BY addedTimestamp DESC")
    fun getAllWhitelist(): Flow<List<WhitelistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist_entries WHERE senderOrNumber = :sender LIMIT 1)")
    suspend fun isWhitelisted(sender: String): Boolean

    @Query("SELECT senderOrNumber FROM whitelist_entries")
    suspend fun getAllSenders(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelist(entry: WhitelistEntity)

    @Delete
    suspend fun deleteWhitelist(entry: WhitelistEntity)

    @Query("DELETE FROM whitelist_entries WHERE senderOrNumber = :sender")
    suspend fun deleteBySender(sender: String)
}
