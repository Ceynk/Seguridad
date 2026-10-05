package com.antiphishing.smishguard.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity;
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
public final class CallDao_Impl implements CallDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallRecordEntity> __insertionAdapterOfCallRecordEntity;

  private final EntityDeletionOrUpdateAdapter<CallRecordEntity> __deletionAdapterOfCallRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public CallDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallRecordEntity = new EntityInsertionAdapter<CallRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_records` (`id`,`phoneNumber`,`timestamp`,`threatScore`,`threatLevel`,`isBlocked`,`action`,`countryOrOrigin`,`factorsJson`,`summaryReason`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPhoneNumber());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindLong(4, entity.getThreatScore());
        statement.bindString(5, entity.getThreatLevel());
        final int _tmp = entity.isBlocked() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getAction());
        if (entity.getCountryOrOrigin() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCountryOrOrigin());
        }
        statement.bindString(9, entity.getFactorsJson());
        statement.bindString(10, entity.getSummaryReason());
      }
    };
    this.__deletionAdapterOfCallRecordEntity = new EntityDeletionOrUpdateAdapter<CallRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `call_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallRecordEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_records WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_records";
        return _query;
      }
    };
  }

  @Override
  public Object insertCallRecord(final CallRecordEntity record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCallRecordEntity.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCallRecord(final CallRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCallRecordEntity.handle(record);
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
  public Flow<List<CallRecordEntity>> getAllCallRecords() {
    final String _sql = "SELECT * FROM call_records ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_records"}, new Callable<List<CallRecordEntity>>() {
      @Override
      @NonNull
      public List<CallRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfThreatScore = CursorUtil.getColumnIndexOrThrow(_cursor, "threatScore");
          final int _cursorIndexOfThreatLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "threatLevel");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfCountryOrOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "countryOrOrigin");
          final int _cursorIndexOfFactorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "factorsJson");
          final int _cursorIndexOfSummaryReason = CursorUtil.getColumnIndexOrThrow(_cursor, "summaryReason");
          final List<CallRecordEntity> _result = new ArrayList<CallRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpThreatScore;
            _tmpThreatScore = _cursor.getInt(_cursorIndexOfThreatScore);
            final String _tmpThreatLevel;
            _tmpThreatLevel = _cursor.getString(_cursorIndexOfThreatLevel);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpCountryOrOrigin;
            if (_cursor.isNull(_cursorIndexOfCountryOrOrigin)) {
              _tmpCountryOrOrigin = null;
            } else {
              _tmpCountryOrOrigin = _cursor.getString(_cursorIndexOfCountryOrOrigin);
            }
            final String _tmpFactorsJson;
            _tmpFactorsJson = _cursor.getString(_cursorIndexOfFactorsJson);
            final String _tmpSummaryReason;
            _tmpSummaryReason = _cursor.getString(_cursorIndexOfSummaryReason);
            _item = new CallRecordEntity(_tmpId,_tmpPhoneNumber,_tmpTimestamp,_tmpThreatScore,_tmpThreatLevel,_tmpIsBlocked,_tmpAction,_tmpCountryOrOrigin,_tmpFactorsJson,_tmpSummaryReason);
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
  public Flow<List<CallRecordEntity>> getBlockedCalls() {
    final String _sql = "SELECT * FROM call_records WHERE isBlocked = 1 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_records"}, new Callable<List<CallRecordEntity>>() {
      @Override
      @NonNull
      public List<CallRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfThreatScore = CursorUtil.getColumnIndexOrThrow(_cursor, "threatScore");
          final int _cursorIndexOfThreatLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "threatLevel");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfCountryOrOrigin = CursorUtil.getColumnIndexOrThrow(_cursor, "countryOrOrigin");
          final int _cursorIndexOfFactorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "factorsJson");
          final int _cursorIndexOfSummaryReason = CursorUtil.getColumnIndexOrThrow(_cursor, "summaryReason");
          final List<CallRecordEntity> _result = new ArrayList<CallRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpThreatScore;
            _tmpThreatScore = _cursor.getInt(_cursorIndexOfThreatScore);
            final String _tmpThreatLevel;
            _tmpThreatLevel = _cursor.getString(_cursorIndexOfThreatLevel);
            final boolean _tmpIsBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp != 0;
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpCountryOrOrigin;
            if (_cursor.isNull(_cursorIndexOfCountryOrOrigin)) {
              _tmpCountryOrOrigin = null;
            } else {
              _tmpCountryOrOrigin = _cursor.getString(_cursorIndexOfCountryOrOrigin);
            }
            final String _tmpFactorsJson;
            _tmpFactorsJson = _cursor.getString(_cursorIndexOfFactorsJson);
            final String _tmpSummaryReason;
            _tmpSummaryReason = _cursor.getString(_cursorIndexOfSummaryReason);
            _item = new CallRecordEntity(_tmpId,_tmpPhoneNumber,_tmpTimestamp,_tmpThreatScore,_tmpThreatLevel,_tmpIsBlocked,_tmpAction,_tmpCountryOrOrigin,_tmpFactorsJson,_tmpSummaryReason);
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
  public Flow<Integer> getTotalCallsCount() {
    final String _sql = "SELECT COUNT(*) FROM call_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_records"}, new Callable<Integer>() {
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
  public Flow<Integer> getBlockedCallsCount() {
    final String _sql = "SELECT COUNT(*) FROM call_records WHERE isBlocked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_records"}, new Callable<Integer>() {
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
