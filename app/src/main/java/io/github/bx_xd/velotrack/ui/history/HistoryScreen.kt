package io.github.bx_xd.velotrack.ui.history

import android.app.Application
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.model.BikeType
import io.github.bx_xd.velotrack.ui.*
import io.github.bx_xd.velotrack.ui.dashboard.ActivityCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VeloDatabase.getInstance(app)
    private val _filter = MutableStateFlow<BikeType?>(null)
    val filter = _filter.asStateFlow()

    val activities: StateFlow<List<Activity>> = db.activityDao().getAllFlow()
        .combine(_filter) { all, filter ->
            if (filter == null) all else all.filter { it.type == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: BikeType?) { _filter.value = type }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch { db.activityDao().delete(activity) }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onActivityClick: (Activity) -> Unit
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filter == null,
                    onClick  = { viewModel.setFilter(null) },
                    label    = { Text("🗓️ Tout") },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange,
                        selectedLabelColor     = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
            items(BikeType.entries) { type ->
                FilterChip(
                    selected = filter == type,
                    onClick  = { viewModel.setFilter(type) },
                    label    = { Text("${type.emoji} ${type.label}") },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange,
                        selectedLabelColor     = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("📋", fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text("Aucune sortie", fontSize = 17.dp.value.sp, color = TextPrimary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activities, key = { it.id }) { activity ->
                    ActivityCard(
                        activity = activity,
                        onClick  = { onActivityClick(activity) },
                        onDelete = { viewModel.deleteActivity(activity) }
                    )
                }
            }
        }
    }
}
