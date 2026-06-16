package io.github.bx_xd.velotrack.ui.record

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.BikeType
import io.github.bx_xd.velotrack.model.GpsPoint
import io.github.bx_xd.velotrack.model.UserProfile
import io.github.bx_xd.velotrack.service.GpsTrackingService
import io.github.bx_xd.velotrack.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// ── Recording state ───────────────────────────────────────────────
enum class RecState { IDLE, RECORDING, PAUSED, AUTO_PAUSED }

private const val AUTO_PAUSE_SPEED_MS  = 0.8   // m/s ≈ 3 km/h
private const val AUTO_PAUSE_DELAY_MS  = 5_000L // stopped for 5s → auto-pause

data class RecordUiState(
    val state: RecState = RecState.IDLE,
    val distKm: Double = 0.0,
    val elapsedSecs: Int = 0,
    val speedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevGainM: Double = 0.0,
    val currentGrade: Double = 0.0,
    val powerW: Int = 0,
    val avgPowerW: Int = 0,
    val accuracyM: Float = 0f,
    val gpsAvailable: Boolean = false,
    val windData: WindData? = null,
    val windError: String? = null,   // non-null = last fetch failed, contains error type
    val points: List<GpsPoint> = emptyList()
)

data class SaveState(
    val distKm: Double,
    val durationMin: Double,
    val elevGainM: Int,
    val maxSpeedKmh: Double,
    val avgPowerW: Int?,
    val points: List<GpsPoint>
)

class RecordViewModel(app: Application) : AndroidViewModel(app), GpsTrackingService.GpsListener {

    private val db = VeloDatabase.getInstance(app)
    private var gpsService: GpsTrackingService? = null
    private var serviceBound = false

    // ── UI State ──────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState?>(null)
    val saveState: StateFlow<SaveState?> = _saveState.asStateFlow()

    // ── Recording data ────────────────────────────────────────────
    private val points = mutableListOf<GpsPoint>()
    private var startTs = 0L
    private var pausedMs = 0L
    private var pauseStartTs = 0L
    private var elevGainM = 0.0
    private var lastAltSmoothed: Double? = null
    private var maxSpeedKmh = 0.0
    private var powerSum = 0.0
    private var powerCount = 0
    private var prevSpeedMs = 0.0
    private var prevSpeedTs = 0L
    private var smoothedSpeedMs = 0.0
    private var currentWindData: WindData? = null
    private var profile: UserProfile = UserProfile()

    private val kalman = KalmanState()
    private val altEma = AltitudeEma()
    private var lastMovingTs = 0L   // ts of last point with speed > AUTO_PAUSE_SPEED_MS

    // ── Timer ─────────────────────────────────────────────────────
    private var timerJob: Job? = null

    // ── Service connection ────────────────────────────────────────
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            gpsService = (binder as GpsTrackingService.LocalBinder).getService()
            gpsService?.setListener(this@RecordViewModel)
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            gpsService = null
            serviceBound = false
        }
    }

    init {
        bindService()
        loadProfile()
        fetchWindPeriodically()
    }

    override fun onCleared() {
        gpsService?.setListener(null)
        if (serviceBound) {
            getApplication<Application>().unbindService(connection)
            serviceBound = false
        }
        super.onCleared()
    }

    // ── GPS Listener callbacks (called from HandlerThread) ────────
    override fun onPoint(point: GpsPoint) {
        // All processing happens in Kotlin — no WebView, no JS
        processGpsPoint(point)
    }

    override fun onAccuracyChanged(accuracyM: Float, available: Boolean) {
        _uiState.value = _uiState.value.copy(
            gpsAvailable = available,
            accuracyM = accuracyM
        )
    }

    // ── Core GPS processing ───────────────────────────────────────
    private fun processGpsPoint(raw: GpsPoint) {
        val state = _uiState.value.state

        // When auto-paused: GPS still runs, only watch for movement to auto-resume
        if (state == RecState.AUTO_PAUSED) {
            if (raw.accuracy <= GPS_MAX_ACCURACY_M && raw.speed > AUTO_PAUSE_SPEED_MS) {
                triggerAutoResume()
            }
            return
        }

        if (state != RecState.RECORDING) return
        if (raw.accuracy > GPS_MAX_ACCURACY_M) return

        // Update accuracy display
        _uiState.value = _uiState.value.copy(
            accuracyM = raw.accuracy,
            gpsAvailable = true
        )

        // Kalman filter on position
        val (lat, lng) = kalman.filter(raw.lat, raw.lng, raw.accuracy)

        // Auto-pause: track last timestamp with meaningful speed
        val rawSpeedMs = if (raw.speed > 0) raw.speed else 0.0
        if (rawSpeedMs > AUTO_PAUSE_SPEED_MS) {
            lastMovingTs = raw.ts
        } else if (lastMovingTs > 0 && raw.ts - lastMovingTs > AUTO_PAUSE_DELAY_MS) {
            triggerAutoPause()
            return
        }

        // Min distance filter
        if (points.isNotEmpty()) {
            val last = points.last()
            if (haversine(last.lat, last.lng, lat, lng) * 1000 < GPS_MIN_DISTANCE_M) return
        }

        // Auto-reset Kalman + EMA if gap > 30s (resume after pause/screen-off)
        if (points.isNotEmpty()) {
            val lastTs = points.last().ts
            if (raw.ts - lastTs > 30_000L) {
                kalman.reset()
                altEma.reset()
                prevSpeedMs = 0.0
                prevSpeedTs = 0L
                smoothedSpeedMs = 0.0
            }
        }

        // Altitude
        val altSmoothed = altEma.filter(raw.altRaw)

        // Live elevation gain: reference-altitude approach (EMA diff per point is too small)
        // lastAltSmoothed acts as a baseline that only advances on significant changes
        if (altSmoothed != null) {
            val ref = lastAltSmoothed
            if (ref == null) {
                lastAltSmoothed = altSmoothed
            } else {
                val rise = altSmoothed - ref
                when {
                    rise >= ELEV_THRESHOLD_M  -> { elevGainM += rise; lastAltSmoothed = altSmoothed }
                    rise <= -ELEV_THRESHOLD_M -> { lastAltSmoothed = altSmoothed }
                }
            }
        }

        // Speed: prefer native GPS speed
        val speedMs = if (raw.speed > 0.1) raw.speed
                      else if (points.isNotEmpty()) {
                          val last = points.last()
                          val d = haversine(last.lat, last.lng, lat, lng) * 1000
                          val dt = (raw.ts - last.ts) / 1000.0
                          if (dt > 0) d / dt else 0.0
                      } else 0.0

        val speedKmh = speedMs * 3.6
        if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh

        // Grade
        val recentWithAlt = points.takeLast(30).filter { it.altRaw != null }
        val grade = computeGrade(recentWithAlt, lat, lng, altSmoothed)

        // Bearing
        val rideBearing = if (points.isNotEmpty()) {
            val last = points.last()
            bearing(last.lat, last.lng, lat, lng)
        } else 0.0

        // Acceleration — EMA-smoothed speed avoids GPS noise spikes in power calc
        val nowTs = raw.ts
        smoothedSpeedMs = if (prevSpeedTs == 0L) speedMs
                          else SPEED_EMA_ALPHA * speedMs + (1 - SPEED_EMA_ALPHA) * smoothedSpeedMs
        val accel = if (prevSpeedTs > 0 && nowTs - prevSpeedTs < 5000L) {
            ((smoothedSpeedMs - prevSpeedMs) / ((nowTs - prevSpeedTs) / 1000.0)).coerceIn(-1.5, 1.5)
        } else 0.0
        prevSpeedMs = smoothedSpeedMs
        prevSpeedTs = nowTs

        // Power
        val wind = currentWindData
        val power = if (profile.isConfigured) {
            estimatePower(
                speedMs      = speedMs,
                gradeDecimal = grade,
                accelMs2     = accel,
                riderBearingDeg = rideBearing,
                windSpeedMs  = wind?.speedMs ?: 0.0,
                windDirDeg   = wind?.directionDeg ?: 0.0,
                totalMassKg  = profile.totalMassKg,
                cda          = profile.cda,
                crr          = profile.crr,
                efficiency   = profile.efficiency
            ) ?: 0
        } else 0

        if (power > 0) { powerSum += power; powerCount++ }

        // Store point with RAW altitude (computeElevGain does its own smoothing)
        val filteredPoint = raw.copy(lat = lat, lng = lng)
        points.add(filteredPoint)

        if (points.size == 1) fetchWindNow()

        // Update UI state
        val dist = totalDistanceKm(points)
        val elapsed = ((System.currentTimeMillis() - startTs - pausedMs) / 1000).toInt()
        _uiState.value = _uiState.value.copy(
            distKm       = dist,
            elapsedSecs  = elapsed,
            speedKmh     = speedKmh,
            maxSpeedKmh  = maxSpeedKmh,
            elevGainM    = elevGainM,
            currentGrade = grade,
            powerW       = power,
            avgPowerW    = if (powerCount > 0) (powerSum / powerCount).toInt() else 0,
            points       = points.toList()
        )

        // Update notification every ~5s (reduce overhead)
        if (points.size % 5 == 0) {
            gpsService?.updateNotification(dist, elapsed, speedKmh)
        }
    }

    // ── State machine ─────────────────────────────────────────────
    fun startRecording() {
        points.clear()
        startTs = System.currentTimeMillis()
        pausedMs = 0L
        pauseStartTs = 0L
        elevGainM = 0.0
        lastAltSmoothed = null
        maxSpeedKmh = 0.0
        powerSum = 0.0
        powerCount = 0
        prevSpeedMs = 0.0
        prevSpeedTs = 0L
        smoothedSpeedMs = 0.0
        lastMovingTs = 0L
        kalman.reset()
        altEma.reset()

        _uiState.value = RecordUiState(state = RecState.RECORDING)
        startGpsService()
        startTimer()
        fetchLastKnownLocation()  // show weather before first GPS point arrives
    }

    fun pauseRecording() {
        pauseStartTs = System.currentTimeMillis()
        lastMovingTs = 0L
        _uiState.value = _uiState.value.copy(state = RecState.PAUSED)
        stopGpsService()
        timerJob?.cancel()
    }

    fun resumeRecording() {
        val wasAutoPaused = _uiState.value.state == RecState.AUTO_PAUSED
        if (pauseStartTs > 0L) {
            pausedMs += System.currentTimeMillis() - pauseStartTs
            pauseStartTs = 0L
        }
        kalman.reset()
        altEma.reset()
        prevSpeedMs = 0.0
        prevSpeedTs = 0L
        smoothedSpeedMs = 0.0
        lastMovingTs = System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(state = RecState.RECORDING)
        if (!wasAutoPaused) startGpsService()  // GPS already running during auto-pause
        startTimer()
        fetchWindNow()
    }

    private fun triggerAutoPause() {
        pauseStartTs = System.currentTimeMillis()
        lastMovingTs = 0L
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = RecState.AUTO_PAUSED)
        // GPS service intentionally kept running to detect movement
    }

    private fun triggerAutoResume() {
        if (pauseStartTs > 0L) {
            pausedMs += System.currentTimeMillis() - pauseStartTs
            pauseStartTs = 0L
        }
        kalman.reset()
        altEma.reset()
        prevSpeedMs = 0.0
        prevSpeedTs = 0L
        smoothedSpeedMs = 0.0
        lastMovingTs = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(state = RecState.RECORDING)
        startTimer()
    }

    fun stopRecording() {
        if (pauseStartTs > 0L) {
            pausedMs += System.currentTimeMillis() - pauseStartTs
            pauseStartTs = 0L
        }
        timerJob?.cancel()
        stopGpsService()  // safe even when called from AUTO_PAUSED (GPS still running)

        if (points.size < 2) {
            _uiState.value = RecordUiState()
            return
        }

        val distKm    = totalDistanceKm(points)
        val durMs     = System.currentTimeMillis() - startTs - pausedMs
        val durationMin = durMs / 60_000.0
        val elevGain  = computeElevGain(points)
        val avgPower  = if (powerCount > 0) (powerSum / powerCount).toInt() else null

        _saveState.value = SaveState(
            distKm      = distKm,
            durationMin = durationMin,
            elevGainM   = elevGain,
            maxSpeedKmh = maxSpeedKmh,
            avgPowerW   = avgPower,
            points      = points.toList()
        )
        _uiState.value = _uiState.value.copy(state = RecState.IDLE)
    }

    suspend fun saveActivity(
        title: String,
        type: BikeType,
        notes: String,
        hrBpm: Int?
    ) {
        val s = _saveState.value ?: return
        val act = Activity(
            id          = UUID.randomUUID().toString(),
            title       = title.ifBlank { type.label },
            type        = type,
            date        = System.currentTimeMillis(),
            distKm      = s.distKm,
            durationMin = s.durationMin,
            elevGainM   = s.elevGainM,
            maxSpeedKmh = s.maxSpeedKmh,
            avgPowerW   = s.avgPowerW,
            avgHrBpm    = hrBpm,
            notes       = notes.ifBlank { null },
            hasTrace    = true,
            points      = s.points
        )
        db.activityDao().insert(act)

        // Segment matching — run in background, non-blocking
        viewModelScope.launch {
            try {
                val allSegments = db.segmentDao().getAll()
                val efforts = io.github.bx_xd.velotrack.utils.SegmentMatcher.match(
                    activityId   = act.id,
                    activityDate = act.date,
                    points       = act.points,
                    segments     = allSegments
                )
                efforts.forEach { db.segmentEffortDao().insert(it) }
            } catch (_: Exception) {}
        }

        _saveState.value = null
        _uiState.value = RecordUiState()
    }

    fun discardRecording() {
        _saveState.value = null
        _uiState.value = RecordUiState()
    }

    // ── Timer ─────────────────────────────────────────────────────
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.state == RecState.RECORDING) {
                    val elapsed = ((System.currentTimeMillis() - startTs - pausedMs) / 1000).toInt()
                    _uiState.value = _uiState.value.copy(elapsedSecs = elapsed)
                }
            }
        }
    }

    // ── GPS Service control ───────────────────────────────────────
    private fun startGpsService() {
        val app = getApplication<Application>()
        val i = Intent(app, GpsTrackingService::class.java).apply { action = GpsTrackingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(i)
        else app.startService(i)
        bindService()
    }

    private fun stopGpsService() {
        getApplication<Application>().startService(
            Intent(getApplication(), GpsTrackingService::class.java)
                .apply { action = GpsTrackingService.ACTION_STOP }
        )
    }

    private fun bindService() {
        if (!serviceBound) {
            getApplication<Application>().bindService(
                Intent(getApplication(), GpsTrackingService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    // ── Wind ──────────────────────────────────────────────────────
    private fun fetchWindPeriodically() {
        viewModelScope.launch {
            while (true) {
                // Retry every 30s until first success, then every 5 min
                val interval = if (currentWindData == null) 30_000L else 300_000L
                delay(interval)
                if (_uiState.value.state == RecState.RECORDING || _uiState.value.state == RecState.AUTO_PAUSED) {
                    fetchWindNow()
                }
            }
        }
    }

    fun fetchWindNow(lat: Double? = null, lng: Double? = null) {
        val fetchLat: Double
        val fetchLng: Double
        if (lat != null && lng != null) {
            fetchLat = lat; fetchLng = lng
        } else if (points.isNotEmpty()) {
            val last = points.last(); fetchLat = last.lat; fetchLng = last.lng
        } else {
            return
        }
        viewModelScope.launch {
            val wind = WindService.fetch(fetchLat, fetchLng)
            if (wind != null) {
                currentWindData = wind
                _uiState.value = _uiState.value.copy(windData = wind)
            }
        }
    }

    private fun fetchLastKnownLocation() {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        Log.d("VeloTrack.Wind", "lastLocation → ${loc.latitude}, ${loc.longitude}")
                        fetchWindNow(loc.latitude, loc.longitude)
                    } else {
                        Log.w("VeloTrack.Wind", "lastLocation = null (no cached position yet)")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("VeloTrack.Wind", "lastLocation failed: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.e("VeloTrack.Wind", "lastLocation permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e("VeloTrack.Wind", "lastLocation exception: ${e.message}")
        }
    }

    // ── Profile ───────────────────────────────────────────────────
    private fun loadProfile() {
        viewModelScope.launch {
            db.profileDao().get()?.let { profile = it }
        }
    }
}
