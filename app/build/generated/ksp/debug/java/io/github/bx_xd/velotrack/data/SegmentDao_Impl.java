package io.github.bx_xd.velotrack.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.github.bx_xd.velotrack.model.Segment;
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
public final class SegmentDao_Impl implements SegmentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Segment> __insertionAdapterOfSegment;

  private final EntityDeletionOrUpdateAdapter<Segment> __deletionAdapterOfSegment;

  public SegmentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSegment = new EntityInsertionAdapter<Segment>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `segments` (`id`,`name`,`activityId`,`startIndex`,`endIndex`,`startLat`,`startLng`,`endLat`,`endLng`,`distKm`,`elevGainM`,`durationSecs`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Segment entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getActivityId());
        statement.bindLong(4, entity.getStartIndex());
        statement.bindLong(5, entity.getEndIndex());
        statement.bindDouble(6, entity.getStartLat());
        statement.bindDouble(7, entity.getStartLng());
        statement.bindDouble(8, entity.getEndLat());
        statement.bindDouble(9, entity.getEndLng());
        statement.bindDouble(10, entity.getDistKm());
        statement.bindLong(11, entity.getElevGainM());
        statement.bindLong(12, entity.getDurationSecs());
        statement.bindLong(13, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSegment = new EntityDeletionOrUpdateAdapter<Segment>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `segments` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Segment entity) {
        statement.bindString(1, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final Segment segment, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSegment.insert(segment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Segment segment, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSegment.handle(segment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Segment>> getByActivityFlow(final String activityId) {
    final String _sql = "SELECT * FROM segments WHERE activityId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, activityId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"segments"}, new Callable<List<Segment>>() {
      @Override
      @NonNull
      public List<Segment> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfActivityId = CursorUtil.getColumnIndexOrThrow(_cursor, "activityId");
          final int _cursorIndexOfStartIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "startIndex");
          final int _cursorIndexOfEndIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "endIndex");
          final int _cursorIndexOfStartLat = CursorUtil.getColumnIndexOrThrow(_cursor, "startLat");
          final int _cursorIndexOfStartLng = CursorUtil.getColumnIndexOrThrow(_cursor, "startLng");
          final int _cursorIndexOfEndLat = CursorUtil.getColumnIndexOrThrow(_cursor, "endLat");
          final int _cursorIndexOfEndLng = CursorUtil.getColumnIndexOrThrow(_cursor, "endLng");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfDurationSecs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSecs");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Segment> _result = new ArrayList<Segment>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Segment _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpActivityId;
            _tmpActivityId = _cursor.getString(_cursorIndexOfActivityId);
            final int _tmpStartIndex;
            _tmpStartIndex = _cursor.getInt(_cursorIndexOfStartIndex);
            final int _tmpEndIndex;
            _tmpEndIndex = _cursor.getInt(_cursorIndexOfEndIndex);
            final double _tmpStartLat;
            _tmpStartLat = _cursor.getDouble(_cursorIndexOfStartLat);
            final double _tmpStartLng;
            _tmpStartLng = _cursor.getDouble(_cursorIndexOfStartLng);
            final double _tmpEndLat;
            _tmpEndLat = _cursor.getDouble(_cursorIndexOfEndLat);
            final double _tmpEndLng;
            _tmpEndLng = _cursor.getDouble(_cursorIndexOfEndLng);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final int _tmpDurationSecs;
            _tmpDurationSecs = _cursor.getInt(_cursorIndexOfDurationSecs);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Segment(_tmpId,_tmpName,_tmpActivityId,_tmpStartIndex,_tmpEndIndex,_tmpStartLat,_tmpStartLng,_tmpEndLat,_tmpEndLng,_tmpDistKm,_tmpElevGainM,_tmpDurationSecs,_tmpCreatedAt);
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
  public Object getById(final String id, final Continuation<? super Segment> $completion) {
    final String _sql = "SELECT * FROM segments WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Segment>() {
      @Override
      @Nullable
      public Segment call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfActivityId = CursorUtil.getColumnIndexOrThrow(_cursor, "activityId");
          final int _cursorIndexOfStartIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "startIndex");
          final int _cursorIndexOfEndIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "endIndex");
          final int _cursorIndexOfStartLat = CursorUtil.getColumnIndexOrThrow(_cursor, "startLat");
          final int _cursorIndexOfStartLng = CursorUtil.getColumnIndexOrThrow(_cursor, "startLng");
          final int _cursorIndexOfEndLat = CursorUtil.getColumnIndexOrThrow(_cursor, "endLat");
          final int _cursorIndexOfEndLng = CursorUtil.getColumnIndexOrThrow(_cursor, "endLng");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfDurationSecs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSecs");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Segment _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpActivityId;
            _tmpActivityId = _cursor.getString(_cursorIndexOfActivityId);
            final int _tmpStartIndex;
            _tmpStartIndex = _cursor.getInt(_cursorIndexOfStartIndex);
            final int _tmpEndIndex;
            _tmpEndIndex = _cursor.getInt(_cursorIndexOfEndIndex);
            final double _tmpStartLat;
            _tmpStartLat = _cursor.getDouble(_cursorIndexOfStartLat);
            final double _tmpStartLng;
            _tmpStartLng = _cursor.getDouble(_cursorIndexOfStartLng);
            final double _tmpEndLat;
            _tmpEndLat = _cursor.getDouble(_cursorIndexOfEndLat);
            final double _tmpEndLng;
            _tmpEndLng = _cursor.getDouble(_cursorIndexOfEndLng);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final int _tmpDurationSecs;
            _tmpDurationSecs = _cursor.getInt(_cursorIndexOfDurationSecs);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Segment(_tmpId,_tmpName,_tmpActivityId,_tmpStartIndex,_tmpEndIndex,_tmpStartLat,_tmpStartLng,_tmpEndLat,_tmpEndLng,_tmpDistKm,_tmpElevGainM,_tmpDurationSecs,_tmpCreatedAt);
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
  public Object getAll(final Continuation<? super List<Segment>> $completion) {
    final String _sql = "SELECT * FROM segments";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Segment>>() {
      @Override
      @NonNull
      public List<Segment> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfActivityId = CursorUtil.getColumnIndexOrThrow(_cursor, "activityId");
          final int _cursorIndexOfStartIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "startIndex");
          final int _cursorIndexOfEndIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "endIndex");
          final int _cursorIndexOfStartLat = CursorUtil.getColumnIndexOrThrow(_cursor, "startLat");
          final int _cursorIndexOfStartLng = CursorUtil.getColumnIndexOrThrow(_cursor, "startLng");
          final int _cursorIndexOfEndLat = CursorUtil.getColumnIndexOrThrow(_cursor, "endLat");
          final int _cursorIndexOfEndLng = CursorUtil.getColumnIndexOrThrow(_cursor, "endLng");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfDurationSecs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSecs");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Segment> _result = new ArrayList<Segment>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Segment _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpActivityId;
            _tmpActivityId = _cursor.getString(_cursorIndexOfActivityId);
            final int _tmpStartIndex;
            _tmpStartIndex = _cursor.getInt(_cursorIndexOfStartIndex);
            final int _tmpEndIndex;
            _tmpEndIndex = _cursor.getInt(_cursorIndexOfEndIndex);
            final double _tmpStartLat;
            _tmpStartLat = _cursor.getDouble(_cursorIndexOfStartLat);
            final double _tmpStartLng;
            _tmpStartLng = _cursor.getDouble(_cursorIndexOfStartLng);
            final double _tmpEndLat;
            _tmpEndLat = _cursor.getDouble(_cursorIndexOfEndLat);
            final double _tmpEndLng;
            _tmpEndLng = _cursor.getDouble(_cursorIndexOfEndLng);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final int _tmpDurationSecs;
            _tmpDurationSecs = _cursor.getInt(_cursorIndexOfDurationSecs);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Segment(_tmpId,_tmpName,_tmpActivityId,_tmpStartIndex,_tmpEndIndex,_tmpStartLat,_tmpStartLng,_tmpEndLat,_tmpEndLng,_tmpDistKm,_tmpElevGainM,_tmpDurationSecs,_tmpCreatedAt);
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
