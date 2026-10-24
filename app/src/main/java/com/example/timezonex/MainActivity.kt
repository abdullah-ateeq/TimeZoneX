package com.example.timezonex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timezonex.domain.calculator.TimeZoneCalculator
import com.example.timezonex.ui.navigation.Screen
import com.example.timezonex.ui.screens.AboutScreen
import com.example.timezonex.ui.screens.CitiesScreen
import com.example.timezonex.ui.screens.HomeScreen
import com.example.timezonex.ui.screens.SettingsScreen
import com.example.timezonex.ui.screens.SplashScreen
import com.example.timezonex.ui.theme.TimeZoneXTheme
import com.example.timezonex.ui.viewmodel.TimeZoneViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: TimeZoneViewModel = viewModel(
                factory = TimeZoneViewModel.provideFactory(context)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TimeZoneXTheme(
                themePreference = uiState.settings.theme,
                dynamicColor = uiState.settings.dynamicColor
            ) {
                TimeZoneXApp(viewModel = viewModel)
            }
        }
    }
}

private data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun TimeZoneXApp(
    viewModel: TimeZoneViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calculator = remember { TimeZoneCalculator() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Screen.Home, Icons.Filled.Public, Icons.Outlined.Public),
            BottomNavItem(Screen.Cities, Icons.Filled.LocationCity, Icons.Outlined.LocationCity),
            BottomNavItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings),
            BottomNavItem(Screen.About, Icons.Filled.Info, Icons.Outlined.Info)
        )
    }

    val showBottomBar = currentRoute != null && currentRoute != Screen.Splash.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.screen.title
                                )
                            },
                            label = {
                                Text(text = item.screen.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Home
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    calculator = calculator,
                    onTick = { viewModel.updateCurrentInstant() },
                    onRefresh = { viewModel.refreshNow() },
                    onNavigateToCities = {
                        navController.navigate(Screen.Cities.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRemoveCity = { cityId -> viewModel.removeCity(cityId) },
                    onMoveCityUp = { cityId -> viewModel.moveCityUp(cityId) },
                    onMoveCityDown = { cityId -> viewModel.moveCityDown(cityId) },
                    onReorderCity = { from, to -> viewModel.reorderCity(from, to) },
                    onToggleFavorite = { cityId -> viewModel.toggleFavorite(cityId) },
                    onFindOverlap = { viewModel.calculateBusinessHourOverlap() },
                    onClearError = { viewModel.clearErrorMessage() }
                )
            }

            // 3. Cities
            composable(Screen.Cities.route) {
                CitiesScreen(
                    uiState = uiState,
                    calculator = calculator,
                    onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                    onAddCity = { cityId -> viewModel.addCity(cityId) },
                    onRemoveCity = { cityId -> viewModel.removeCity(cityId) },
                    onToggleFavorite = { cityId -> viewModel.toggleFavorite(cityId) },
                    onClearError = { viewModel.clearErrorMessage() }
                )
            }

            // 4. Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = uiState.settings,
                    favoritesCount = uiState.favoriteCityIds.size,
                    onSetTheme = { viewModel.setThemePreference(it) },
                    onSetDynamicColor = { viewModel.setDynamicColor(it) },
                    onSetTimeFormat = { viewModel.setTimeFormat(it) },
                    onSetShowSeconds = { viewModel.setShowSeconds(it) },
                    onSetShowTimeZoneId = { viewModel.setShowTimeZoneId(it) },
                    onSetShowUtcOffset = { viewModel.setShowUtcOffset(it) },
                    onSetShowDstStatus = { viewModel.setShowDstStatus(it) },
                    onSetMaxCities = { viewModel.setMaxCities(it) },
                    onSetShowDifference = { viewModel.setShowDifference(it) },
                    onSetShowDayNight = { viewModel.setShowDayNight(it) },
                    onSetShowPositionNumbers = { viewModel.setShowPositionNumbers(it) },
                    onSetShowBusinessHours = { viewModel.setShowBusinessHours(it) },
                    onSetBusinessHours = { sh, sm, eh, em ->
                        viewModel.setBusinessHours(sh, sm, eh, em)
                    },
                    onSetRestoreLastSelection = { viewModel.setRestoreLastSelection(it) },
                    onClearFavorites = { viewModel.clearFavorites() },
                    onResetSettings = { viewModel.resetSettings() }
                )
            }

            // 5. About
            composable(Screen.About.route) {
                AboutScreen(
                    totalCitiesCount = uiState.allCities.size,
                    localZoneId = uiState.localZoneId
                )
            }
        }
    }
}
