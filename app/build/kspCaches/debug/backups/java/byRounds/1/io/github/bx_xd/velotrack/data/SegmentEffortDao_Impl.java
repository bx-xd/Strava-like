package io.github.bx_xd.velotrack.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.github.bx_xd.velotrack.model.SegmentEffort;
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
public final class SegmentEffortDao_Impl implements SegmentEffortDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SegmentEffort> __insertionAdapterOfSegmentEffort;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySegment;

  public SegmentEffortDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSegmentEffort = new EntityInsertionAdapter<SegmentEffort>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `segment_efforts` (`id`,`segmentId`,`segmentName`,`activityId`,`startIndex`,`endIndex`,`durationSecs`,`distKm`,`avgSpeedKmh`,`date`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SegmentEffort entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSegmentId());
        statement.bindString(3, entity.getSegmentName());
        statement.bindString(4, entity.getActivityId());
        statement.bindLong(5, entity.getStartIndex());
        statement.bindLong(6, entity.getEndIndex());
        statement.bindLong(7, entity.getDurationSecs());
        statement.bindDouble(8, entity.getDistKm());
        statement.bindDouble(9, entity.getAvgSpeedKmh());
        statement.bindLong(10, entity.getDate());
      }
    };
    this.__preparedStmtOfDeleteBySegment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM segment_efforts WHERE segmentId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SegmentEffort effort, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSegmentEffort.insert(effort);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySegment(final String segmentId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySegment.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, segmentId);
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
          __preparedStmtOfDeleteBySegment.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SegmentEffort>> getByActivityFlow(final String activityId) {
    final String _sql = "SELECT * FROM segment_efforts WHERE activityId = ? ORDER BY durationSecs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, activityId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"segment_efforts"}, new Callable<List<SegmentEffort>>() {
      @Override
      @NonNull
      public List<SegmentEffort> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSegmentId = CursorUtil.getColumnIndexOrThrow(_cursor, "segmentId");
          final int _cursorIndexOfSegmentName = CursorUtil.getColumnIndexOrThrow(_cursor, "segmentName");
          final int _cursorIndexOfActivityId = CursorUtil.getColumnIndexOrThrow(_cursor, "activityId");
          final int _cursorIndexOfStartIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "startIndex");
          final int _cursorIndexOfEndIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "endIndex");
          final int _cursorIndexOfDurationSecs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSecs");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfAvgSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSpeedKmh");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<SegmentEffort> _result = new ArrayList<SegmentEffort>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SegmentEffort _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSegmentId;
            _tmpSegmentId = _cursor.getString(_cursorIndexOfSegmentId);
            final String _tmpSegmentName;
            _tmpSegmentName = _cursor.getString(_cursorIndexOfSegmentName);
            final String _tmpActivityId;
            _tmpActivityId = _cursor.getString(_cursorIndexOfActivityId);
            final int _tmpStartIndex;
            _tmpStartIndex = _cursor.getInt(_cursorIndexOfStartIndex);
            final int _tmpEndIndex;
            _tmpEndIndex = _cursor.getInt(_cursorIndexOfEndIndex);
            final int _tmpDurationSecs;
            _tmpDurationSecs = _cursor.getInt(_cursorIndexOfDurationSecs);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpAvgSpeedKmh;
            _tmpAvgSpeedKmh = _cursor.getDouble(_cursorIndexOfAvgSpeedKmh);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            _item = new SegmentEffort(_tmpId,_tmpSegmentId,_tmpSegmentName,_tmpActivityId,_tmpStartIndex,_tmpEndIndex,_tmpDurationSecs,_tmpDistKm,_tmpAvgSpeedKmh,_tmpDate);
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
  public Flow<List<SegmentEffort>> getBySegmentFlow(final String segmentId) {
    final String _sql = "SELECT * FROM segment_efforts WHERE segmentId = ? ORDER BY durationSecs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, segmentId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"segment_efforts"}, new Callable<List<SegmentEffort>>() {
      @Override
      @NonNull
      public List<SegmentEffort> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSegmentId = CursorUtil.getColumnIndexOrThrow(_cursor, "segmentId");
          final int _cursorIndexOfSegmentName = CursorUtil.getColumnIndexOrThrow(_cursor, "segmentName");
          final int _cursorIndexOfActivityId = CursorUtil.getColumnIndexOrThrow(_cursor, "activityId");
          final int _cursorIndexOfStartIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "startIndex");
          final int _cursorIndexOfEndIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "endIndex");
          final int _cursorIndexOfDurationSecs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSecs");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfAvgSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSpeedKmh");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<SegmentEffort> _result = new ArrayList<SegmentEffort>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SegmentEffort _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSegmentId;
            _tmpSegmentId = _cursor.getString(_cursorIndexOfSegmentId);
            final String _tmpSegmentName;
            _tmpSegmentName = _cursor.getString(_cursorIndexOfSegmentName);
            final String _tmpActivityId;
            _tmpActivityId = _cursor.getString(_cursorIndexOfActivityId);
            final int _tmpStartIndex;
            _tmpStartIndex = _cursor.getInt(_cursorIndexOfStartIndex);
            final int _tmpEndIndex;
            _tmpEndIndex = _cursor.getInt(_cursorIndexOfEndIndex);
            final int _tmpDurationSecs;
            _tmpDurationSecs = _cursor.getInt(_cursorIndexOfDurationSecs);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpAvgSpeedKmh;
            _tmpAvgSpeedKmh = _cursor.getDouble(_cursorIndexOfAvgSpeedKmh);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            _item = new SegmentEffort(_tmpId,_tmpSegmentId,_tmpSegmentName,_tmpActivityId,_tmpStartIndex,_tmpEndIndex,_tmpDurationSecs,_tmpDistKm,_tmpAvgSpeedKmh,_tmpDate);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
