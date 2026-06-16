package io.github.bx_xd.velotrack.utils

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
    private val client = OkHttpClient()

    suspend fun fetch(lat: Double, lng: Double): WindData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${"%.4f".format(lat)}&longitude=${"%.4f".format(lng)}" +
                "&current=wind_speed_10m,wind_direction_10m,temperature_2m,apparent_temperature," +
                "relative_humidity_2m,precipitation,weather_code" +
                "&wind_speed_unit=ms&forecast_days=1"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: return@withContext null)
            val c = json.getJSONObject("current")
            WindData(
                speedMs            = c.getDouble("wind_speed_10m"),
                directionDeg       = c.getDouble("wind_direction_10m"),
                temperatureCelsius = if (c.has("temperature_2m")) c.getDouble("temperature_2m") else null,
                feelsLikeCelsius   = if (c.has("apparent_temperature")) c.getDouble("apparent_temperature") else null,
                weatherCode        = if (c.has("weather_code")) c.getInt("weather_code") else null,
                humidityPct        = if (c.has("relative_humidity_2m")) c.getInt("relative_humidity_2m") else null,
                precipitationMm    = if (c.has("precipitation")) c.getDouble("precipitation") else null
            )
        } catch (_: Exception) { null }
    }
}
