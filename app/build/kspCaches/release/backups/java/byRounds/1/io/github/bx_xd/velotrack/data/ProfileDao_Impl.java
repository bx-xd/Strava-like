package io.github.bx_xd.velotrack.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.github.bx_xd.velotrack.model.UserProfile;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProfileDao_Impl implements ProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProfile> __insertionAdapterOfUserProfile;

  public ProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProfile = new EntityInsertionAdapter<UserProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `profile` (`id`,`name`,`weightKg`,`bikeWeightKg`,`cda`,`crr`,`efficiency`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getWeightKg());
        statement.bindDouble(4, entity.getBikeWeightKg());
        statement.bindDouble(5, entity.getCda());
        statement.bindDouble(6, entity.getCrr());
        statement.bindDouble(7, entity.getEfficiency());
      }
    };
  }

  @Override
  public Object save(final UserProfile profile, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserProfile.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final Continuation<? super UserProfile> $completion) {
    final String _sql = "SELECT * FROM profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProfile>() {
      @Override
      @Nullable
      public UserProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfBikeWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "bikeWeightKg");
          final int _cursorIndexOfCda = CursorUtil.getColumnIndexOrThrow(_cursor, "cda");
          final int _cursorIndexOfCrr = CursorUtil.getColumnIndexOrThrow(_cursor, "crr");
          final int _cursorIndexOfEfficiency = CursorUtil.getColumnIndexOrThrow(_cursor, "efficiency");
          final UserProfile _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final double _tmpBikeWeightKg;
            _tmpBikeWeightKg = _cursor.getDouble(_cursorIndexOfBikeWeightKg);
            final double _tmpCda;
            _tmpCda = _cursor.getDouble(_cursorIndexOfCda);
            final double _tmpCrr;
            _tmpCrr = _cursor.getDouble(_cursorIndexOfCrr);
            final double _tmpEfficiency;
            _tmpEfficiency = _cursor.getDouble(_cursorIndexOfEfficiency);
            _result = new UserProfile(_tmpId,_tmpName,_tmpWeightKg,_tmpBikeWeightKg,_tmpCda,_tmpCrr,_tmpEfficiency);
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
  public Flow<UserProfile> getFlow() {
    final String _sql = "SELECT * FROM profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"profile"}, new Callable<UserProfile>() {
      @Override
      @Nullable
      public UserProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfBikeWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "bikeWeightKg");
          final int _cursorIndexOfCda = CursorUtil.getColumnIndexOrThrow(_cursor, "cda");
          final int _cursorIndexOfCrr = CursorUtil.getColumnIndexOrThrow(_cursor, "crr");
          final int _cursorIndexOfEfficiency = CursorUtil.getColumnIndexOrThrow(_cursor, "efficiency");
          final UserProfile _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final double _tmpBikeWeightKg;
            _tmpBikeWeightKg = _cursor.getDouble(_cursorIndexOfBikeWeightKg);
            final double _tmpCda;
            _tmpCda = _cursor.getDouble(_cursorIndexOfCda);
            final double _tmpCrr;
            _tmpCrr = _cursor.getDouble(_cursorIndexOfCrr);
            final double _tmpEfficiency;
            _tmpEfficiency = _cursor.getDouble(_cursorIndexOfEfficiency);
            _result = new UserProfile(_tmpId,_tmpName,_tmpWeightKg,_tmpBikeWeightKg,_tmpCda,_tmpCrr,_tmpEfficiency);
          } else {
            _result = null;
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
