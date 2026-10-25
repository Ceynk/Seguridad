package com.antiphishing.smishguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antiphishing.smishguard.data.local.dao.CallDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.SmsDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity

@Database(
    entities = [
        SmsRecordEntity::class,
        CallRecordEntity::class,
        WhitelistEntity::class,
        CustomRuleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao
    abstract fun callDao(): CallDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun customRuleDao(): CustomRuleDao

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
                    ).addMigrations(MIGRATION_3_4).build()
                    INSTANCE = newInstance
                    newInstance
                }

            }
        }
    }
}
