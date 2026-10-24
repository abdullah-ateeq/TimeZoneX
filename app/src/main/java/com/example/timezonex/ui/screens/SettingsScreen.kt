package com.example.timezonex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timezonex.domain.model.ThemePreference
import com.example.timezonex.domain.model.TimeFormatPreference
import com.example.timezonex.domain.model.UserSettings
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: UserSettings,
    favoritesCount: Int,
    onSetTheme: (ThemePreference) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetTimeFormat: (TimeFormatPreference) -> Unit,
    onSetShowSeconds: (Boolean) -> Unit,
    onSetShowTimeZoneId: (Boolean) -> Unit,
    onSetShowUtcOffset: (Boolean) -> Unit,
    onSetShowDstStatus: (Boolean) -> Unit,
    onSetMaxCities: (Int) -> Unit,
    onSetShowDifference: (Boolean) -> Unit,
    onSetShowDayNight: (Boolean) -> Unit,
    onSetShowPositionNumbers: (Boolean) -> Unit,
    onSetShowBusinessHours: (Boolean) -> Unit,
    onSetBusinessHours: (Int, Int, Int, Int) -> Unit,
    onSetRestoreLastSelection: (Boolean) -> Unit,
    onClearFavorites: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun triggerHaptic() {
        if (settings.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "settings_title") {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize display, time formatting, and comparison preferences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. APPEARANCE
            item(key = "section_appearance") {
                SettingsGroupCard(title = "APPEARANCE") {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = settings.theme == pref,
                                onClick = {
                                    triggerHaptic()
                                    onSetTheme(pref)
                                },
                                label = { Text(pref.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Dynamic Color",
                        subtitle = "Use Material You wallpaper colors on Android 12+",
                        checked = settings.dynamicColor,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetDynamicColor(it)
                        }
                    )
                }
            }

            // 2. TIME
            item(key = "section_time") {
                SettingsGroupCard(title = "TIME") {
                    Text(
                        text = "Time Format",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeFormatPreference.entries.forEach { formatPref ->
                            FilterChip(
                                selected = settings.timeFormat == formatPref,
                                onClick = {
                                    triggerHaptic()
                                    onSetTimeFormat(formatPref)
                                },
                                label = { Text(formatPref.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show Seconds",
                        subtitle = "Display live seconds on local and city clocks",
                        checked = settings.showSeconds,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowSeconds(it)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show Time Zone ID",
                        subtitle = "Display IANA identifier (e.g., Europe/London)",
                        checked = settings.showTimeZoneId,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowTimeZoneId(it)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show UTC Offset",
                        subtitle = "Display current offset (e.g., UTC +05:00)",
                        checked = settings.showUtcOffset,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowUtcOffset(it)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show DST Status",
                        subtitle = "Show whether Daylight Saving Time or Standard Time is active",
                        checked = settings.showDstStatus,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowDstStatus(it)
                        }
                    )
                }
            }

            // 3. HOME
            item(key = "section_home") {
                SettingsGroupCard(title = "HOME") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Maximum Cities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Limit active comparison cards (up to 12)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${settings.maxCities}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = settings.maxCities.toFloat(),
                        onValueChange = { value ->
                            val newMax = value.roundToInt().coerceIn(1, 12)
                            if (newMax != settings.maxCities) {
                                triggerHaptic()
                                onSetMaxCities(newMax)
                            }
                        },
                        valueRange = 1f..12f,
                        steps = 10
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show Difference",
                        subtitle = "Display hours/minutes ahead or behind your local time",
                        checked = settings.showDifference,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowDifference(it)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show Day/Night",
                        subtitle = "Display ☀ Day (06:00–17:59) and ☾ Night (18:00–05:59) indicators",
                        checked = settings.showDayNight,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowDayNight(it)
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Show Position Numbers",
                        subtitle = "Display numbered order badges on each city card",
                        checked = settings.showPositionNumbers,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowPositionNumbers(it)
                        }
                    )
                }
            }

            // 4. BUSINESS HOURS
            item(key = "section_business_hours") {
                SettingsGroupCard(title = "BUSINESS HOURS") {
                    SettingsSwitchRow(
                        title = "Show Business Hours",
                        subtitle = "Generic reference badge (● Open / ○ Outside hours) on cards",
                        checked = settings.showBusinessHours,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetShowBusinessHours(it)
                        }
                    )

                    SettingsDivider()

                    val startFormatted = String.format(
                        Locale.US,
                        "%02d:%02d",
                        settings.businessStartHour,
                        settings.businessStartMinute
                    )
                    val endFormatted = String.format(
                        Locale.US,
                        "%02d:%02d",
                        settings.businessEndHour,
                        settings.businessEndMinute
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start Hour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = startFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.businessStartHour.toFloat(),
                        onValueChange = { v ->
                            val h = v.roundToInt().coerceIn(0, 23)
                            if (h != settings.businessStartHour) {
                                triggerHaptic()
                                onSetBusinessHours(
                                    h,
                                    settings.businessStartMinute,
                                    settings.businessEndHour,
                                    settings.businessEndMinute
                                )
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "End Hour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = endFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.businessEndHour.toFloat(),
                        onValueChange = { v ->
                            val h = v.roundToInt().coerceIn(0, 23)
                            if (h != settings.businessEndHour) {
                                triggerHaptic()
                                onSetBusinessHours(
                                    settings.businessStartHour,
                                    settings.businessStartMinute,
                                    h,
                                    settings.businessEndMinute
                                )
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Text(
                        text = "Note: Generic reference window used for 'Find Overlap' and optional card indicators.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // 5. DATA
            item(key = "section_data") {
                SettingsGroupCard(title = "DATA") {
                    SettingsSwitchRow(
                        title = "Restore Last Selection",
                        subtitle = "Keep your selected cities and custom order across app launches",
                        checked = settings.restoreLastSelection,
                        onCheckedChange = {
                            triggerHaptic()
                            onSetRestoreLastSelection(it)
                        }
                    )

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                triggerHaptic()
                                onClearFavorites()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cleared favorite cities")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear Favorites ($favoritesCount)")
                        }

                        OutlinedButton(
                            onClick = {
                                triggerHaptic()
                                onResetSettings()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Settings restored to defaults")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset Settings")
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
}
