package io.github.bx_xd.velotrack.data

import android.content.Context
import androidx.room.*
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.ActivityConverters
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

// ── Room Database ─────────────────────────────────────────────────
@Database(
    entities = [Activity::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ActivityConverters::class)
abstract class VeloDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile private var INSTANCE: VeloDatabase? = null

        fun getInstance(context: Context): VeloDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeloDatabase::class.java,
                    "velotrack.db"
                ).build().also { INSTANCE = it }
            }
    }
}
