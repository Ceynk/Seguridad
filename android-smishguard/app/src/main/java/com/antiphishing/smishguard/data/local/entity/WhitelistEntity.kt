package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist_entries",
    indices = [Index(value = ["senderOrNumber"], unique = true)]
)
data class WhitelistEntity(
    @PrimaryKey
    val senderOrNumber: String,
    val contactName: String?,
    val addedTimestamp: Long = System.currentTimeMillis()
)
