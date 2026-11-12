package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityBlacklistDao {

    @Query("SELECT * FROM community_blacklist ORDER BY actualizadoEn DESC")
    fun obtenerTodos(): Flow<List<CommunityBlacklistEntity>>

    @Query("SELECT indicador FROM community_blacklist")
    suspend fun obtenerIndicadores(): List<String>

    @Query("SELECT * FROM community_blacklist")
    suspend fun obtenerTodosAhora(): List<CommunityBlacklistEntity>

    @Query("SELECT * FROM community_blacklist WHERE pendienteSincronizacion = 1")
    suspend fun obtenerPendientes(): List<CommunityBlacklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(entrada: CommunityBlacklistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(entradas: List<CommunityBlacklistEntity>)

    @Query("DELETE FROM community_blacklist WHERE indicador = :indicador")
    suspend fun eliminarPorIndicador(indicador: String)

    @Query("DELETE FROM community_blacklist WHERE pendienteSincronizacion = 0")
    suspend fun eliminarSincronizados()
}
