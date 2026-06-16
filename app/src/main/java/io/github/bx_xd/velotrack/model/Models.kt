package io.github.bx_xd.velotrack.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// ── Bike activity types ───────────────────────────────────────────
enum class BikeType(val label: String, val emoji: String) {
    ROUTE("Route", "🚴"),
    GRAVEL("Gravel", "🚵"),
    VTT("VTT", "⛰️"),
    URBAIN("Urbain", "🏙️"),
    AUTRE("Autre", "⚡")
}

// ── Single GPS point ──────────────────────────────────────────────
data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val altRaw: Double?,   // altitude brute baro (pour computeElevGain post-hoc)
    val speed: Double,     // m/s (natif FusedLocation)
    val accuracy: Float,
    val ts: Long           // System.currentTimeMillis()
)

// ── Saved activity ────────────────────────────────────────────────
@Entity(tableName = "activities")
@TypeConverters(ActivityConverters::class)
data class Activity(
    @PrimaryKey val id: String,
    val title: String,
    val type: BikeType,
    val date: Long,                    // epoch ms
    val distKm: Double,
    val durationMin: Double,
    val elevGainM: Int,
    val maxSpeedKmh: Double,
    val avgPowerW: Int?,
    val avgHrBpm: Int?,
    val notes: String?,
    val hasTrace: Boolean,
    val points: List<GpsPoint>         // stored as JSON via TypeConverter
)

// ── Segment (sub-section of an activity defined by the user) ─────
@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey val id: String,
    val name: String,
    val activityId: String,
    val startIndex: Int,
    val endIndex: Int,
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val distKm: Double,
    val elevGainM: Int,
    val durationSecs: Int,
    val createdAt: Long
)

// ── SegmentEffort — one detected passage through a segment ────────
@Entity(tableName = "segment_efforts")
data class SegmentEffort(
    @PrimaryKey val id: String,          // "${segmentId}_${activityId}"
    val segmentId: String,
    val segmentName: String,
    val activityId: String,
    val startIndex: Int,
    val endIndex: Int,
    val durationSecs: Int,
    val distKm: Double,
    val avgSpeedKmh: Double,
    val date: Long
)

// ── Room TypeConverters ───────────────────────────────────────────
class ActivityConverters {
    @TypeConverter
    fun fromBikeType(type: BikeType): String = type.name

    @TypeConverter
    fun toBikeType(name: String): BikeType = BikeType.valueOf(name)

    @TypeConverter
    fun fromPoints(points: List<GpsPoint>): String {
        // Manual JSON serialization — no Kotlin serialization dependency needed
        val sb = StringBuilder("[")
        points.forEachIndexed { i, p ->
            if (i > 0) sb.append(",")
            sb.append("""{"lat":${p.lat},"lng":${p.lng},"alt":${p.altRaw ?: "null"},"speed":${p.speed},"acc":${p.accuracy},"ts":${p.ts}}""")
        }
        sb.append("]")
        return sb.toString()
    }

    @TypeConverter
    fun toPoints(json: String): List<GpsPoint> {
        if (json == "[]") return emptyList()
        val result = mutableListOf<GpsPoint>()
        // Simple JSON array parser
        val content = json.trim().removeSurrounding("[", "]")
        if (content.isEmpty()) return emptyList()
        // Split on },{ boundary
        var depth = 0
        var start = 0
        val objects = mutableListOf<String>()
        for (i in content.indices) {
            when (content[i]) {
                '{' -> { if (depth == 0) start = i; depth++ }
                '}' -> { depth--; if (depth == 0) objects.add(content.substring(start, i + 1)) }
            }
        }
        for (obj in objects) {
            try {
                val lat    = extractDouble(obj, "lat") ?: continue
                val lng    = extractDouble(obj, "lng") ?: continue
                val alt    = extractDouble(obj, "alt")
                val speed  = extractDouble(obj, "speed") ?: 0.0
                val acc    = extractDouble(obj, "acc")?.toFloat() ?: 0f
                val ts     = extractLong(obj, "ts") ?: System.currentTimeMillis()
                result.add(GpsPoint(lat, lng, alt, speed, acc, ts))
            } catch (_: Exception) {}
        }
        return result
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = """"$key":\s*([0-9.\-Ee]+|null)""".toRegex()
        val match = pattern.find(json)?.groupValues?.get(1) ?: return null
        return if (match == "null") null else match.toDoubleOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = """"$key":\s*([0-9]+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }
}
