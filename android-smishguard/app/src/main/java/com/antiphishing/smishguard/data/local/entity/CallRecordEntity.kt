package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isBlocked"]),
        Index(value = ["threatLevel"])
    ]
)
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val threatScore: Int,
    val threatLevel: String,
    val isBlocked: Boolean,
    val action: String,
    val countryOrOrigin: String?,
    val factorsJson: String,
    val summaryReason: String
)
