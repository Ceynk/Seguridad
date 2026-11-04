package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_blacklist")
data class PersonalBlacklistEntity(
    @PrimaryKey val numeroE164: String,
    val pais: String?,
    val motivo: String,
    val origen: String,
    val nivelRiesgo: String,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val activo: Boolean = true
)
