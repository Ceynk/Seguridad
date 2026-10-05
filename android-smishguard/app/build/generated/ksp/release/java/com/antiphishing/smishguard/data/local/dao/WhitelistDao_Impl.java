package com.antiphishing.smishguard.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
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
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
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
public final class WhitelistDao_Impl implements WhitelistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WhitelistEntity> __insertionAdapterOfWhitelistEntity;

  private final EntityDeletionOrUpdateAdapter<WhitelistEntity> __deletionAdapterOfWhitelistEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySender;

  public WhitelistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWhitelistEntity = new EntityInsertionAdapter<WhitelistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `whitelist_entries` (`senderOrNumber`,`contactName`,`addedTimestamp`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WhitelistEntity entity) {
        statement.bindString(1, entity.getSenderOrNumber());
        if (entity.getContactName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getContactName());
        }
        statement.bindLong(3, entity.getAddedTimestamp());
      }
    };
    this.__deletionAdapterOfWhitelistEntity = new EntityDeletionOrUpdateAdapter<WhitelistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `whitelist_entries` WHERE `senderOrNumber` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WhitelistEntity entity) {
        statement.bindString(1, entity.getSenderOrNumber());
      }
    };
    this.__preparedStmtOfDeleteBySender = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM whitelist_entries WHERE senderOrNumber = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertWhitelist(final WhitelistEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWhitelistEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteWhitelist(final WhitelistEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWhitelistEntity.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySender(final String sender, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySender.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sender);
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
          __preparedStmtOfDeleteBySender.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WhitelistEntity>> getAllWhitelist() {
    final String _sql = "SELECT * FROM whitelist_entries ORDER BY addedTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"whitelist_entries"}, new Callable<List<WhitelistEntity>>() {
      @Override
      @NonNull
      public List<WhitelistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSenderOrNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "senderOrNumber");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfAddedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "addedTimestamp");
          final List<WhitelistEntity> _result = new ArrayList<WhitelistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WhitelistEntity _item;
            final String _tmpSenderOrNumber;
            _tmpSenderOrNumber = _cursor.getString(_cursorIndexOfSenderOrNumber);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final long _tmpAddedTimestamp;
            _tmpAddedTimestamp = _cursor.getLong(_cursorIndexOfAddedTimestamp);
            _item = new WhitelistEntity(_tmpSenderOrNumber,_tmpContactName,_tmpAddedTimestamp);
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
  public Object isWhitelisted(final String sender,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM whitelist_entries WHERE senderOrNumber = ? LIMIT 1)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sender);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
