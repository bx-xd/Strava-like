package io.github.bx_xd.velotrack.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.GpsPoint
import io.github.bx_xd.velotrack.model.Segment
import io.github.bx_xd.velotrack.utils.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEditorScreen(
    activityId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { VeloDatabase.getInstance(context) }
    var activity by remember { mutableStateOf<Activity?>(null) }

    LaunchedEffect(activityId) {
        activity = db.activityDao().getById(activityId)
    }

    val a = activity
    if (a == null || a.points.size < 2) {
        Box(Modifier.fillMaxSize().background(BgDark))
        return
    }

    val points = a.points
    val scope = rememberCoroutineScope()

    val startIdxState = remember { mutableStateOf<Int?>(null) }
    val endIdxState   = remember { mutableStateOf<Int?>(null) }
    var segmentName   by remember { mutableStateOf("") }

    val segPolyRef     = remember { mutableStateOf<Polyline?>(null) }
    val startMarkerRef = remember { mutableStateOf<Marker?>(null) }
    val endMarkerRef   = remember { mutableStateOf<Marker?>(null) }

    val startIdx = startIdxState.value
    val endIdx   = endIdxState.value

    val stats: Triple<Double, Int, Int>? = remember(startIdx, endIdx) {
        if (startIdx != null && endIdx != null && endIdx > startIdx + 1) {
            val slice = points.subList(startIdx, endIdx + 1)
            Triple(
                totalDistanceKm(slice),
                computeElevGain(slice),
                ((slice.last().ts - slice.first().ts) / 1000).toInt()
            )
        } else null
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Nouveau segment", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = AccentOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Map ───────────────────────────────────────────────
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(0.62f),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = "VeloTrack/1.0"
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        overlayManager.tilesOverlay.setColorFilter(
                            android.graphics.ColorMatrixColorFilter(
                                floatArrayOf(
                                    0.8f, 0f, 0f, 0f, 0f,
                                    0f, 0.8f, 0f, 0f, 0f,
                                    0f, 0f, 0.8f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )

                        val allGeo = points.map { GeoPoint(it.lat, it.lng) }

                        // Full trace — grey
                        overlays.add(Polyline().apply {
                            setPoints(allGeo)
                            outlinePaint.color = AndroidColor.parseColor("#888888")
                            outlinePaint.strokeWidth = 6f
                            outlinePaint.isAntiAlias = true
                        })

                        // Selected segment — orange, initially empty
                        val segPoly = Polyline().apply {
                            outlinePaint.color = AndroidColor.parseColor("#FF4D00")
                            outlinePaint.strokeWidth = 11f
                            outlinePaint.isAntiAlias = true
                        }
                        overlays.add(segPoly)
                        segPolyRef.value = segPoly

                        // Start marker — green circle
                        val sm = Marker(this).apply {
                            icon = circleDrawable(AndroidColor.parseColor("#00E676"), 22, ctx)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            isEnabled = false
                        }
                        overlays.add(sm)
                        startMarkerRef.value = sm

                        // End marker — red circle
                        val em = Marker(this).apply {
                            icon = circleDrawable(AndroidColor.parseColor("#FF3B30"), 22, ctx)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            isEnabled = false
                        }
                        overlays.add(em)
                        endMarkerRef.value = em

                        // Tap handler — index 0 so it processes taps first
                        overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                val idx = nearestPointIndex(p.latitude, p.longitude, points)
                                val si  = startIdxState.value
                                val ei  = endIdxState.value
                                when {
                                    si == null -> startIdxState.value = idx
                                    ei == null -> when {
                                        idx > si -> endIdxState.value = idx
                                        idx < si -> { endIdxState.value = si; startIdxState.value = idx }
                                    }
                                    else -> { startIdxState.value = idx; endIdxState.value = null }
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint) = false
                        }))

                        val bbox = BoundingBox.fromGeoPoints(allGeo)
                        post { zoomToBoundingBox(bbox, true, 60) }
                    }
                },
                update = { map ->
                    val segPoly = segPolyRef.value     ?: return@AndroidView
                    val sm      = startMarkerRef.value ?: return@AndroidView
                    val em      = endMarkerRef.value   ?: return@AndroidView

                    if (startIdx != null && endIdx != null) {
                        segPoly.setPoints(
                            points.subList(startIdx, endIdx + 1).map { GeoPoint(it.lat, it.lng) }
                        )
                    } else {
                        segPoly.setPoints(emptyList())
                    }

                    sm.isEnabled = startIdx != null
                    if (startIdx != null)
                        sm.position = GeoPoint(points[startIdx].lat, points[startIdx].lng)

                    em.isEnabled = endIdx != null
                    if (endIdx != null)
                        em.position = GeoPoint(points[endIdx].lat, points[endIdx].lng)

                    map.invalidate()
                }
            )

            // ── Bottom panel ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    startIdx == null -> {
                        SegEditorStep(1, "Touchez le tracé pour placer le début du segment")
                    }
                    endIdx == null -> {
                        SegEditorStep(2, "Touchez pour placer la fin du segment")
                        TextButton(onClick = { startIdxState.value = null }) {
                            Text("↩ Recommencer", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                    stats != null -> {
                        val (distKm, elevGainM, durationSecs) = stats
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            SegMiniStat("${"%.2f".format(distKm)}", "km")
                            SegMiniStat("$elevGainM", "m D+")
                            SegMiniStat(formatDurationSecs(durationSecs), "durée")
                        }
                        OutlinedTextField(
                            value         = segmentName,
                            onValueChange = { segmentName = it },
                            label         = { Text("Nom du segment", color = TextMuted) },
                            placeholder   = { Text("Ex : Montée du col", color = TextMuted) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AccentOrange,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick  = { startIdxState.value = null; endIdxState.value = null; segmentName = "" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("↩ Recommencer", color = TextMuted)
                            }
                            VeloButton(
                                text     = "💾 Sauvegarder",
                                modifier = Modifier.weight(2f),
                                onClick  = {
                                    scope.launch {
                                        db.segmentDao().insert(
                                            Segment(
                                                id           = UUID.randomUUID().toString(),
                                                name         = segmentName.ifBlank { "Segment" },
                                                activityId   = a.id,
                                                startIndex   = startIdx,
                                                endIndex     = endIdx,
                                                startLat     = points[startIdx].lat,
                                                startLng     = points[startIdx].lng,
                                                endLat       = points[endIdx].lat,
                                                endLng       = points[endIdx].lng,
                                                distKm       = distKm,
                                                elevGainM    = elevGainM,
                                                durationSecs = durationSecs,
                                                createdAt    = System.currentTimeMillis()
                                            )
                                        )
                                        onSaved()
                                    }
                                }
                            )
                        }
                    }
                    else -> {
                        // startIdx + endIdx set but points too close
                        SegEditorStep(2, "Sélectionnez un point plus éloigné")
                        TextButton(onClick = { startIdxState.value = null; endIdxState.value = null }) {
                            Text("↩ Recommencer", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegEditorStep(step: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = AccentOrange) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Text("$step", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Text(text, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun SegMiniStat(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(unit.uppercase(), color = TextMuted, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}

private fun nearestPointIndex(tapLat: Double, tapLng: Double, points: List<GpsPoint>): Int {
    var minDist = Double.MAX_VALUE
    var minIdx  = 0
    points.forEachIndexed { i, p ->
        val d = (p.lat - tapLat) * (p.lat - tapLat) + (p.lng - tapLng) * (p.lng - tapLng)
        if (d < minDist) { minDist = d; minIdx = i }
    }
    return minIdx
}

private fun circleDrawable(color: Int, sizeDp: Int, context: Context): Drawable {
    val px = (sizeDp * context.resources.displayMetrics.density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(px, px)
    }
}
