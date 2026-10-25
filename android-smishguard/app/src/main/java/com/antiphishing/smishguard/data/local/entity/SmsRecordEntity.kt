package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isQuarantined"]),
        Index(value = ["threatLevel"]),
        Index(value = ["messageIdentity"], unique = true)
    ]
)
data class SmsRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val rawBody: String,
    val sanitizedBody: String,
    val timestamp: Long,
    val threatScore: Int,
    val threatLevel: String,
    val isQuarantined: Boolean,
    val factorsJson: String,
    val extractedUrls: String,
    val wasRestored: Boolean = false
    ,
    val messageIdentity: String = ""
)
