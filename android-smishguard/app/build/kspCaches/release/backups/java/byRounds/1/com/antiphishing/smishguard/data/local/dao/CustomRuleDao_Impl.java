package com.antiphishing.smishguard.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class CustomRuleDao_Impl implements CustomRuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CustomRuleEntity> __insertionAdapterOfCustomRuleEntity;

  private final EntityDeletionOrUpdateAdapter<CustomRuleEntity> __deletionAdapterOfCustomRuleEntity;

  private final EntityDeletionOrUpdateAdapter<CustomRuleEntity> __updateAdapterOfCustomRuleEntity;

  public CustomRuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomRuleEntity = new EntityInsertionAdapter<CustomRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_rules` (`id`,`pattern`,`isRegex`,`description`,`isEnabled`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomRuleEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPattern());
        final int _tmp = entity.isRegex() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getDescription());
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
      }
    };
    this.__deletionAdapterOfCustomRuleEntity = new EntityDeletionOrUpdateAdapter<CustomRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `custom_rules` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomRuleEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCustomRuleEntity = new EntityDeletionOrUpdateAdapter<CustomRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `custom_rules` SET `id` = ?,`pattern` = ?,`isRegex` = ?,`description` = ?,`isEnabled` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomRuleEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPattern());
        final int _tmp = entity.isRegex() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getDescription());
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object insertRule(final CustomRuleEntity rule,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCustomRuleEntity.insertAndReturnId(rule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRule(final CustomRuleEntity rule,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCustomRuleEntity.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRule(final CustomRuleEntity rule,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCustomRuleEntity.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CustomRuleEntity>> getAllRules() {
    final String _sql = "SELECT * FROM custom_rules ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"custom_rules"}, new Callable<List<CustomRuleEntity>>() {
      @Override
      @NonNull
      public List<CustomRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "pattern");
          final int _cursorIndexOfIsRegex = CursorUtil.getColumnIndexOrThrow(_cursor, "isRegex");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final List<CustomRuleEntity> _result = new ArrayList<CustomRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomRuleEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPattern;
            _tmpPattern = _cursor.getString(_cursorIndexOfPattern);
            final boolean _tmpIsRegex;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRegex);
            _tmpIsRegex = _tmp != 0;
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            _item = new CustomRuleEntity(_tmpId,_tmpPattern,_tmpIsRegex,_tmpDescription,_tmpIsEnabled);
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
  public Object getActiveRules(final Continuation<? super List<CustomRuleEntity>> $completion) {
    final String _sql = "SELECT * FROM custom_rules WHERE isEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CustomRuleEntity>>() {
      @Override
      @NonNull
      public List<CustomRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "pattern");
          final int _cursorIndexOfIsRegex = CursorUtil.getColumnIndexOrThrow(_cursor, "isRegex");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final List<CustomRuleEntity> _result = new ArrayList<CustomRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomRuleEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPattern;
            _tmpPattern = _cursor.getString(_cursorIndexOfPattern);
            final boolean _tmpIsRegex;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRegex);
            _tmpIsRegex = _tmp != 0;
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            _item = new CustomRuleEntity(_tmpId,_tmpPattern,_tmpIsRegex,_tmpDescription,_tmpIsEnabled);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
