package io.github.bx_xd.velotrack.ui.record

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
enum class RecState { IDLE, RECORDING, PAUSED }

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
    private var currentWindData: WindData? = null
    private var profile: UserProfile = UserProfile()

    private val kalman = KalmanState()
    private val altEma = AltitudeEma()

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
        if (state != RecState.RECORDING) return
        if (raw.accuracy > GPS_MAX_ACCURACY_M) return

        // Update accuracy display
        _uiState.value = _uiState.value.copy(
            accuracyM = raw.accuracy,
            gpsAvailable = true
        )

        // Kalman filter on position
        val (lat, lng) = kalman.filter(raw.lat, raw.lng, raw.accuracy)

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
            }
        }

        // Altitude
        val altSmoothed = altEma.filter(raw.altRaw)

        // Live elevation gain (on smoothed alt)
        if (altSmoothed != null && lastAltSmoothed != null) {
            val diff = altSmoothed - lastAltSmoothed!!
            if (diff > ELEV_THRESHOLD_M) elevGainM += diff
        }
        if (altSmoothed != null) lastAltSmoothed = altSmoothed

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
        val recentWithAlt = points.takeLast(5).filter { it.altRaw != null }
        val grade = computeGrade(recentWithAlt, lat, lng, altSmoothed)

        // Bearing
        val rideBearing = if (points.isNotEmpty()) {
            val last = points.last()
            bearing(last.lat, last.lng, lat, lng)
        } else 0.0

        // Acceleration
        val nowTs = raw.ts
        val accel = if (prevSpeedTs > 0 && nowTs - prevSpeedTs < 5000L) {
            (speedMs - prevSpeedMs) / ((nowTs - prevSpeedTs) / 1000.0)
        } else 0.0
        prevSpeedMs = speedMs
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
        kalman.reset()
        altEma.reset()

        _uiState.value = RecordUiState(state = RecState.RECORDING)
        startGpsService()
        startTimer()
    }

    fun pauseRecording() {
        pauseStartTs = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(state = RecState.PAUSED)
        stopGpsService()
        timerJob?.cancel()
    }

    fun resumeRecording() {
        if (pauseStartTs > 0L) {
            pausedMs += System.currentTimeMillis() - pauseStartTs
            pauseStartTs = 0L
        }
        // Reset per-resume state
        kalman.reset()
        altEma.reset()
        prevSpeedMs = 0.0
        prevSpeedTs = 0L

        _uiState.value = _uiState.value.copy(state = RecState.RECORDING)
        startGpsService()
        startTimer()
        fetchWindNow()
    }

    fun stopRecording() {
        if (pauseStartTs > 0L) {
            pausedMs += System.currentTimeMillis() - pauseStartTs
            pauseStartTs = 0L
        }
        timerJob?.cancel()
        stopGpsService()

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
                delay(300_000) // every 5 min
                fetchWindNow()
            }
        }
    }

    fun fetchWindNow() {
        if (points.isEmpty()) return
        val last = points.last()
        viewModelScope.launch {
            val wind = WindService.fetch(last.lat, last.lng)
            if (wind != null) {
                currentWindData = wind
                _uiState.value = _uiState.value.copy(windData = wind)
            }
        }
    }

    // ── Profile ───────────────────────────────────────────────────
    private fun loadProfile() {
        viewModelScope.launch {
            db.profileDao().get()?.let { profile = it }
        }
    }
}
