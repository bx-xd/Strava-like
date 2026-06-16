package io.github.bx_xd.velotrack.utils

import io.github.bx_xd.velotrack.model.GpsPoint
import kotlin.math.*

// ── Constants ─────────────────────────────────────────────────────
const val GPS_MAX_ACCURACY_M  = 100f   // reject points beyond this
const val GPS_MIN_DISTANCE_M  = 3.0    // min distance between stored points
const val ALT_EMA_ALPHA       = 0.15   // altitude EMA smoothing
const val ELEV_THRESHOLD_M    = 2.0    // hysteresis for live elev gain
const val SPEED_EMA_ALPHA     = 0.3    // speed display smoothing
const val RHO                 = 1.225  // air density kg/m³
const val G                   = 9.81   // gravity m/s²

// ── Haversine distance (km) ───────────────────────────────────────
fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun totalDistanceKm(points: List<GpsPoint>): Double {
    var d = 0.0
    for (i in 1 until points.size) {
        d += haversine(points[i-1].lat, points[i-1].lng, points[i].lat, points[i].lng)
    }
    return d
}

// ── Bearing (degrees) ─────────────────────────────────────────────
fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val x = cos(Math.toRadians(lat2)) * sin(dLon)
    val y = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return (Math.toDegrees(atan2(x, y)) + 360) % 360
}

// ── Kalman filter (lat/lng position smoothing) ────────────────────
data class KalmanState(
    var lat: Double? = null,
    var lng: Double? = null,
    var vLat: Double = 0.0,
    var vLng: Double = 0.0,
    val Q: Double = 3e-10
) {
    fun reset() { lat = null; lng = null; vLat = 0.0; vLng = 0.0 }

    fun filter(rawLat: Double, rawLng: Double, accuracy: Float): Pair<Double, Double> {
        val R = (accuracy / 111320.0).pow(2)
        if (lat == null) {
            lat = rawLat; lng = rawLng; vLat = R; vLng = R
            return Pair(rawLat, rawLng)
        }
        val pvLat = vLat + Q
        val pvLng = vLng + Q
        val kLat = pvLat / (pvLat + R)
        val kLng = pvLng / (pvLng + R)
        lat = lat!! + kLat * (rawLat - lat!!)
        lng = lng!! + kLng * (rawLng - lng!!)
        vLat = (1 - kLat) * pvLat
        vLng = (1 - kLng) * pvLng
        return Pair(lat!!, lng!!)
    }
}

// ── Altitude EMA ──────────────────────────────────────────────────
class AltitudeEma {
    private var smoothed: Double? = null

    fun reset() { smoothed = null }

    fun filter(raw: Double?): Double? {
        raw ?: return null
        smoothed = if (smoothed == null) raw
                   else ALT_EMA_ALPHA * raw + (1 - ALT_EMA_ALPHA) * smoothed!!
        return smoothed
    }
}

// ── Post-hoc elevation gain (Strava-like) ─────────────────────────
// Moving average 20-point + 2m hysteresis accumulation
fun computeElevGain(points: List<GpsPoint>): Int {
    val alts = points.mapNotNull { it.altRaw }
    if (alts.size < 8) return 0
    val window = 20
    val smoothed = alts.mapIndexed { i, _ ->
        val w = alts.subList(maxOf(0, i - window / 2), minOf(alts.size, i + window / 2 + 1))
        w.average()
    }
    var gain = 0.0
    var ref = smoothed[0]
    for (i in 1 until smoothed.size) {
        val diff = smoothed[i] - ref
        when {
            diff > 2.0  -> { gain += diff; ref = smoothed[i] }
            diff < -2.0 -> ref = smoothed[i]
        }
    }
    return gain.roundToInt()
}

// ── Power estimation (physics model) ─────────────────────────────
fun estimatePower(
    speedMs: Double,
    gradeDecimal: Double,
    accelMs2: Double,
    riderBearingDeg: Double,
    windSpeedMs: Double,
    windDirDeg: Double,   // meteorological: direction wind comes FROM
    totalMassKg: Double,
    cda: Double,
    crr: Double,
    efficiency: Double
): Int? {
    if (speedMs < 0.5) return 0
    if (totalMassKg < 10) return null

    // Wind apparent component (headwind positive)
    val windVecDeg = (windDirDeg + 180) % 360
    val angleDiff = Math.toRadians(riderBearingDeg - windVecDeg)
    val windComponent = windSpeedMs * cos(angleDiff)
    val vAir = speedMs + windComponent

    val fRoll  = crr * totalMassKg * G * cos(atan(gradeDecimal))
    val fAero  = 0.5 * RHO * cda * vAir * abs(vAir)
    val fClimb = totalMassKg * G * sin(atan(gradeDecimal))
    val fAccel = totalMassKg * accelMs2

    val power = (fRoll + fAero + fClimb + fAccel) * speedMs / efficiency
    return power.roundToInt().coerceIn(0, 2000)
}

// ── Grade calculation from recent points ─────────────────────────
fun computeGrade(recentPoints: List<GpsPoint>, currentLat: Double, currentLng: Double, currentAlt: Double?): Double {
    if (recentPoints.size < 2 || currentAlt == null) return 0.0
    val ref = recentPoints.first { it.altRaw != null }
    val distM = haversine(ref.lat, ref.lng, currentLat, currentLng) * 1000
    if (distM < 2.0) return 0.0
    val altDiff = currentAlt - (ref.altRaw ?: currentAlt)
    return (altDiff / distM).coerceIn(-0.5, 0.5)
}

// ── Formatting utilities ──────────────────────────────────────────
fun formatDuration(minutes: Double): String {
    if (minutes <= 0) return "—"
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).roundToInt()
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "${m}min"
}

fun formatDurationSecs(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "$h:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
           else "${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}

fun formatSpeed(distKm: Double, durationMin: Double): String {
    if (distKm <= 0 || durationMin <= 0) return "—"
    return "%.1f".format(distKm / (durationMin / 60))
}

