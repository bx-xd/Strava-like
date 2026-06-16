package io.github.bx_xd.velotrack.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.BikeType
import io.github.bx_xd.velotrack.ui.*
import io.github.bx_xd.velotrack.utils.formatDuration
import io.github.bx_xd.velotrack.utils.formatSpeed
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onActivityClick: (Activity) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Week hero card
        item {
            WeekHeroCard(state)
        }

        // All-time stats
        item {
            AllTimeStats(state)
        }

        // Recent activities
        item {
            SectionTitle("Récentes", Modifier.padding(top = 4.dp))
        }

        if (state.activities.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(state.activities.take(5), key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick  = { onActivityClick(activity) },
                    onDelete = {
                        kotlinx.coroutines.MainScope().launch {
                            viewModel.deleteActivity(activity)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WeekHeroCard(state: DashboardUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A0A00), Color(0xFF2A1200), Color(0xFF1A0A00))
                ),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "CETTE SEMAINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = AccentOrange
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${"%.0f".format(state.weekDistKm)}",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(" km", fontSize = 22.sp, color = TextMuted)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                WeekMetric(formatDuration(state.weekDurationMin), "Durée")
                WeekMetric(
                    if (state.weekAvgSpeed > 0) "${"%.1f".format(state.weekAvgSpeed)} km/h" else "—",
                    "Vit. moy"
                )
                WeekMetric(
                    if (state.weekElevM > 0) "${state.weekElevM}m" else "—",
                    "Dénivelé"
                )
                WeekMetric("${state.weekCount}", "Sorties")
            }
        }
    }
}

@Composable
private fun WeekMetric(value: String, label: String) {
    Column {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun AllTimeStats(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value    = "${"%.0f".format(state.totalDistKm)}",
            unit     = "km",
            label    = "Distance totale",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value    = "${state.totalCount}",
            unit     = "",
            label    = "Sorties",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value    = "${state.totalElevM}",
            unit     = "m",
            label    = "Dénivelé cumulé",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value    = if (state.totalDistKm > 0 && state.totalDurationMin > 0)
                           "${"%.1f".format(state.totalDistKm / (state.totalDurationMin / 60))}"
                       else "—",
            unit     = "km/h",
            label    = "Vitesse moy",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActivityCard(
    activity: Activity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val speedAvg = if (activity.distKm > 0 && activity.durationMin > 0)
        activity.distKm / (activity.durationMin / 60) else 0.0
    val vam = if (activity.elevGainM > 0 && activity.durationMin > 0)
        ((activity.elevGainM.toDouble() / activity.durationMin) * 60).toInt() else 0

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = BgCard,
            title = { Text("Supprimer ?", color = TextPrimary) },
            text  = { Text("Cette sortie sera définitivement supprimée.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Supprimer", color = RedStop)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = TextMuted)
                }
            }
        )
    }

    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        color     = BgCard,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape    = RoundedCornerShape(12.dp),
                color    = typeColor(activity.type).copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(activity.type.emoji, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    maxLines   = 1
                )
                Text(
                    formatDate(activity.date),
                    fontSize = 12.sp,
                    color    = TextMuted
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (activity.distKm > 0)
                        MiniStat("${"%.1f".format(activity.distKm)}", "km")
                    if (activity.durationMin > 0)
                        MiniStat(formatDuration(activity.durationMin), "")
                    if (speedAvg > 0)
                        MiniStat("${"%.1f".format(speedAvg)}", "km/h")
                    if (activity.elevGainM > 0)
                        MiniStat("↑${activity.elevGainM}", "m")
                    if (activity.avgPowerW != null)
                        MiniStat("⚡${activity.avgPowerW}", "W")
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, unit: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚴", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Aucune sortie", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Appuie sur ⏺ pour enregistrer", fontSize = 14.sp, color = TextMuted)
    }
}

fun formatDate(epoch: Long): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
    return sdf.format(Date(epoch))
}

fun typeColor(type: BikeType) = when (type) {
    BikeType.ROUTE  -> AccentOrange
    BikeType.GRAVEL -> Color(0xFFAA00FF)
    BikeType.VTT    -> Color(0xFF00E676)
    BikeType.URBAIN -> Color(0xFFFFD600)
    BikeType.AUTRE  -> Color(0xFF2979FF)
}
