package io.github.bx_xd.velotrack.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class WindData(
    val speedMs: Double = 0.0,
    val directionDeg: Double = 0.0,
    val temperatureCelsius: Double? = null,
    val feelsLikeCelsius: Double? = null,
    val weatherCode: Int? = null,
    val humidityPct: Int? = null,
    val precipitationMm: Double? = null
) {
    val speedKmh get() = speedMs * 3.6
    val directionArrow get(): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return dirs[((directionDeg / 45).toInt()) % 8]
    }
    val weatherEmoji get(): String = when (weatherCode) {
        0                -> "☀️"
        1, 2             -> "🌤️"
        3                -> "☁️"
        45, 48           -> "🌫️"
        51, 53, 55       -> "🌦️"
        61, 63, 65       -> "🌧️"
        71, 73, 75, 77   -> "❄️"
        80, 81, 82       -> "🌦️"
        85, 86           -> "🌨️"
        95, 96, 99       -> "⛈️"
        else             -> if (weatherCode != null) "🌡️" else ""
    }
}

object WindService {
    private const val TAG = "VeloTrack.Wind"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun fetch(lat: Double, lng: Double): WindData? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${String.format(java.util.Locale.US, "%.4f", lat)}&longitude=${String.format(java.util.Locale.US, "%.4f", lng)}" +
            "&current=wind_speed_10m,wind_direction_10m,temperature_2m,apparent_temperature," +
            "relative_humidity_2m,precipitation,weather_code" +
            "&wind_speed_unit=ms&forecast_days=1"
        Log.d(TAG, "fetch → $url")
        try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            Log.d(TAG, "HTTP ${resp.code}")
            if (!resp.isSuccessful) {
                Log.w(TAG, "Non-2xx response: ${resp.code}")
                return@withContext null
            }
            val body = resp.body?.string() ?: run {
                Log.w(TAG, "Empty body")
                return@withContext null
            }
            val c = JSONObject(body).getJSONObject("current")
            val result = WindData(
                speedMs            = c.getDouble("wind_speed_10m"),
                directionDeg       = c.getDouble("wind_direction_10m"),
                temperatureCelsius = if (c.has("temperature_2m")) c.getDouble("temperature_2m") else null,
                feelsLikeCelsius   = if (c.has("apparent_temperature")) c.getDouble("apparent_temperature") else null,
                weatherCode        = if (c.has("weather_code")) c.getInt("weather_code") else null,
                humidityPct        = if (c.has("relative_humidity_2m")) c.getInt("relative_humidity_2m") else null,
                precipitationMm    = if (c.has("precipitation")) c.getDouble("precipitation") else null
            )
            Log.d(TAG, "OK → ${result.temperatureCelsius}°C, ${result.speedKmh} km/h")
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetch failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}
