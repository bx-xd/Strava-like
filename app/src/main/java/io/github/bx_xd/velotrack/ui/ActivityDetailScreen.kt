package io.github.bx_xd.velotrack.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import io.github.bx_xd.velotrack.ui.dashboard.formatDate
import io.github.bx_xd.velotrack.ui.dashboard.typeColor
import io.github.bx_xd.velotrack.utils.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activity: Activity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onNewSegment: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { VeloDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val segments by db.segmentDao().getByActivityFlow(activity.id).collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = BgCard,
            title = { Text("Supprimer ?", color = TextPrimary) },
            text  = { Text("Cette sortie sera définitivement supprimée.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Supprimer", color = RedStop)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = TextMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(activity.type.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            activity.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = AccentOrange)
                    }
                },
                actions = {
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text("🗑️", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Map with trace
            if (activity.hasTrace && activity.points.isNotEmpty()) {
                ActivityMapView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    points = activity.points
                )
                Spacer(Modifier.height(12.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date
                Text(
                    formatDate(activity.date),
                    fontSize = 13.sp,
                    color = TextMuted
                )

                // Stats grid
                val speedAvg = if (activity.distKm > 0 && activity.durationMin > 0)
                    activity.distKm / (activity.durationMin / 60) else 0.0
                val vam = if (activity.elevGainM > 0 && activity.durationMin > 0)
                    ((activity.elevGainM.toDouble() / activity.durationMin) * 60).toInt() else 0
                val wpkg = if (activity.avgPowerW != null)
                    activity.avgPowerW.toDouble() else null

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activity.distKm > 0)
                        StatCard("${"%.1f".format(activity.distKm)}", "km", "Distance", Modifier.weight(1f))
                    StatCard(formatDuration(activity.durationMin), "", "Durée", Modifier.weight(1f))
                }
                if (speedAvg > 0 || activity.maxSpeedKmh > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (speedAvg > 0)
                            StatCard("${"%.1f".format(speedAvg)}", "km/h", "Vitesse moy", Modifier.weight(1f))
                        if (activity.maxSpeedKmh > 0)
                            StatCard("${"%.1f".format(activity.maxSpeedKmh)}", "km/h", "Vitesse max", Modifier.weight(1f))
                    }
                }
                if (activity.elevGainM > 0 || vam > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (activity.elevGainM > 0)
                            StatCard("${activity.elevGainM}", "m", "Dénivelé", Modifier.weight(1f))
                        if (vam > 0)
                            StatCard("$vam", "m/h", "VAM", Modifier.weight(1f))
                    }
                }
                if (activity.avgPowerW != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("${activity.avgPowerW}", "W", "Puissance est.", Modifier.weight(1f))
                        // W/kg needs profile — skip for simplicity
                        Spacer(Modifier.weight(1f))
                    }
                }
                if (activity.avgHrBpm != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("${activity.avgHrBpm}", "bpm", "FC moyenne", Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                    }
                }

                // Notes
                if (!activity.notes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BgCard2
                    ) {
                        Text(
                            activity.notes,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                }

                // Segments section
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Segments")
                    IconButton(onClick = onNewSegment) {
                        Icon(Icons.Default.Add, contentDescription = "Nouveau segment", tint = AccentOrange)
                    }
                }
                if (segments.isEmpty()) {
                    Text(
                        "Aucun segment — touchez + pour en créer un.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                } else {
                    segments.forEach { seg ->
                        SegmentRow(
                            segment  = seg,
                            onDelete = { scope.launch { db.segmentDao().delete(seg) } }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SegmentRow(segment: Segment, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BgCard2,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(segment.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    "${"%.2f".format(segment.distKm)} km · ${segment.elevGainM} m D+ · ${formatDurationSecs(segment.durationSecs)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedStop, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ActivityMapView(modifier: Modifier, points: List<GpsPoint>) {
    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            Configuration.getInstance().userAgentValue = "VeloTrack/1.0"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                isClickable = false
                overlayManager.tilesOverlay.setColorFilter(
                    android.graphics.ColorMatrixColorFilter(
                        floatArrayOf(
                            0.8f,0f,0f,0f,0f, 0f,0.8f,0f,0f,0f,
                            0f,0f,0.8f,0f,0f, 0f,0f,0f,1f,0f
                        )
                    )
                )
                val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
                val poly = Polyline().apply {
                    setPoints(geoPoints)
                    outlinePaint.color = AndroidColor.parseColor("#FF4D00")
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.isAntiAlias = true
                }
                overlays.add(poly)

                // Start marker (green)
                overlays.add(Marker(this).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ctx.getDrawable(android.R.drawable.ic_menu_mylocation)
                })

                // Fit to trace
                val bbox = BoundingBox.fromGeoPoints(geoPoints)
                post { zoomToBoundingBox(bbox, true, 40) }
            }
        }
    )
}
