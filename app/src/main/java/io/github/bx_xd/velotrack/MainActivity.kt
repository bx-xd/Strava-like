package io.github.bx_xd.velotrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import io.github.bx_xd.velotrack.model.Activity
import io.github.bx_xd.velotrack.ui.*
import io.github.bx_xd.velotrack.ui.SegmentEditorScreen
import io.github.bx_xd.velotrack.ui.dashboard.*
import io.github.bx_xd.velotrack.ui.history.*
import io.github.bx_xd.velotrack.ui.profile.*
import io.github.bx_xd.velotrack.ui.record.*
import io.github.bx_xd.velotrack.ui.stats.*

// ── Navigation destinations ───────────────────────────────────────
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object History   : Screen("history")
    object Record    : Screen("record")
    object Stats     : Screen("stats")
    object Profile   : Screen("profile")
    object Detail    : Screen("detail/{activityId}") {
        fun createRoute(id: String) = "detail/$id"
    }
    object SegmentEditor : Screen("segmentEditor/{activityId}") {
        fun createRoute(id: String) = "segmentEditor/$id"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Tableau",    Icons.Outlined.GridView,     Icons.Filled.GridView),
    BottomNavItem(Screen.History,   "Historique", Icons.Outlined.History,      Icons.Filled.History),
    BottomNavItem(Screen.Record,    "Sortie",     Icons.Outlined.RadioButtonUnchecked, Icons.Filled.RadioButtonChecked),
    BottomNavItem(Screen.Stats,     "Stats",      Icons.Outlined.ShowChart,    Icons.Filled.ShowChart),
    BottomNavItem(Screen.Profile,   "Profil",     Icons.Outlined.Person,       Icons.Filled.Person),
)

class MainActivity : ComponentActivity() {

    // ── Permission launcher ───────────────────────────────────────
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // After base perms granted, request background location
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            requestBackgroundLocationIfNeeded()
        }
    }

    private val bgPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled by system */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBasePermissions()
        setContent {
            VeloTrackTheme {
                VeloTrackApp()
            }
        }
    }

    private fun requestBasePermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
        else requestBackgroundLocationIfNeeded()
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeloTrackApp() {
    val navController = rememberNavController()

    // Shared ViewModels — created once, survive navigation
    val recordVm:   RecordViewModel    = viewModel()
    val dashboardVm: DashboardViewModel = viewModel()
    val historyVm:  HistoryViewModel   = viewModel()
    val statsVm:    StatsViewModel     = viewModel()
    val profileVm:  ProfileViewModel   = viewModel()

    val recordState by recordVm.uiState.collectAsState()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "VeloTrack 🚴",
                        fontSize = 20.sp,
                        color = AccentOrange,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgDark
                )
            )
        },
        bottomBar = {
            VeloBottomNav(
                navController = navController,
                isRecording   = recordState.state == RecState.RECORDING || recordState.state == RecState.PAUSED
            )
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardVm,
                    onActivityClick = { activity ->
                        navController.navigate(Screen.Detail.createRoute(activity.id))
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyVm,
                    onActivityClick = { activity ->
                        navController.navigate(Screen.Detail.createRoute(activity.id))
                    }
                )
            }
            composable(Screen.Record.route) {
                RecordScreen(viewModel = recordVm)
            }
            composable(Screen.Stats.route) {
                StatsScreen(viewModel = statsVm)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = profileVm)
            }
            composable(Screen.Detail.route) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
                val activity = dashboardVm.state.collectAsState().value.activities
                    .find { it.id == activityId }
                if (activity != null) {
                    ActivityDetailScreen(
                        activity     = activity,
                        onBack       = { navController.popBackStack() },
                        onDelete     = {
                            kotlinx.coroutines.MainScope().launch {
                                dashboardVm.deleteActivity(activity)
                            }
                            navController.popBackStack()
                        },
                        onNewSegment = {
                            navController.navigate(Screen.SegmentEditor.createRoute(activity.id))
                        }
                    )
                }
            }
            composable(Screen.SegmentEditor.route) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
                SegmentEditorScreen(
                    activityId = activityId,
                    onBack     = { navController.popBackStack() },
                    onSaved    = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun VeloBottomNav(navController: androidx.navigation.NavController, isRecording: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = BgCard,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            val isRecordBtn = item.screen == Screen.Record

            NavigationBarItem(
                icon = {
                    if (isRecordBtn) {
                        // Record button — special styling
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .size(32.dp)
                                .background(
                                    if (isRecording) RedStop else AccentOrange,
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.RadioButtonChecked,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = androidx.compose.ui.Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.iconSelected else item.icon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp
                    )
                },
                selected = selected,
                onClick  = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AccentOrange,
                    selectedTextColor   = AccentOrange,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor      = AccentOrange.copy(alpha = 0.12f)
                )
            )
        }
    }
}
