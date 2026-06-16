package io.github.bx_xd.velotrack.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.utils.totalDistanceKm
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val activities: List<Activity> = emptyList(),
    val weekDistKm: Double = 0.0,
    val weekDurationMin: Double = 0.0,
    val weekElevM: Int = 0,
    val weekCount: Int = 0,
    val weekAvgSpeed: Double = 0.0,
    val totalDistKm: Double = 0.0,
    val totalCount: Int = 0,
    val totalElevM: Int = 0,
    val totalDurationMin: Double = 0.0
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VeloDatabase.getInstance(app)

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            db.activityDao().getAllFlow().collect { activities ->
                val weekStart = startOfWeek()
                val week = activities.filter { it.date >= weekStart }
                val weekDist = week.sumOf { it.distKm }
                val weekDur  = week.sumOf { it.durationMin }
                _state.value = DashboardUiState(
                    activities       = activities,
                    weekDistKm       = weekDist,
                    weekDurationMin  = weekDur,
                    weekElevM        = week.sumOf { it.elevGainM },
                    weekCount        = week.size,
                    weekAvgSpeed     = if (weekDur > 0) weekDist / (weekDur / 60.0) else 0.0,
                    totalDistKm      = activities.sumOf { it.distKm },
                    totalCount       = activities.size,
                    totalElevM       = activities.sumOf { it.elevGainM },
                    totalDurationMin = activities.sumOf { it.durationMin }
                )
            }
        }
    }

    suspend fun deleteActivity(activity: Activity) {
        db.activityDao().delete(activity)
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMon = (dow + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMon)
        return cal.timeInMillis
    }
}
