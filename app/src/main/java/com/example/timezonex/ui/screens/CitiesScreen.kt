package com.example.timezonex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timezonex.domain.calculator.TimeZoneCalculator
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.TimeZoneUiState
import com.example.timezonex.ui.components.CityDetailsBottomSheet

@Composable
fun CitiesScreen(
    uiState: TimeZoneUiState,
    calculator: TimeZoneCalculator,
    onSearchQueryChange: (String) -> Unit,
    onAddCity: (String) -> Boolean,
    onRemoveCity: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inspectedCity by remember { mutableStateOf<CityTimeZone?>(null) }

    val selectedIds = remember(uiState.selectedCities) {
        uiState.selectedCities.map { it.city.id }.toSet()
    }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearError()
        }
    }

    val isSearching = uiState.searchQuery.isNotBlank()
    val selectedSectionCities = remember(uiState.selectedCities) {
        uiState.selectedCities.map { it.city }
    }
    val favoriteSectionCities = remember(uiState.allCities, uiState.favoriteCityIds) {
        uiState.allCities.filter { uiState.favoriteCityIds.contains(it.id) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cities",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Search ${uiState.allCities.size} global cities, countries, or IANA zones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Search cities"
                    },
                placeholder = { Text("Search cities...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSearching) {
                    if (uiState.searchResults.isEmpty()) {
                        item(key = "no_search_results") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
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
                                        text = "No cities found.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try searching by city name, country, or IANA zone (e.g., Europe/London).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "search_results_header") {
                            SectionHeader("RESULTS (${uiState.searchResults.size})")
                        }
                        items(
                            items = uiState.searchResults,
                            key = { "search_${it.id}" }
                        ) { city ->
                            CitySelectionRow(
                                city = city,
                                isSelected = selectedIds.contains(city.id),
                                isFavorite = uiState.favoriteCityIds.contains(city.id),
                                onSelectClick = {
                                    if (!selectedIds.contains(city.id)) {
                                        val added = onAddCity(city.id)
                                        if (added && uiState.settings.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onToggleFavorite = {
                                    if (uiState.settings.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onToggleFavorite(city.id)
                                },
                                onOpenDetails = {
                                    inspectedCity = city
                                }
                            )
                        }
                    }
                } else {
                    // Section 1: SELECTED
                    if (selectedSectionCities.isNotEmpty()) {
                        item(key = "section_selected") {
                            SectionHeader("SELECTED (${selectedSectionCities.size}/${uiState.settings.maxCities})")
                        }
                        items(
                            items = selectedSectionCities,
                            key = { "sel_${it.id}" }
                        ) { city ->
                            CitySelectionRow(
                                city = city,
                                isSelected = true,
                                isFavorite = uiState.favoriteCityIds.contains(city.id),
                                onSelectClick = {},
                                onToggleFavorite = {
                                    if (uiState.settings.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onToggleFavorite(city.id)
                                },
                                onOpenDetails = { inspectedCity = city }
                            )
                        }
                    }

                    // Section 2: FAVORITES
                    if (favoriteSectionCities.isNotEmpty()) {
                        item(key = "section_favorites") {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader("FAVORITES (${favoriteSectionCities.size})")
                        }
                        items(
                            items = favoriteSectionCities,
                            key = { "fav_${it.id}" }
                        ) { city ->
                            val isSel = selectedIds.contains(city.id)
                            CitySelectionRow(
                                city = city,
                                isSelected = isSel,
                                isFavorite = true,
                                onSelectClick = {
                                    if (!isSel) {
                                        val added = onAddCity(city.id)
                                        if (added && uiState.settings.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onToggleFavorite = {
                                    if (uiState.settings.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onToggleFavorite(city.id)
                                },
                                onOpenDetails = { inspectedCity = city }
                            )
                        }
                    }

                    // Section 3: ALL CITIES
                    item(key = "section_all") {
                        Spacer(modifier = Modifier.height(6.dp))
                        SectionHeader("ALL CITIES (${uiState.allCities.size})")
                    }
                    items(
                        items = uiState.allCities,
                        key = { "all_${it.id}" }
                    ) { city ->
                        val isSel = selectedIds.contains(city.id)
                        CitySelectionRow(
                            city = city,
                            isSelected = isSel,
                            isFavorite = uiState.favoriteCityIds.contains(city.id),
                            onSelectClick = {
                                if (!isSel) {
                                    val added = onAddCity(city.id)
                                    if (added && uiState.settings.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                            onToggleFavorite = {
                                if (uiState.settings.hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onToggleFavorite(city.id)
                            },
                            onOpenDetails = { inspectedCity = city }
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

    val currentInspected = inspectedCity
    if (currentInspected != null) {
        val isSel = selectedIds.contains(currentInspected.id)
        val isFav = uiState.favoriteCityIds.contains(currentInspected.id)
        CityDetailsBottomSheet(
            city = currentInspected,
            currentInstant = uiState.currentInstant,
            referenceDateTime = uiState.localDateTime,
            isSelected = isSel,
            isFavorite = isFav,
            use24HourFormat = uiState.is24HourResolved,
            showSeconds = uiState.settings.showSeconds,
            calculator = calculator,
            onAddCity = {
                val added = onAddCity(currentInspected.id)
                if (added && uiState.settings.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onRemoveCity = {
                if (uiState.settings.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onRemoveCity(currentInspected.id)
            },
            onToggleFavorite = {
                if (uiState.settings.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onToggleFavorite(currentInspected.id)
            },
            onDismiss = { inspectedCity = null }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CitySelectionRow(
    city: CityTimeZone,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelectClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .semantics {
                contentDescription = "${city.cityName}, ${city.countryName}, ${city.zoneId}. " +
                    if (isSelected) "Selected" else "Not selected"
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = city.flagEmoji,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = city.cityName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = city.countryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = city.zoneId,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.semantics {
                        contentDescription = if (isFavorite) {
                            "Remove ${city.cityName} from favorites"
                        } else {
                            "Mark ${city.cityName} as favorite"
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable(onClick = onSelectClick)
                            .semantics {
                                contentDescription = "Add ${city.cityName} to Home"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
