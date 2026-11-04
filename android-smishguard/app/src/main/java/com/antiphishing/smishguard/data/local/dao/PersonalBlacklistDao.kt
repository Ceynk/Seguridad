package com.antiphishing.smishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antiphishing.smishguard.data.local.entity.PersonalBlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBlacklistDao {

    @Query("SELECT EXISTS(SELECT 1 FROM personal_blacklist WHERE numeroE164 = :numero AND activo = 1)")
    suspend fun contieneActivo(numero: String): Boolean

    @Query("SELECT * FROM personal_blacklist ORDER BY fechaRegistro DESC")
    fun obtenerTodos(): Flow<List<PersonalBlacklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(entrada: PersonalBlacklistEntity)

    @Query("UPDATE personal_blacklist SET activo = 0 WHERE numeroE164 = :numero")
    suspend fun desactivar(numero: String)
}
