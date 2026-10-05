package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["isEnabled"])]
)
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val isRegex: Boolean = false,
    val description: String = "",
    val isEnabled: Boolean = true
)
