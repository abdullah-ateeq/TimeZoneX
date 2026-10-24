package com.example.timezonex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.timezonex.domain.calculator.TimeZoneCalculator
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.TimeZoneUiState
import com.example.timezonex.ui.components.CityDetailsBottomSheet
import com.example.timezonex.ui.components.CityTimeCard
import com.example.timezonex.ui.components.OverlapBottomSheet
import com.example.timezonex.ui.components.TimeComparisonTimeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: TimeZoneUiState,
    calculator: TimeZoneCalculator,
    onTick: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToCities: () -> Unit,
    onRemoveCity: (String) -> Unit,
    onMoveCityUp: (String) -> Unit,
    onMoveCityDown: (String) -> Unit,
    onReorderCity: (Int, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onFindOverlap: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isResumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }

    var showOverlapSheet by remember { mutableStateOf(false) }
    var inspectedCity by remember { mutableStateOf<CityTimeZone?>(null) }

    // Drag-and-drop state for city reordering
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Lifecycle-aware live clock ticker: stops when app is backgrounded or screen is not visible
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isResumed) {
        while (isResumed) {
            onTick()
            delay(1000L)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: TimeZoneX + Refresh action
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TimeZoneX",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Compare time around the world.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onRefresh()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Refresh world clocks now"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh world clocks now",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // YOUR TIME / Reference Card
            item(key = "your_time_reference") {
                YourLocalTimeHeroCard(uiState = uiState)
            }

            // Compact Horizontal 24-Hour Comparison Timeline
            if (uiState.selectedCities.isNotEmpty()) {
                item(key = "visual_timeline") {
                    TimeComparisonTimeline(
                        localCityName = uiState.localCityName,
                        localDateTime = uiState.localDateTime,
                        selectedCities = uiState.selectedCities,
                        workingStart = uiState.settings.businessStartTime,
                        workingEnd = uiState.settings.businessEndTime
                    )
                }
            }

            // Section Title + Action Buttons
            item(key = "selected_cities_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECTED CITIES (${uiState.selectedCities.size}/${uiState.settings.maxCities})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Hold ≡ to drag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (uiState.selectedCities.isEmpty()) {
                item(key = "empty_cities_state") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Cities Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add global cities to compare live local times, UTC offsets, and working hours.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.selectedCities,
                    key = { _, item -> item.city.id }
                ) { index, cityState ->
                    val isBeingDragged = index == draggedIndex

                    CityTimeCard(
                        state = cityState,
                        positionNumber = index + 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.selectedCities.lastIndex,
                        settings = uiState.settings,
                        onRemove = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onRemoveCity(cityState.city.id)
                        },
                        onMoveUp = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onMoveCityUp(cityState.city.id)
                        },
                        onMoveDown = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onMoveCityDown(cityState.city.id)
                        },
                        onCopyInfo = {
                            val clipText = "${cityState.city.cityName} (${cityState.city.countryName}): " +
                                "${cityState.formattedTime}, ${cityState.formattedDate} (${cityState.formattedUtcOffset}, ${cityState.formattedDifference})"
                            copyToClipboard(context, clipText)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied ${cityState.city.cityName} time to clipboard")
                            }
                        },
                        onClick = {
                            inspectedCity = cityState.city
                        },
                        dragHandleModifier = Modifier.pointerInput(uiState.selectedCities.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                    if (uiState.settings.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val thresholdPx = 150f
                                    val currentIdx = draggedIndex
                                    if (currentIdx != -1) {
                                        if (dragOffsetY > thresholdPx && currentIdx < uiState.selectedCities.lastIndex) {
                                            onReorderCity(currentIdx, currentIdx + 1)
                                            draggedIndex = currentIdx + 1
                                            dragOffsetY = 0f
                                            if (uiState.settings.hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else if (dragOffsetY < -thresholdPx && currentIdx > 0) {
                                            onReorderCity(currentIdx, currentIdx - 1)
                                            draggedIndex = currentIdx - 1
                                            dragOffsetY = 0f
                                            if (uiState.settings.hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(durationMillis = 250),
                                fadeOutSpec = tween(durationMillis = 220),
                                placementSpec = tween(durationMillis = 260)
                            )
                            .zIndex(if (isBeingDragged) 2f else 0f)
                            .graphicsLayer {
                                translationY = if (isBeingDragged) dragOffsetY else 0f
                                scaleX = if (isBeingDragged) 1.02f else 1f
                                scaleY = if (isBeingDragged) 1.02f else 1f
                            }
                    )
                }
            }

            // Bottom Action Buttons: [ + ADD CITY ] and [ FIND OVERLAP ]
            item(key = "home_actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onNavigateToCities()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics {
                                contentDescription = "Add a city."
                            },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ ADD CITY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (uiState.settings.hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onFindOverlap()
                            showOverlapSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .semantics {
                                contentDescription = "Find working hour overlap across selected cities"
                            },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FIND OVERLAP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showOverlapSheet) {
        OverlapBottomSheet(
            overlapResult = uiState.overlapResult,
            workingHoursStartHour = uiState.settings.businessStartHour,
            workingHoursStartMinute = uiState.settings.businessStartMinute,
            workingHoursEndHour = uiState.settings.businessEndHour,
            workingHoursEndMinute = uiState.settings.businessEndMinute,
            onDismiss = { showOverlapSheet = false }
        )
    }

    val currentInspected = inspectedCity
    if (currentInspected != null) {
        val isSelected = uiState.selectedCities.any { it.city.id == currentInspected.id }
        val isFav = uiState.favoriteCityIds.contains(currentInspected.id)
        CityDetailsBottomSheet(
            city = currentInspected,
            currentInstant = uiState.currentInstant,
            referenceDateTime = uiState.localDateTime,
            isSelected = isSelected,
            isFavorite = isFav,
            use24HourFormat = uiState.is24HourResolved,
            showSeconds = uiState.settings.showSeconds,
            calculator = calculator,
            onAddCity = {},
            onRemoveCity = { onRemoveCity(currentInspected.id) },
            onToggleFavorite = { onToggleFavorite(currentInspected.id) },
            onDismiss = { inspectedCity = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YourLocalTimeHeroCard(
    uiState: TimeZoneUiState
) {
    val a11ySummary = buildString {
        append("Your local time in ${uiState.localCityName}. ")
        append("${uiState.localFormattedTime}. ")
        append("${uiState.localFormattedDayOfWeek}, ${uiState.localFormattedDate}. ")
        append("${uiState.localFormattedUtcOffset}.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11ySummary
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR TIME",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Reference Zone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = uiState.localCityName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.localFormattedTime,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${uiState.localFormattedDayOfWeek} • ${uiState.localFormattedDate}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = uiState.localFormattedUtcOffset,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (uiState.settings.showDayNight) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (uiState.localIsDay) "☀ Day" else "☾ Night",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (uiState.settings.showDstStatus) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (uiState.localIsDst) "DST Active" else "Standard Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (uiState.settings.showTimeZoneId) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = uiState.localZoneId,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("TimeZoneX City Time", text)
    clipboard?.setPrimaryClip(clip)
}
