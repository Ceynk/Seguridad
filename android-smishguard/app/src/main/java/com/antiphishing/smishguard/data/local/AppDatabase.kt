package com.antiphishing.smishguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antiphishing.smishguard.data.local.dao.CallDao
import com.antiphishing.smishguard.data.local.dao.CommunityBlacklistDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.SmsDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.dao.PersonalBlacklistDao
import com.antiphishing.smishguard.data.local.dao.UserReportDao
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.local.entity.PersonalBlacklistEntity
import com.antiphishing.smishguard.data.local.entity.UserReportEntity

@Database(
    entities = [
        SmsRecordEntity::class,
        CallRecordEntity::class,
        WhitelistEntity::class,
        CustomRuleEntity::class,
        CommunityBlacklistEntity::class,
        PersonalBlacklistEntity::class,
        UserReportEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao
    abstract fun callDao(): CallDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun personalBlacklistDao(): PersonalBlacklistDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun communityBlacklistDao(): CommunityBlacklistDao
    abstract fun userReportDao(): UserReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sms_records ADD COLUMN messageIdentity TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE sms_records SET messageIdentity = 'legacy:' || id WHERE messageIdentity = ''")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sms_records_messageIdentity ON sms_records(messageIdentity)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS community_blacklist (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "indicador TEXT NOT NULL, " +
                        "tipo TEXT NOT NULL, " +
                        "reportes INTEGER NOT NULL, " +
                        "esMalicioso INTEGER NOT NULL, " +
                        "actualizadoEn INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_community_blacklist_indicador " +
                        "ON community_blacklist(indicador)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE community_blacklist ADD COLUMN pendienteSincronizacion INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS personal_blacklist (" +
                        "numeroE164 TEXT NOT NULL PRIMARY KEY, " +
                        "pais TEXT, " +
                        "motivo TEXT NOT NULL, " +
                        "origen TEXT NOT NULL, " +
                        "nivelRiesgo TEXT NOT NULL, " +
                        "fechaRegistro INTEGER NOT NULL, " +
                        "activo INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_reports (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "assignedLevel TEXT NOT NULL, " +
                        "userLevel TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "messageHash TEXT NOT NULL, " +
                        "sender TEXT NOT NULL, " +
                        "originalBodyEncrypted TEXT)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            val currentInstance = INSTANCE
            if (currentInstance != null) {
                return currentInstance
            }

            return synchronized(this) {
                val existingInstance = INSTANCE
                if (existingInstance != null) {
                    existingInstance
                } else {
                    val newInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "smishguard_vault.db"
                    ).addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    ).build()
                    INSTANCE = newInstance
                    newInstance
                }

            }
        }
    }
}
