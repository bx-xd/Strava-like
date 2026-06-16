package io.github.bx_xd.velotrack.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class WindData(
    val speedMs: Double = 0.0,
    val directionDeg: Double = 0.0
) {
    val speedKmh get() = speedMs * 3.6
    val directionArrow get(): String {
        val dirs = listOf("N","NE","E","SE","S","SO","O","NO")
        return dirs[((directionDeg / 45).toInt()) % 8]
    }
}

object WindService {
    private val client = OkHttpClient()

    suspend fun fetch(lat: Double, lng: Double): WindData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${"%.4f".format(lat)}&longitude=${"%.4f".format(lng)}" +
                "&current=wind_speed_10m,wind_direction_10m&wind_speed_unit=ms&forecast_days=1"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: return@withContext null)
            val current = json.getJSONObject("current")
            WindData(
                speedMs = current.getDouble("wind_speed_10m"),
                directionDeg = current.getDouble("wind_direction_10m")
            )
        } catch (_: Exception) { null }
    }
}
