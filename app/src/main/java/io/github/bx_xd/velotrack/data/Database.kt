package io.github.bx_xd.velotrack.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.ActivityConverters
import io.github.bx_xd.velotrack.model.Segment
import io.github.bx_xd.velotrack.model.UserProfile
import kotlinx.coroutines.flow.Flow

// ── Activity DAO ──────────────────────────────────────────────────
@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY date DESC")
    suspend fun getAll(): List<Activity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: String): Activity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM activities WHERE date >= :since ORDER BY date DESC")
    suspend fun getSince(since: Long): List<Activity>
}

// ── Profile DAO ───────────────────────────────────────────────────
@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): UserProfile?

    @Query("SELECT * FROM profile WHERE id = 1")
    fun getFlow(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: UserProfile)
}

// ── Segment DAO ───────────────────────────────────────────────────
@Dao
interface SegmentDao {
    @Query("SELECT * FROM segments WHERE activityId = :activityId ORDER BY createdAt DESC")
    fun getByActivityFlow(activityId: String): Flow<List<Segment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: Segment)

    @Delete
    suspend fun delete(segment: Segment)
}

// ── Migration 1 → 2 ──────────────────────────────────────────────
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `segments` (" +
            "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `activityId` TEXT NOT NULL, " +
            "`startIndex` INTEGER NOT NULL, `endIndex` INTEGER NOT NULL, " +
            "`distKm` REAL NOT NULL, `elevGainM` INTEGER NOT NULL, " +
            "`durationSecs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))"
        )
    }
}

// ── Room Database ─────────────────────────────────────────────────
@Database(
    entities = [Activity::class, UserProfile::class, Segment::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(ActivityConverters::class)
abstract class VeloDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun profileDao(): ProfileDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        @Volatile private var INSTANCE: VeloDatabase? = null

        fun getInstance(context: Context): VeloDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeloDatabase::class.java,
                    "velotrack.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
    }
}
