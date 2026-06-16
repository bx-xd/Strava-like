package io.github.bx_xd.velotrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Segment
import io.github.bx_xd.velotrack.model.SegmentEffort
import io.github.bx_xd.velotrack.utils.formatDurationSecs
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentDetailScreen(
    segmentId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { VeloDatabase.getInstance(context) }
    var segment by remember { mutableStateOf<Segment?>(null) }
    val efforts by db.segmentEffortDao().getBySegmentFlow(segmentId).collectAsState(initial = emptyList())

    LaunchedEffect(segmentId) {
        segment = db.segmentDao().getById(segmentId)
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        segment?.name ?: "Segment",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = AccentOrange)
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
                .padding(horizontal = 16.dp)
        ) {
            val seg = segment
            if (seg != null) {
                // Segment info card
                Surface(shape = RoundedCornerShape(12.dp), color = BgCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SegDetailStat("${"%.2f".format(seg.distKm)}", "km")
                        SegDetailStat("${seg.elevGainM}", "m D+")
                        SegDetailStat(formatDurationSecs(seg.durationSecs), "ref")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Best time hero
            val best = efforts.minByOrNull { it.durationSecs }
            if (best != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentOrange.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Meilleur temps", fontSize = 12.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatDurationSecs(best.durationSecs),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "${"%.1f".format(best.avgSpeedKmh)} km/h moy",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // All efforts
            SectionTitle("Passages (${efforts.size})")
            Spacer(Modifier.height(8.dp))

            if (efforts.isEmpty()) {
                Text(
                    "Aucun passage détecté — enregistrez une sortie qui passe par ce segment.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            } else {
                efforts.forEachIndexed { index, effort ->
                    EffortDetailRow(rank = index + 1, effort = effort, isBest = effort.id == best?.id)
                    if (index < efforts.size - 1) Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SegDetailStat(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(unit.uppercase(), fontSize = 11.sp, color = TextMuted, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun EffortDetailRow(rank: Int, effort: SegmentEffort, isBest: Boolean) {
    val dateStr = remember(effort.date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(effort.date))
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isBest) AccentOrange.copy(alpha = 0.08f) else BgCard2,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBest) AccentOrange else TextMuted,
                modifier = Modifier.width(28.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateStr, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "${"%.2f".format(effort.distKm)} km · ${"%.1f".format(effort.avgSpeedKmh)} km/h",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Text(
                formatDurationSecs(effort.durationSecs),
                fontSize = 16.sp,
                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                color = if (isBest) AccentOrange else TextPrimary
            )
        }
    }
}
