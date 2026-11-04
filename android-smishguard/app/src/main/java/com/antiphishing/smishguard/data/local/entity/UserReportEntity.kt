package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reports")
data class UserReportEntity(
    @PrimaryKey
    val id: String,
    val assignedLevel: String,
    val userLevel: String,
    val timestamp: Long,
    val messageHash: String,
    val sender: String,
    val originalBodyEncrypted: String?
)
