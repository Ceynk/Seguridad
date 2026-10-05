package com.antiphishing.smishguard.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SmsDao_Impl implements SmsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SmsRecordEntity> __insertionAdapterOfSmsRecordEntity;

  private final EntityDeletionOrUpdateAdapter<SmsRecordEntity> __deletionAdapterOfSmsRecordEntity;

  private final EntityDeletionOrUpdateAdapter<SmsRecordEntity> __updateAdapterOfSmsRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfClearAllQuarantined;

  private final SharedSQLiteStatement __preparedStmtOfClearNonQuarantined;

  public SmsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSmsRecordEntity = new EntityInsertionAdapter<SmsRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sms_records` (`id`,`sender`,`rawBody`,`sanitizedBody`,`timestamp`,`threatScore`,`threatLevel`,`isQuarantined`,`factorsJson`,`extractedUrls`,`wasRestored`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SmsRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSender());
        statement.bindString(3, entity.getRawBody());
        statement.bindString(4, entity.getSanitizedBody());
        statement.bindLong(5, entity.getTimestamp());
        statement.bindLong(6, entity.getThreatScore());
        statement.bindString(7, entity.getThreatLevel());
        final int _tmp = entity.isQuarantined() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getFactorsJson());
        statement.bindString(10, entity.getExtractedUrls());
        final int _tmp_1 = entity.getWasRestored() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
      }
    };
    this.__deletionAdapterOfSmsRecordEntity = new EntityDeletionOrUpdateAdapter<SmsRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sms_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SmsRecordEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSmsRecordEntity = new EntityDeletionOrUpdateAdapter<SmsRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sms_records` SET `id` = ?,`sender` = ?,`rawBody` = ?,`sanitizedBody` = ?,`timestamp` = ?,`threatScore` = ?,`threatLevel` = ?,`isQuarantined` = ?,`factorsJson` = ?,`extractedUrls` = ?,`wasRestored` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SmsRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSender());
        statement.bindString(3, entity.getRawBody());
        statement.bindString(4, entity.getSanitizedBody());
        statement.bindLong(5, entity.getTimestamp());
        statement.bindLong(6, entity.getThreatScore());
        statement.bindString(7, entity.getThreatLevel());
        final int _tmp = entity.isQuarantined() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getFactorsJson());
        statement.bindString(10, entity.getExtractedUrls());
        final int _tmp_1 = entity.getWasRestored() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_records WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_records";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllQuarantined = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0";
        return _query;
      }
    };
    this.__preparedStmtOfClearNonQuarantined = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_records WHERE isQuarantined = 0 OR wasRestored = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertRecord(final SmsRecordEntity record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSmsRecordEntity.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecord(final SmsRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSmsRecordEntity.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecord(final SmsRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSmsRecordEntity.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllQuarantined(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllQuarantined.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAllQuarantined.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearNonQuarantined(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearNonQuarantined.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearNonQuarantined.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SmsRecordEntity>> getAllRecords() {
    final String _sql = "SELECT * FROM sms_records ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sms_records"}, new Callable<List<SmsRecordEntity>>() {
      @Override
      @NonNull
      public List<SmsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfRawBody = CursorUtil.getColumnIndexOrThrow(_cursor, "rawBody");
          final int _cursorIndexOfSanitizedBody = CursorUtil.getColumnIndexOrThrow(_cursor, "sanitizedBody");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfThreatScore = CursorUtil.getColumnIndexOrThrow(_cursor, "threatScore");
          final int _cursorIndexOfThreatLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "threatLevel");
          final int _cursorIndexOfIsQuarantined = CursorUtil.getColumnIndexOrThrow(_cursor, "isQuarantined");
          final int _cursorIndexOfFactorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "factorsJson");
          final int _cursorIndexOfExtractedUrls = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedUrls");
          final int _cursorIndexOfWasRestored = CursorUtil.getColumnIndexOrThrow(_cursor, "wasRestored");
          final List<SmsRecordEntity> _result = new ArrayList<SmsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SmsRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpRawBody;
            _tmpRawBody = _cursor.getString(_cursorIndexOfRawBody);
            final String _tmpSanitizedBody;
            _tmpSanitizedBody = _cursor.getString(_cursorIndexOfSanitizedBody);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpThreatScore;
            _tmpThreatScore = _cursor.getInt(_cursorIndexOfThreatScore);
            final String _tmpThreatLevel;
            _tmpThreatLevel = _cursor.getString(_cursorIndexOfThreatLevel);
            final boolean _tmpIsQuarantined;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsQuarantined);
            _tmpIsQuarantined = _tmp != 0;
            final String _tmpFactorsJson;
            _tmpFactorsJson = _cursor.getString(_cursorIndexOfFactorsJson);
            final String _tmpExtractedUrls;
            _tmpExtractedUrls = _cursor.getString(_cursorIndexOfExtractedUrls);
            final boolean _tmpWasRestored;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfWasRestored);
            _tmpWasRestored = _tmp_1 != 0;
            _item = new SmsRecordEntity(_tmpId,_tmpSender,_tmpRawBody,_tmpSanitizedBody,_tmpTimestamp,_tmpThreatScore,_tmpThreatLevel,_tmpIsQuarantined,_tmpFactorsJson,_tmpExtractedUrls,_tmpWasRestored);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<SmsRecordEntity>> getQuarantinedRecords() {
    final String _sql = "SELECT * FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sms_records"}, new Callable<List<SmsRecordEntity>>() {
      @Override
      @NonNull
      public List<SmsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfRawBody = CursorUtil.getColumnIndexOrThrow(_cursor, "rawBody");
          final int _cursorIndexOfSanitizedBody = CursorUtil.getColumnIndexOrThrow(_cursor, "sanitizedBody");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfThreatScore = CursorUtil.getColumnIndexOrThrow(_cursor, "threatScore");
          final int _cursorIndexOfThreatLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "threatLevel");
          final int _cursorIndexOfIsQuarantined = CursorUtil.getColumnIndexOrThrow(_cursor, "isQuarantined");
          final int _cursorIndexOfFactorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "factorsJson");
          final int _cursorIndexOfExtractedUrls = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedUrls");
          final int _cursorIndexOfWasRestored = CursorUtil.getColumnIndexOrThrow(_cursor, "wasRestored");
          final List<SmsRecordEntity> _result = new ArrayList<SmsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SmsRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpRawBody;
            _tmpRawBody = _cursor.getString(_cursorIndexOfRawBody);
            final String _tmpSanitizedBody;
            _tmpSanitizedBody = _cursor.getString(_cursorIndexOfSanitizedBody);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpThreatScore;
            _tmpThreatScore = _cursor.getInt(_cursorIndexOfThreatScore);
            final String _tmpThreatLevel;
            _tmpThreatLevel = _cursor.getString(_cursorIndexOfThreatLevel);
            final boolean _tmpIsQuarantined;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsQuarantined);
            _tmpIsQuarantined = _tmp != 0;
            final String _tmpFactorsJson;
            _tmpFactorsJson = _cursor.getString(_cursorIndexOfFactorsJson);
            final String _tmpExtractedUrls;
            _tmpExtractedUrls = _cursor.getString(_cursorIndexOfExtractedUrls);
            final boolean _tmpWasRestored;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfWasRestored);
            _tmpWasRestored = _tmp_1 != 0;
            _item = new SmsRecordEntity(_tmpId,_tmpSender,_tmpRawBody,_tmpSanitizedBody,_tmpTimestamp,_tmpThreatScore,_tmpThreatLevel,_tmpIsQuarantined,_tmpFactorsJson,_tmpExtractedUrls,_tmpWasRestored);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecordById(final long id,
      final Continuation<? super SmsRecordEntity> $completion) {
    final String _sql = "SELECT * FROM sms_records WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SmsRecordEntity>() {
      @Override
      @Nullable
      public SmsRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfRawBody = CursorUtil.getColumnIndexOrThrow(_cursor, "rawBody");
          final int _cursorIndexOfSanitizedBody = CursorUtil.getColumnIndexOrThrow(_cursor, "sanitizedBody");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfThreatScore = CursorUtil.getColumnIndexOrThrow(_cursor, "threatScore");
          final int _cursorIndexOfThreatLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "threatLevel");
          final int _cursorIndexOfIsQuarantined = CursorUtil.getColumnIndexOrThrow(_cursor, "isQuarantined");
          final int _cursorIndexOfFactorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "factorsJson");
          final int _cursorIndexOfExtractedUrls = CursorUtil.getColumnIndexOrThrow(_cursor, "extractedUrls");
          final int _cursorIndexOfWasRestored = CursorUtil.getColumnIndexOrThrow(_cursor, "wasRestored");
          final SmsRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpRawBody;
            _tmpRawBody = _cursor.getString(_cursorIndexOfRawBody);
            final String _tmpSanitizedBody;
            _tmpSanitizedBody = _cursor.getString(_cursorIndexOfSanitizedBody);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpThreatScore;
            _tmpThreatScore = _cursor.getInt(_cursorIndexOfThreatScore);
            final String _tmpThreatLevel;
            _tmpThreatLevel = _cursor.getString(_cursorIndexOfThreatLevel);
            final boolean _tmpIsQuarantined;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsQuarantined);
            _tmpIsQuarantined = _tmp != 0;
            final String _tmpFactorsJson;
            _tmpFactorsJson = _cursor.getString(_cursorIndexOfFactorsJson);
            final String _tmpExtractedUrls;
            _tmpExtractedUrls = _cursor.getString(_cursorIndexOfExtractedUrls);
            final boolean _tmpWasRestored;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfWasRestored);
            _tmpWasRestored = _tmp_1 != 0;
            _result = new SmsRecordEntity(_tmpId,_tmpSender,_tmpRawBody,_tmpSanitizedBody,_tmpTimestamp,_tmpThreatScore,_tmpThreatLevel,_tmpIsQuarantined,_tmpFactorsJson,_tmpExtractedUrls,_tmpWasRestored);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getTotalAnalyzedCount() {
    final String _sql = "SELECT COUNT(*) FROM sms_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sms_records"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getActiveQuarantinedCount() {
    final String _sql = "SELECT COUNT(*) FROM sms_records WHERE isQuarantined = 1 AND wasRestored = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sms_records"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getPhishingDetectedCount() {
    final String _sql = "SELECT COUNT(*) FROM sms_records WHERE threatLevel = 'PHISHING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sms_records"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
