package io.github.bx_xd.velotrack.ui.stats

import android.app.Application
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.github.bx_xd.velotrack.data.VeloDatabase
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class StatPeriod(val label: String, val days: Int) {
    WEEK("7j", 7),
    MONTH("30j", 30),
    YEAR("1an", 365)
}

data class StatsUiState(
    val period: StatPeriod = StatPeriod.WEEK,
    val distLabels: List<String> = emptyList(),
    val distValues: List<Float> = emptyList(),
    val elevValues: List<Float> = emptyList(),
    val speedValues: List<Float> = emptyList()
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = VeloDatabase.getInstance(app)
    private val _period = MutableStateFlow(StatPeriod.WEEK)

    val state: StateFlow<StatsUiState> = combine(
        db.activityDao().getAllFlow(),
        _period
    ) { activities, period ->
        buildState(activities, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setPeriod(p: StatPeriod) { _period.value = p }

    private fun buildState(activities: List<Activity>, period: StatPeriod): StatsUiState {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -(period.days - 1))
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        val since = cal.timeInMillis
        val filtered = activities.filter { it.date >= since }

        return when (period) {
            StatPeriod.WEEK  -> buildDayBuckets(filtered, StatPeriod.WEEK)
            StatPeriod.MONTH -> buildDayBuckets(filtered, StatPeriod.MONTH)
            StatPeriod.YEAR  -> buildMonthBuckets(filtered)
        }
    }

    private fun buildDayBuckets(activities: List<Activity>, period: StatPeriod): StatsUiState {
        val days = period.days
        val labels = mutableListOf<String>()
        val dist = mutableListOf<Float>()
        val elev = mutableListOf<Float>()
        val speed = mutableListOf<Float>()
        val dayNames = listOf("Dim","Lun","Mar","Mer","Jeu","Ven","Sam")
        val sdf = SimpleDateFormat("dd/MM", Locale.FRENCH)

        for (i in days - 1 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = cal.timeInMillis

            val dayActs = activities.filter { it.date in dayStart until dayEnd }
            val totDist = dayActs.sumOf { it.distKm }.toFloat()
            val totDur  = dayActs.sumOf { it.durationMin }.toFloat()
            dist.add(totDist)
            elev.add(dayActs.sumOf { it.elevGainM }.toFloat())
            speed.add(if (totDur > 0) (totDist / (totDur / 60f)) else 0f)
            labels.add(if (days == 7) dayNames[Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -i) }.get(Calendar.DAY_OF_WEEK) - 1]
                       else if (i % 5 == 0) sdf.format(Date(dayStart)) else "")
        }
        return StatsUiState(period, labels, dist, elev, speed)
    }

    private fun buildMonthBuckets(activities: List<Activity>): StatsUiState {
        val labels = mutableListOf<String>()
        val dist = mutableListOf<Float>()
        val elev = mutableListOf<Float>()
        val speed = mutableListOf<Float>()
        val monthNames = listOf("Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc")

        for (i in 11 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            val mStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val mEnd = cal.timeInMillis

            val mActs = activities.filter { it.date in mStart until mEnd }
            val totDist = mActs.sumOf { it.distKm }.toFloat()
            val totDur  = mActs.sumOf { it.durationMin }.toFloat()
            dist.add(totDist)
            elev.add(mActs.sumOf { it.elevGainM }.toFloat())
            speed.add(if (totDur > 0) (totDist / (totDur / 60f)) else 0f)
            labels.add(monthNames[Calendar.getInstance().apply { add(Calendar.MONTH, -i) }.get(Calendar.MONTH)])
        }
        return StatsUiState(StatPeriod.YEAR, labels, dist, elev, speed)
    }
}

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPeriod.entries.forEach { p ->
                FilterChip(
                    selected = state.period == p,
                    onClick  = { viewModel.setPeriod(p) },
                    label    = { Text(p.label) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }

        // Distance chart
        ChartCard("Distance (km)") {
            BarChartView(
                labels = state.distLabels,
                values = state.distValues,
                color  = AccentOrange.toArgb()
            )
        }

        // Elevation chart
        ChartCard("Dénivelé (m)") {
            BarChartView(
                labels = state.distLabels,
                values = state.elevValues,
                color  = AndroidColor.parseColor("#2979FF")
            )
        }

        // Speed chart
        ChartCard("Vitesse moyenne (km/h)") {
            LineChartView(
                labels = state.distLabels,
                values = state.speedValues,
                color  = AndroidColor.parseColor("#00E676")
            )
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BgCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                fontSize = 11.sp,
                color = TextMuted,
                letterSpacing = 1.dp.value.sp
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BarChartView(labels: List<String>, values: List<Float>, color: Int) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        factory  = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawBorders(false)
                axisRight.isEnabled = false
                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#6B6B85")
                    gridColor = AndroidColor.parseColor("#2A2A3A")
                    axisLineColor = AndroidColor.TRANSPARENT
                }
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#6B6B85")
                    gridColor = AndroidColor.TRANSPARENT
                    axisLineColor = AndroidColor.TRANSPARENT
                    setDrawGridLines(false)
                    granularity = 1f
                }
                setTouchEnabled(false)
            }
        },
        update   = { chart ->
            if (values.isEmpty()) return@AndroidView
            val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
            val ds = BarDataSet(entries, "").apply {
                this.color = color
                setDrawValues(false)
            }
            chart.data = BarData(ds)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.invalidate()
        }
    )
}

@Composable
fun LineChartView(labels: List<String>, values: List<Float>, color: Int) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        factory  = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                axisRight.isEnabled = false
                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#6B6B85")
                    gridColor = AndroidColor.parseColor("#2A2A3A")
                    axisLineColor = AndroidColor.TRANSPARENT
                }
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#6B6B85")
                    gridColor = AndroidColor.TRANSPARENT
                    axisLineColor = AndroidColor.TRANSPARENT
                    setDrawGridLines(false)
                    granularity = 1f
                }
                setTouchEnabled(false)
            }
        },
        update   = { chart ->
            if (values.isEmpty()) return@AndroidView
            val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val ds = LineDataSet(entries, "").apply {
                this.color = color
                setCircleColor(color)
                circleRadius = 2f
                lineWidth = 2f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = color
                fillAlpha = 40
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(ds)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.invalidate()
        }
    )
}
