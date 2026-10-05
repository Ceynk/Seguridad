package com.antiphishing.smishguard.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.antiphishing.smishguard.data.local.dao.CallDao;
import com.antiphishing.smishguard.data.local.dao.CallDao_Impl;
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao;
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao_Impl;
import com.antiphishing.smishguard.data.local.dao.SmsDao;
import com.antiphishing.smishguard.data.local.dao.SmsDao_Impl;
import com.antiphishing.smishguard.data.local.dao.WhitelistDao;
import com.antiphishing.smishguard.data.local.dao.WhitelistDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile SmsDao _smsDao;

  private volatile CallDao _callDao;

  private volatile WhitelistDao _whitelistDao;

  private volatile CustomRuleDao _customRuleDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sms_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, `rawBody` TEXT NOT NULL, `sanitizedBody` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `threatScore` INTEGER NOT NULL, `threatLevel` TEXT NOT NULL, `isQuarantined` INTEGER NOT NULL, `factorsJson` TEXT NOT NULL, `extractedUrls` TEXT NOT NULL, `wasRestored` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_records_timestamp` ON `sms_records` (`timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_records_isQuarantined` ON `sms_records` (`isQuarantined`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_records_threatLevel` ON `sms_records` (`threatLevel`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `call_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phoneNumber` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `threatScore` INTEGER NOT NULL, `threatLevel` TEXT NOT NULL, `isBlocked` INTEGER NOT NULL, `action` TEXT NOT NULL, `countryOrOrigin` TEXT, `factorsJson` TEXT NOT NULL, `summaryReason` TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_records_timestamp` ON `call_records` (`timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_records_isBlocked` ON `call_records` (`isBlocked`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_records_threatLevel` ON `call_records` (`threatLevel`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `whitelist_entries` (`senderOrNumber` TEXT NOT NULL, `contactName` TEXT, `addedTimestamp` INTEGER NOT NULL, PRIMARY KEY(`senderOrNumber`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_whitelist_entries_senderOrNumber` ON `whitelist_entries` (`senderOrNumber`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pattern` TEXT NOT NULL, `isRegex` INTEGER NOT NULL, `description` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_rules_isEnabled` ON `custom_rules` (`isEnabled`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '427aca52da5e8e65b23eb9182220013e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `sms_records`");
        db.execSQL("DROP TABLE IF EXISTS `call_records`");
        db.execSQL("DROP TABLE IF EXISTS `whitelist_entries`");
        db.execSQL("DROP TABLE IF EXISTS `custom_rules`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSmsRecords = new HashMap<String, TableInfo.Column>(11);
        _columnsSmsRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("sender", new TableInfo.Column("sender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("rawBody", new TableInfo.Column("rawBody", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("sanitizedBody", new TableInfo.Column("sanitizedBody", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("threatScore", new TableInfo.Column("threatScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("threatLevel", new TableInfo.Column("threatLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("isQuarantined", new TableInfo.Column("isQuarantined", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("factorsJson", new TableInfo.Column("factorsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("extractedUrls", new TableInfo.Column("extractedUrls", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSmsRecords.put("wasRestored", new TableInfo.Column("wasRestored", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSmsRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSmsRecords = new HashSet<TableInfo.Index>(3);
        _indicesSmsRecords.add(new TableInfo.Index("index_sms_records_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        _indicesSmsRecords.add(new TableInfo.Index("index_sms_records_isQuarantined", false, Arrays.asList("isQuarantined"), Arrays.asList("ASC")));
        _indicesSmsRecords.add(new TableInfo.Index("index_sms_records_threatLevel", false, Arrays.asList("threatLevel"), Arrays.asList("ASC")));
        final TableInfo _infoSmsRecords = new TableInfo("sms_records", _columnsSmsRecords, _foreignKeysSmsRecords, _indicesSmsRecords);
        final TableInfo _existingSmsRecords = TableInfo.read(db, "sms_records");
        if (!_infoSmsRecords.equals(_existingSmsRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "sms_records(com.antiphishing.smishguard.data.local.entity.SmsRecordEntity).\n"
                  + " Expected:\n" + _infoSmsRecords + "\n"
                  + " Found:\n" + _existingSmsRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsCallRecords = new HashMap<String, TableInfo.Column>(10);
        _columnsCallRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("threatScore", new TableInfo.Column("threatScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("threatLevel", new TableInfo.Column("threatLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("countryOrOrigin", new TableInfo.Column("countryOrOrigin", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("factorsJson", new TableInfo.Column("factorsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallRecords.put("summaryReason", new TableInfo.Column("summaryReason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCallRecords = new HashSet<TableInfo.Index>(3);
        _indicesCallRecords.add(new TableInfo.Index("index_call_records_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        _indicesCallRecords.add(new TableInfo.Index("index_call_records_isBlocked", false, Arrays.asList("isBlocked"), Arrays.asList("ASC")));
        _indicesCallRecords.add(new TableInfo.Index("index_call_records_threatLevel", false, Arrays.asList("threatLevel"), Arrays.asList("ASC")));
        final TableInfo _infoCallRecords = new TableInfo("call_records", _columnsCallRecords, _foreignKeysCallRecords, _indicesCallRecords);
        final TableInfo _existingCallRecords = TableInfo.read(db, "call_records");
        if (!_infoCallRecords.equals(_existingCallRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "call_records(com.antiphishing.smishguard.data.local.entity.CallRecordEntity).\n"
                  + " Expected:\n" + _infoCallRecords + "\n"
                  + " Found:\n" + _existingCallRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsWhitelistEntries = new HashMap<String, TableInfo.Column>(3);
        _columnsWhitelistEntries.put("senderOrNumber", new TableInfo.Column("senderOrNumber", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWhitelistEntries.put("contactName", new TableInfo.Column("contactName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWhitelistEntries.put("addedTimestamp", new TableInfo.Column("addedTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWhitelistEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWhitelistEntries = new HashSet<TableInfo.Index>(1);
        _indicesWhitelistEntries.add(new TableInfo.Index("index_whitelist_entries_senderOrNumber", true, Arrays.asList("senderOrNumber"), Arrays.asList("ASC")));
        final TableInfo _infoWhitelistEntries = new TableInfo("whitelist_entries", _columnsWhitelistEntries, _foreignKeysWhitelistEntries, _indicesWhitelistEntries);
        final TableInfo _existingWhitelistEntries = TableInfo.read(db, "whitelist_entries");
        if (!_infoWhitelistEntries.equals(_existingWhitelistEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "whitelist_entries(com.antiphishing.smishguard.data.local.entity.WhitelistEntity).\n"
                  + " Expected:\n" + _infoWhitelistEntries + "\n"
                  + " Found:\n" + _existingWhitelistEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomRules = new HashMap<String, TableInfo.Column>(5);
        _columnsCustomRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomRules.put("pattern", new TableInfo.Column("pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomRules.put("isRegex", new TableInfo.Column("isRegex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomRules.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomRules.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomRules = new HashSet<TableInfo.Index>(1);
        _indicesCustomRules.add(new TableInfo.Index("index_custom_rules_isEnabled", false, Arrays.asList("isEnabled"), Arrays.asList("ASC")));
        final TableInfo _infoCustomRules = new TableInfo("custom_rules", _columnsCustomRules, _foreignKeysCustomRules, _indicesCustomRules);
        final TableInfo _existingCustomRules = TableInfo.read(db, "custom_rules");
        if (!_infoCustomRules.equals(_existingCustomRules)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_rules(com.antiphishing.smishguard.data.local.entity.CustomRuleEntity).\n"
                  + " Expected:\n" + _infoCustomRules + "\n"
                  + " Found:\n" + _existingCustomRules);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "427aca52da5e8e65b23eb9182220013e", "e35f4bf164ce79809f38512ea8b41d0e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "sms_records","call_records","whitelist_entries","custom_rules");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `sms_records`");
      _db.execSQL("DELETE FROM `call_records`");
      _db.execSQL("DELETE FROM `whitelist_entries`");
      _db.execSQL("DELETE FROM `custom_rules`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SmsDao.class, SmsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CallDao.class, CallDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WhitelistDao.class, WhitelistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomRuleDao.class, CustomRuleDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SmsDao smsDao() {
    if (_smsDao != null) {
      return _smsDao;
    } else {
      synchronized(this) {
        if(_smsDao == null) {
          _smsDao = new SmsDao_Impl(this);
        }
        return _smsDao;
      }
    }
  }

  @Override
  public CallDao callDao() {
    if (_callDao != null) {
      return _callDao;
    } else {
      synchronized(this) {
        if(_callDao == null) {
          _callDao = new CallDao_Impl(this);
        }
        return _callDao;
      }
    }
  }

  @Override
  public WhitelistDao whitelistDao() {
    if (_whitelistDao != null) {
      return _whitelistDao;
    } else {
      synchronized(this) {
        if(_whitelistDao == null) {
          _whitelistDao = new WhitelistDao_Impl(this);
        }
        return _whitelistDao;
      }
    }
  }

  @Override
  public CustomRuleDao customRuleDao() {
    if (_customRuleDao != null) {
      return _customRuleDao;
    } else {
      synchronized(this) {
        if(_customRuleDao == null) {
          _customRuleDao = new CustomRuleDao_Impl(this);
        }
        return _customRuleDao;
      }
    }
  }
}
