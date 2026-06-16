package io.github.bx_xd.velotrack.ui.record

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.bx_xd.velotrack.model.BikeType
import io.github.bx_xd.velotrack.model.GpsPoint
import io.github.bx_xd.velotrack.ui.*
import io.github.bx_xd.velotrack.utils.formatDurationSecs
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(viewModel: RecordViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    // Show save dialog when recording is stopped
    if (saveState != null) {
        SaveActivityDialog(
            saveState  = saveState!!,
            onSave     = { title, type, notes, hr ->
                viewModel.apply {
                    kotlinx.coroutines.MainScope().launch {
                        saveActivity(title, type, notes, hr)
                    }
                }
            },
            onDiscard = { viewModel.discardRecording() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── Map (60% of screen) ───────────────────────────────────
        OsmMapView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f),
            points = state.points
        )

        // ── HUD ───────────────────────────────────────────────────
        RecordHud(
            state     = state,
            modifier  = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            onStart   = { viewModel.startRecording() },
            onPause   = { viewModel.pauseRecording() },
            onResume  = { viewModel.resumeRecording() },
            onStop    = { viewModel.stopRecording() }
        )
    }
}

// ── OSM Map ───────────────────────────────────────────────────────
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    points: List<GpsPoint>
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var polyline by remember { mutableStateOf<Polyline?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            Configuration.getInstance().userAgentValue = "VeloTrack/1.0"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(48.1, -1.7))
                // Dark tile filter
                overlayManager.tilesOverlay.setColorFilter(
                    android.graphics.ColorMatrixColorFilter(
                        floatArrayOf(
                            0.8f, 0f,   0f,   0f, 0f,
                            0f,   0.8f, 0f,   0f, 0f,
                            0f,   0f,   0.8f, 0f, 0f,
                            0f,   0f,   0f,   1f, 0f
                        )
                    )
                )
                mapView = this
                // Init polyline
                polyline = Polyline().apply {
                    outlinePaint.color = AndroidColor.parseColor("#FF4D00")
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.isAntiAlias = true
                    overlays.add(this)
                }
            }
        },
        update = { map ->
            if (points.isEmpty()) return@AndroidView
            val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
            polyline?.setPoints(geoPoints)

            val last = points.last()
            val lastGeo = GeoPoint(last.lat, last.lng)

            if (marker == null) {
                marker = Marker(map).apply {
                    position = lastGeo
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    map.overlays.add(this)
                }
            } else {
                marker!!.position = lastGeo
            }

            map.controller.animateTo(lastGeo)
            map.invalidate()
        }
    )
}

// ── HUD ───────────────────────────────────────────────────────────
@Composable
fun RecordHud(
    state: RecordUiState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = modifier
            .background(BgDark)
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // GPS status + auto-pause badge + wind
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GpsDot(state.gpsAvailable, state.accuracyM)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (state.gpsAvailable) "±${state.accuracyM.toInt()}m" else "En attente GPS…",
                fontSize = 12.sp,
                color = TextMuted
            )
            if (state.state == RecState.AUTO_PAUSED) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = YellowPause.copy(alpha = 0.15f)
                ) {
                    Text(
                        "⏸ Pause auto",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = YellowPause,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            state.windData?.let { wind ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BlueWind.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (wind.temperatureCelsius != null) {
                            Text(
                                text = "${wind.weatherEmoji} ${"%.0f".format(wind.temperatureCelsius)}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text("·", fontSize = 13.sp, color = TextMuted)
                        }
                        Text(
                            text = "💨 ${"%.0f".format(wind.speedKmh)} km/h ${wind.directionArrow}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueWind
                        )
                    }
                }
            }
        }

        // Stats grid (2 rows × 4 cols)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HudStat("${"%.2f".format(state.distKm)}", "km",   Modifier.weight(1f))
                HudStat(formatDurationSecs(state.elapsedSecs), "durée", Modifier.weight(1f))
                HudStat("${"%.1f".format(state.speedKmh)}", "km/h", Modifier.weight(1f))
                HudStat(
                    if (state.powerW > 0) "${state.powerW}" else "—",
                    "watts",
                    Modifier.weight(1f),
                    valueColor = if (state.powerW > 0) YellowPause else TextMuted
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HudStat("${state.elevGainM.toInt()}", "dénivelé m", Modifier.weight(1f))
                HudStat(
                    if (state.state == RecState.RECORDING && state.elapsedSecs > 60)
                        "${((state.elevGainM / (state.elapsedSecs / 60.0)) * 60).toInt()}"
                    else "—",
                    "vam m/h", Modifier.weight(1f)
                )
                HudStat("${"%.1f".format(state.maxSpeedKmh)}", "max km/h", Modifier.weight(1f))
                HudStat("${"%.1f".format(state.currentGrade * 100)}%", "pente", Modifier.weight(1f))
            }
        }

        // Elevation profile chart — live, draws as GPS points come in
        ElevationProfileChart(
            points   = state.points,
            modifier = Modifier.fillMaxWidth().height(62.dp)
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state.state) {
                RecState.IDLE -> {
                    VeloButton(
                        text     = "▶  Démarrer",
                        onClick  = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        color    = GreenGps,
                        contentColor = Color.Black
                    )
                }
                RecState.RECORDING -> {
                    VeloButton(
                        text    = "⏸  Pause",
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        color   = YellowPause,
                        contentColor = Color.Black
                    )
                    StopButton(onClick = onStop)
                }
                RecState.PAUSED, RecState.AUTO_PAUSED -> {
                    VeloButton(
                        text    = "▶  Reprendre",
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        color   = GreenGps,
                        contentColor = Color.Black
                    )
                    StopButton(onClick = onStop)
                }
            }
        }
    }
}

@Composable
fun StopButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = RedStop),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Default.Stop, contentDescription = "Terminer", tint = Color.White)
    }
}

// ── Save dialog ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveActivityDialog(
    saveState: SaveState,
    onSave: (String, BikeType, String, Int?) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BikeType.ROUTE) }
    var hrText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        containerColor = BgCard,
        title = {
            Text(
                "Enregistrer la sortie",
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Summary stats
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BgCard2
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            SummaryItem("Distance", "${"%.2f".format(saveState.distKm)} km")
                            SummaryItem("Durée", formatDurationSecs((saveState.durationMin * 60).toInt()))
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            SummaryItem("Vit. moy", "${"%.1f".format(saveState.distKm / (saveState.durationMin / 60))} km/h")
                            SummaryItem("Vit. max", "${"%.1f".format(saveState.maxSpeedKmh)} km/h")
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            SummaryItem("Dénivelé", "${saveState.elevGainM} m")
                            SummaryItem("Puissance", if (saveState.avgPowerW != null) "${saveState.avgPowerW} W" else "—")
                        }
                    }
                }

                // Bike type selector
                Text("Type", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BikeType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick  = { selectedType = type },
                            label    = { Text("${type.emoji} ${type.label}", fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentOrange.copy(alpha = 0.2f),
                                selectedLabelColor     = AccentOrange
                            )
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre", color = TextMuted) },
                    placeholder = { Text(selectedType.label, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentOrange,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary
                    )
                )

                // HR
                OutlinedTextField(
                    value = hrText,
                    onValueChange = { hrText = it.filter { c -> c.isDigit() } },
                    label = { Text("FC moyenne (bpm)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentOrange,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary
                    )
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = TextMuted) },
                    placeholder = { Text("Sensation, météo, vélo…", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentOrange,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            VeloButton(
                text    = "💾  Sauvegarder",
                onClick = { onSave(title, selectedType, notes, hrText.toIntOrNull()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("🗑️ Supprimer", color = RedStop)
            }
        }
    )
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label.uppercase(), fontSize = 10.sp, color = TextMuted, letterSpacing = 0.5.sp)
    }
}

// ── Live elevation profile ────────────────────────────────────────
@Composable
private fun ElevationProfileChart(points: List<GpsPoint>, modifier: Modifier = Modifier) {
    val ptsWithAlt = remember(points.size) { points.filter { it.altRaw != null } }

    if (ptsWithAlt.size < 5) {
        // Reserve space with a subtle placeholder
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            if (points.isNotEmpty()) {
                Text("Profil d'altitude…", fontSize = 10.sp, color = TextMuted)
            }
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(false)
                isDoubleTapToZoomEnabled = false
                axisRight.isEnabled = false
                setViewPortOffsets(40f, 4f, 4f, 18f)
                setBackgroundColor(AndroidColor.TRANSPARENT)

                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#6B6B85")
                    textSize = 8f
                    gridColor = AndroidColor.parseColor("#1A1A2A")
                    axisLineColor = AndroidColor.TRANSPARENT
                    setLabelCount(3, true)
                }
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#6B6B85")
                    textSize = 8f
                    gridColor = AndroidColor.TRANSPARENT
                    axisLineColor = AndroidColor.TRANSPARENT
                    setDrawGridLines(false)
                    setLabelCount(4, false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "%.1f".format(value)
                    }
                }
            }
        },
        update = { chart ->
            if (ptsWithAlt.size < 5) { chart.clear(); return@AndroidView }

            // Subsample to max 150 entries for performance
            val step = maxOf(1, ptsWithAlt.size / 150)
            val sampled = ptsWithAlt.filterIndexed { i, _ ->
                i % step == 0 || i == ptsWithAlt.size - 1
            }

            // Build entries with cumulative distance as X axis
            var cumDistKm = 0.0
            val entries = ArrayList<Entry>(sampled.size)
            for (i in sampled.indices) {
                if (i > 0) {
                    cumDistKm += io.github.bx_xd.velotrack.utils.haversine(
                        sampled[i - 1].lat, sampled[i - 1].lng,
                        sampled[i].lat,     sampled[i].lng
                    )
                }
                entries.add(Entry(cumDistKm.toFloat(), sampled[i].altRaw!!.toFloat()))
            }

            val accent = AndroidColor.parseColor("#FF4D00")
            val ds = LineDataSet(entries, "").apply {
                color     = accent
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = accent
                fillAlpha = 55
                mode           = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.1f
            }
            chart.data = LineData(ds)
            chart.invalidate()
        }
    )
}

