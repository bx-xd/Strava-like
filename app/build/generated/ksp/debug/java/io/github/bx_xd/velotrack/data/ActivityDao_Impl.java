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
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.github.bx_xd.velotrack.model.Activity;
import io.github.bx_xd.velotrack.model.ActivityConverters;
import io.github.bx_xd.velotrack.model.BikeType;
import io.github.bx_xd.velotrack.model.GpsPoint;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class ActivityDao_Impl implements ActivityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Activity> __insertionAdapterOfActivity;

  private final ActivityConverters __activityConverters = new ActivityConverters();

  private final EntityDeletionOrUpdateAdapter<Activity> __deletionAdapterOfActivity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public ActivityDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfActivity = new EntityInsertionAdapter<Activity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `activities` (`id`,`title`,`type`,`date`,`distKm`,`durationMin`,`elevGainM`,`maxSpeedKmh`,`avgPowerW`,`avgHrBpm`,`notes`,`hasTrace`,`points`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Activity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        final String _tmp = __activityConverters.fromBikeType(entity.getType());
        statement.bindString(3, _tmp);
        statement.bindLong(4, entity.getDate());
        statement.bindDouble(5, entity.getDistKm());
        statement.bindDouble(6, entity.getDurationMin());
        statement.bindLong(7, entity.getElevGainM());
        statement.bindDouble(8, entity.getMaxSpeedKmh());
        if (entity.getAvgPowerW() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getAvgPowerW());
        }
        if (entity.getAvgHrBpm() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getAvgHrBpm());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
        final int _tmp_1 = entity.getHasTrace() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        final String _tmp_2 = __activityConverters.fromPoints(entity.getPoints());
        statement.bindString(13, _tmp_2);
      }
    };
    this.__deletionAdapterOfActivity = new EntityDeletionOrUpdateAdapter<Activity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `activities` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Activity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM activities WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Activity activity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfActivity.insert(activity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Activity activity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfActivity.handle(activity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
  public Flow<List<Activity>> getAllFlow() {
    final String _sql = "SELECT * FROM activities ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"activities"}, new Callable<List<Activity>>() {
      @Override
      @NonNull
      public List<Activity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMin");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfMaxSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedKmh");
          final int _cursorIndexOfAvgPowerW = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPowerW");
          final int _cursorIndexOfAvgHrBpm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgHrBpm");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfHasTrace = CursorUtil.getColumnIndexOrThrow(_cursor, "hasTrace");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final List<Activity> _result = new ArrayList<Activity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Activity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final BikeType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __activityConverters.toBikeType(_tmp);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpDurationMin;
            _tmpDurationMin = _cursor.getDouble(_cursorIndexOfDurationMin);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final double _tmpMaxSpeedKmh;
            _tmpMaxSpeedKmh = _cursor.getDouble(_cursorIndexOfMaxSpeedKmh);
            final Integer _tmpAvgPowerW;
            if (_cursor.isNull(_cursorIndexOfAvgPowerW)) {
              _tmpAvgPowerW = null;
            } else {
              _tmpAvgPowerW = _cursor.getInt(_cursorIndexOfAvgPowerW);
            }
            final Integer _tmpAvgHrBpm;
            if (_cursor.isNull(_cursorIndexOfAvgHrBpm)) {
              _tmpAvgHrBpm = null;
            } else {
              _tmpAvgHrBpm = _cursor.getInt(_cursorIndexOfAvgHrBpm);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpHasTrace;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasTrace);
            _tmpHasTrace = _tmp_1 != 0;
            final List<GpsPoint> _tmpPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __activityConverters.toPoints(_tmp_2);
            _item = new Activity(_tmpId,_tmpTitle,_tmpType,_tmpDate,_tmpDistKm,_tmpDurationMin,_tmpElevGainM,_tmpMaxSpeedKmh,_tmpAvgPowerW,_tmpAvgHrBpm,_tmpNotes,_tmpHasTrace,_tmpPoints);
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
  public Object getAll(final Continuation<? super List<Activity>> $completion) {
    final String _sql = "SELECT * FROM activities ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Activity>>() {
      @Override
      @NonNull
      public List<Activity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMin");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfMaxSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedKmh");
          final int _cursorIndexOfAvgPowerW = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPowerW");
          final int _cursorIndexOfAvgHrBpm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgHrBpm");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfHasTrace = CursorUtil.getColumnIndexOrThrow(_cursor, "hasTrace");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final List<Activity> _result = new ArrayList<Activity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Activity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final BikeType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __activityConverters.toBikeType(_tmp);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpDurationMin;
            _tmpDurationMin = _cursor.getDouble(_cursorIndexOfDurationMin);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final double _tmpMaxSpeedKmh;
            _tmpMaxSpeedKmh = _cursor.getDouble(_cursorIndexOfMaxSpeedKmh);
            final Integer _tmpAvgPowerW;
            if (_cursor.isNull(_cursorIndexOfAvgPowerW)) {
              _tmpAvgPowerW = null;
            } else {
              _tmpAvgPowerW = _cursor.getInt(_cursorIndexOfAvgPowerW);
            }
            final Integer _tmpAvgHrBpm;
            if (_cursor.isNull(_cursorIndexOfAvgHrBpm)) {
              _tmpAvgHrBpm = null;
            } else {
              _tmpAvgHrBpm = _cursor.getInt(_cursorIndexOfAvgHrBpm);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpHasTrace;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasTrace);
            _tmpHasTrace = _tmp_1 != 0;
            final List<GpsPoint> _tmpPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __activityConverters.toPoints(_tmp_2);
            _item = new Activity(_tmpId,_tmpTitle,_tmpType,_tmpDate,_tmpDistKm,_tmpDurationMin,_tmpElevGainM,_tmpMaxSpeedKmh,_tmpAvgPowerW,_tmpAvgHrBpm,_tmpNotes,_tmpHasTrace,_tmpPoints);
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

  @Override
  public Object getById(final String id, final Continuation<? super Activity> $completion) {
    final String _sql = "SELECT * FROM activities WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Activity>() {
      @Override
      @Nullable
      public Activity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMin");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfMaxSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedKmh");
          final int _cursorIndexOfAvgPowerW = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPowerW");
          final int _cursorIndexOfAvgHrBpm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgHrBpm");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfHasTrace = CursorUtil.getColumnIndexOrThrow(_cursor, "hasTrace");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final Activity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final BikeType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __activityConverters.toBikeType(_tmp);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpDurationMin;
            _tmpDurationMin = _cursor.getDouble(_cursorIndexOfDurationMin);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final double _tmpMaxSpeedKmh;
            _tmpMaxSpeedKmh = _cursor.getDouble(_cursorIndexOfMaxSpeedKmh);
            final Integer _tmpAvgPowerW;
            if (_cursor.isNull(_cursorIndexOfAvgPowerW)) {
              _tmpAvgPowerW = null;
            } else {
              _tmpAvgPowerW = _cursor.getInt(_cursorIndexOfAvgPowerW);
            }
            final Integer _tmpAvgHrBpm;
            if (_cursor.isNull(_cursorIndexOfAvgHrBpm)) {
              _tmpAvgHrBpm = null;
            } else {
              _tmpAvgHrBpm = _cursor.getInt(_cursorIndexOfAvgHrBpm);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpHasTrace;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasTrace);
            _tmpHasTrace = _tmp_1 != 0;
            final List<GpsPoint> _tmpPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __activityConverters.toPoints(_tmp_2);
            _result = new Activity(_tmpId,_tmpTitle,_tmpType,_tmpDate,_tmpDistKm,_tmpDurationMin,_tmpElevGainM,_tmpMaxSpeedKmh,_tmpAvgPowerW,_tmpAvgHrBpm,_tmpNotes,_tmpHasTrace,_tmpPoints);
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
  public Object getSince(final long since, final Continuation<? super List<Activity>> $completion) {
    final String _sql = "SELECT * FROM activities WHERE date >= ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Activity>>() {
      @Override
      @NonNull
      public List<Activity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDistKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distKm");
          final int _cursorIndexOfDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMin");
          final int _cursorIndexOfElevGainM = CursorUtil.getColumnIndexOrThrow(_cursor, "elevGainM");
          final int _cursorIndexOfMaxSpeedKmh = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedKmh");
          final int _cursorIndexOfAvgPowerW = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPowerW");
          final int _cursorIndexOfAvgHrBpm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgHrBpm");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfHasTrace = CursorUtil.getColumnIndexOrThrow(_cursor, "hasTrace");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final List<Activity> _result = new ArrayList<Activity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Activity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final BikeType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __activityConverters.toBikeType(_tmp);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpDistKm;
            _tmpDistKm = _cursor.getDouble(_cursorIndexOfDistKm);
            final double _tmpDurationMin;
            _tmpDurationMin = _cursor.getDouble(_cursorIndexOfDurationMin);
            final int _tmpElevGainM;
            _tmpElevGainM = _cursor.getInt(_cursorIndexOfElevGainM);
            final double _tmpMaxSpeedKmh;
            _tmpMaxSpeedKmh = _cursor.getDouble(_cursorIndexOfMaxSpeedKmh);
            final Integer _tmpAvgPowerW;
            if (_cursor.isNull(_cursorIndexOfAvgPowerW)) {
              _tmpAvgPowerW = null;
            } else {
              _tmpAvgPowerW = _cursor.getInt(_cursorIndexOfAvgPowerW);
            }
            final Integer _tmpAvgHrBpm;
            if (_cursor.isNull(_cursorIndexOfAvgHrBpm)) {
              _tmpAvgHrBpm = null;
            } else {
              _tmpAvgHrBpm = _cursor.getInt(_cursorIndexOfAvgHrBpm);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpHasTrace;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasTrace);
            _tmpHasTrace = _tmp_1 != 0;
            final List<GpsPoint> _tmpPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __activityConverters.toPoints(_tmp_2);
            _item = new Activity(_tmpId,_tmpTitle,_tmpType,_tmpDate,_tmpDistKm,_tmpDurationMin,_tmpElevGainM,_tmpMaxSpeedKmh,_tmpAvgPowerW,_tmpAvgHrBpm,_tmpNotes,_tmpHasTrace,_tmpPoints);
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
