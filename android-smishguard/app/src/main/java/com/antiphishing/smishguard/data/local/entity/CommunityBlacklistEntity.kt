package com.antiphishing.smishguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "community_blacklist",
    indices = [Index(value = ["indicador"], unique = true)]
)
data class CommunityBlacklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val indicador: String,
    val tipo: String,
    val reportes: Int,
    val esMalicioso: Boolean,
    val actualizadoEn: Long = System.currentTimeMillis(),
    val pendienteSincronizacion: Boolean = false
)
