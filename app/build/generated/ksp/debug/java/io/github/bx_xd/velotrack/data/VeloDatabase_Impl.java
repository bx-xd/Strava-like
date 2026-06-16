package io.github.bx_xd.velotrack.data;

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
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VeloDatabase_Impl extends VeloDatabase {
  private volatile ActivityDao _activityDao;

  private volatile ProfileDao _profileDao;

  private volatile SegmentDao _segmentDao;

  private volatile SegmentEffortDao _segmentEffortDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `activities` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `date` INTEGER NOT NULL, `distKm` REAL NOT NULL, `durationMin` REAL NOT NULL, `elevGainM` INTEGER NOT NULL, `maxSpeedKmh` REAL NOT NULL, `avgPowerW` INTEGER, `avgHrBpm` INTEGER, `notes` TEXT, `hasTrace` INTEGER NOT NULL, `points` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `weightKg` REAL NOT NULL, `bikeWeightKg` REAL NOT NULL, `cda` REAL NOT NULL, `crr` REAL NOT NULL, `efficiency` REAL NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `segments` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `activityId` TEXT NOT NULL, `startIndex` INTEGER NOT NULL, `endIndex` INTEGER NOT NULL, `startLat` REAL NOT NULL, `startLng` REAL NOT NULL, `endLat` REAL NOT NULL, `endLng` REAL NOT NULL, `distKm` REAL NOT NULL, `elevGainM` INTEGER NOT NULL, `durationSecs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `segment_efforts` (`id` TEXT NOT NULL, `segmentId` TEXT NOT NULL, `segmentName` TEXT NOT NULL, `activityId` TEXT NOT NULL, `startIndex` INTEGER NOT NULL, `endIndex` INTEGER NOT NULL, `durationSecs` INTEGER NOT NULL, `distKm` REAL NOT NULL, `avgSpeedKmh` REAL NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3060b98a71578c137f31d2e861c08f89')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `activities`");
        db.execSQL("DROP TABLE IF EXISTS `profile`");
        db.execSQL("DROP TABLE IF EXISTS `segments`");
        db.execSQL("DROP TABLE IF EXISTS `segment_efforts`");
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
        final HashMap<String, TableInfo.Column> _columnsActivities = new HashMap<String, TableInfo.Column>(13);
        _columnsActivities.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("distKm", new TableInfo.Column("distKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("durationMin", new TableInfo.Column("durationMin", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("elevGainM", new TableInfo.Column("elevGainM", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("maxSpeedKmh", new TableInfo.Column("maxSpeedKmh", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("avgPowerW", new TableInfo.Column("avgPowerW", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("avgHrBpm", new TableInfo.Column("avgHrBpm", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("hasTrace", new TableInfo.Column("hasTrace", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActivities.put("points", new TableInfo.Column("points", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysActivities = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesActivities = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoActivities = new TableInfo("activities", _columnsActivities, _foreignKeysActivities, _indicesActivities);
        final TableInfo _existingActivities = TableInfo.read(db, "activities");
        if (!_infoActivities.equals(_existingActivities)) {
          return new RoomOpenHelper.ValidationResult(false, "activities(io.github.bx_xd.velotrack.model.Activity).\n"
                  + " Expected:\n" + _infoActivities + "\n"
                  + " Found:\n" + _existingActivities);
        }
        final HashMap<String, TableInfo.Column> _columnsProfile = new HashMap<String, TableInfo.Column>(7);
        _columnsProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("bikeWeightKg", new TableInfo.Column("bikeWeightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("cda", new TableInfo.Column("cda", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("crr", new TableInfo.Column("crr", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfile.put("efficiency", new TableInfo.Column("efficiency", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProfile = new TableInfo("profile", _columnsProfile, _foreignKeysProfile, _indicesProfile);
        final TableInfo _existingProfile = TableInfo.read(db, "profile");
        if (!_infoProfile.equals(_existingProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "profile(io.github.bx_xd.velotrack.model.UserProfile).\n"
                  + " Expected:\n" + _infoProfile + "\n"
                  + " Found:\n" + _existingProfile);
        }
        final HashMap<String, TableInfo.Column> _columnsSegments = new HashMap<String, TableInfo.Column>(13);
        _columnsSegments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("activityId", new TableInfo.Column("activityId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("startIndex", new TableInfo.Column("startIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("endIndex", new TableInfo.Column("endIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("startLat", new TableInfo.Column("startLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("startLng", new TableInfo.Column("startLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("endLat", new TableInfo.Column("endLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("endLng", new TableInfo.Column("endLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("distKm", new TableInfo.Column("distKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("elevGainM", new TableInfo.Column("elevGainM", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("durationSecs", new TableInfo.Column("durationSecs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSegments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSegments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSegments = new TableInfo("segments", _columnsSegments, _foreignKeysSegments, _indicesSegments);
        final TableInfo _existingSegments = TableInfo.read(db, "segments");
        if (!_infoSegments.equals(_existingSegments)) {
          return new RoomOpenHelper.ValidationResult(false, "segments(io.github.bx_xd.velotrack.model.Segment).\n"
                  + " Expected:\n" + _infoSegments + "\n"
                  + " Found:\n" + _existingSegments);
        }
        final HashMap<String, TableInfo.Column> _columnsSegmentEfforts = new HashMap<String, TableInfo.Column>(10);
        _columnsSegmentEfforts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("segmentId", new TableInfo.Column("segmentId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("segmentName", new TableInfo.Column("segmentName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("activityId", new TableInfo.Column("activityId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("startIndex", new TableInfo.Column("startIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("endIndex", new TableInfo.Column("endIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("durationSecs", new TableInfo.Column("durationSecs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("distKm", new TableInfo.Column("distKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("avgSpeedKmh", new TableInfo.Column("avgSpeedKmh", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSegmentEfforts.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSegmentEfforts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSegmentEfforts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSegmentEfforts = new TableInfo("segment_efforts", _columnsSegmentEfforts, _foreignKeysSegmentEfforts, _indicesSegmentEfforts);
        final TableInfo _existingSegmentEfforts = TableInfo.read(db, "segment_efforts");
        if (!_infoSegmentEfforts.equals(_existingSegmentEfforts)) {
          return new RoomOpenHelper.ValidationResult(false, "segment_efforts(io.github.bx_xd.velotrack.model.SegmentEffort).\n"
                  + " Expected:\n" + _infoSegmentEfforts + "\n"
                  + " Found:\n" + _existingSegmentEfforts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3060b98a71578c137f31d2e861c08f89", "355f009840a9253a349d77fafec5bcc2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "activities","profile","segments","segment_efforts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `activities`");
      _db.execSQL("DELETE FROM `profile`");
      _db.execSQL("DELETE FROM `segments`");
      _db.execSQL("DELETE FROM `segment_efforts`");
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
    _typeConvertersMap.put(ActivityDao.class, ActivityDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProfileDao.class, ProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SegmentDao.class, SegmentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SegmentEffortDao.class, SegmentEffortDao_Impl.getRequiredConverters());
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
  public ActivityDao activityDao() {
    if (_activityDao != null) {
      return _activityDao;
    } else {
      synchronized(this) {
        if(_activityDao == null) {
          _activityDao = new ActivityDao_Impl(this);
        }
        return _activityDao;
      }
    }
  }

  @Override
  public ProfileDao profileDao() {
    if (_profileDao != null) {
      return _profileDao;
    } else {
      synchronized(this) {
        if(_profileDao == null) {
          _profileDao = new ProfileDao_Impl(this);
        }
        return _profileDao;
      }
    }
  }

  @Override
  public SegmentDao segmentDao() {
    if (_segmentDao != null) {
      return _segmentDao;
    } else {
      synchronized(this) {
        if(_segmentDao == null) {
          _segmentDao = new SegmentDao_Impl(this);
        }
        return _segmentDao;
      }
    }
  }

  @Override
  public SegmentEffortDao segmentEffortDao() {
    if (_segmentEffortDao != null) {
      return _segmentEffortDao;
    } else {
      synchronized(this) {
        if(_segmentEffortDao == null) {
          _segmentEffortDao = new SegmentEffortDao_Impl(this);
        }
        return _segmentEffortDao;
      }
    }
  }
}
