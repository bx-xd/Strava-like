package io.github.bx_xd.velotrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import io.github.bx_xd.velotrack.MainActivity
import io.github.bx_xd.velotrack.model.GpsPoint
import io.github.bx_xd.velotrack.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GpsTrackingService — 100% Kotlin natif, zéro WebView.
 *
 * Les points GPS sont stockés directement dans pointBuffer (mémoire native).
 * Aucun thread UI, aucune WebView, aucun evaluateJavascript.
 * RecordViewModel observe les StateFlows pour mettre à jour l'UI Compose.
 *
 * Garanties écran éteint :
 *  - ForegroundService (Android ne tue pas)
 *  - PARTIAL_WAKE_LOCK (CPU actif)
 *  - FusedLocationProvider (GPS natif)
 *  - stopWithTask=false (survit au swipe)
 */
class GpsTrackingService : Service() {

    // ── Callback interface → ViewModel ────────────────────────────
    interface GpsListener {
        fun onPoint(point: GpsPoint)
        fun onAccuracyChanged(accuracyM: Float, available: Boolean)
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@GpsTrackingService
    }

    companion object {
        const val ACTION_START = "io.github.bx_xd.velotrack.START"
        const val ACTION_STOP  = "io.github.bx_xd.velotrack.STOP"
        const val CHANNEL_ID   = "velotrack_gps"
        const val NOTIF_ID     = 42
        const val WAKELOCK_MS  = 6L * 3600 * 1000  // 6h max
        const val INTERVAL_MS  = 1000L
        const val FASTEST_MS   = 500L
        const val MIN_DIST_M   = 2f
        const val BUFFER_MAX   = 10800  // 3h @ 1Hz
    }

    // ── State ─────────────────────────────────────────────────────
    private val binder           = LocalBinder()
    private var listener: GpsListener? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var gpsThread: android.os.HandlerThread? = null

    // Buffer: stores points when no listener attached
    // Replayed immediately when listener reconnects (ViewModel binds)
    private val pointBuffer = ArrayDeque<GpsPoint>(BUFFER_MAX)

    // ── Lifecycle ─────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> { stopTracking(); START_NOT_STICKY }
            else        -> { startTracking(); START_STICKY }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { stopLocationUpdates(); releaseWakeLock(); super.onDestroy() }

    // ── Listener management ───────────────────────────────────────
    fun setListener(l: GpsListener?) {
        listener = l
        if (l != null && pointBuffer.isNotEmpty()) {
            // Replay all buffered points to the newly attached ViewModel
            val toReplay = pointBuffer.toList()
            pointBuffer.clear()
            toReplay.forEach { l.onPoint(it) }
        }
    }

    // ── Start / Stop ──────────────────────────────────────────────
    private fun startTracking() {
        startForeground(NOTIF_ID, buildNotification())
        acquireWakeLock()
        startLocationUpdates()
    }

    private fun stopTracking() {
        stopLocationUpdates()
        releaseWakeLock()
        pointBuffer.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
        stopSelf()
    }

    // ── FusedLocationProvider ─────────────────────────────────────
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_MS)
            .setMinUpdateDistanceMeters(MIN_DIST_M)
            .setWaitForAccurateLocation(false)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val point = GpsPoint(
                    lat      = loc.latitude,
                    lng      = loc.longitude,
                    altRaw   = if (loc.hasAltitude()) loc.altitude else null,
                    speed    = if (loc.hasSpeed()) loc.speed.toDouble() else 0.0,
                    accuracy = loc.accuracy,
                    ts       = loc.time
                )
                // Direct delivery if listener active, otherwise buffer
                val l = listener
                if (l != null) {
                    l.onPoint(point)
                } else {
                    if (pointBuffer.size < BUFFER_MAX) pointBuffer.addLast(point)
                }
            }

            override fun onLocationAvailability(avail: LocationAvailability) {
                listener?.onAccuracyChanged(0f, avail.isLocationAvailable)
            }
        }

        try {
            gpsThread = android.os.HandlerThread("VeloTrackGPS").also { it.start() }
            fusedClient?.requestLocationUpdates(request, locationCallback!!, gpsThread!!.looper)
        } catch (e: SecurityException) {
            stopTracking()
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        locationCallback = null
        gpsThread?.quitSafely()
        gpsThread = null
    }

    // ── Wake lock ─────────────────────────────────────────────────
    private fun acquireWakeLock() {
        releaseWakeLock()
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VeloTrack::GPS")
            .apply { acquire(WAKELOCK_MS) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ── Notification ──────────────────────────────────────────────
    fun updateNotification(distKm: Double, durationSecs: Int, speedKmh: Double) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VeloTrack — Enregistrement")
            .setContentText("${"%.1f".format(distKm)} km · ${formatDurationSecs(durationSecs)} · ${"%.1f".format(speedKmh)} km/h")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setColor(0xFFFF4D00.toInt())
            .setColorized(true)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        )
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("VeloTrack")
        .setContentText("🚴 GPS actif — écran éteint autorisé")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setColor(0xFFFF4D00.toInt())
        .setColorized(true)
        .setContentIntent(openAppIntent())
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()

    private fun openAppIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false); enableVibration(false); enableLights(false) }
        )
    }
}
