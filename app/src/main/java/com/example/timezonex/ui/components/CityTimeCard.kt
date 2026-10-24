package com.example.timezonex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timezonex.domain.model.CityTimeState
import com.example.timezonex.domain.model.ThemePreference
import com.example.timezonex.domain.model.UserSettings
import com.example.timezonex.ui.theme.BusinessClosedMuted
import com.example.timezonex.ui.theme.BusinessOpenGreen
import com.example.timezonex.ui.theme.DayCardDark
import com.example.timezonex.ui.theme.DayCardLight
import com.example.timezonex.ui.theme.MoonAccent
import com.example.timezonex.ui.theme.NightCardDark
import com.example.timezonex.ui.theme.NightCardLight
import com.example.timezonex.ui.theme.SunAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CityTimeCard(
    state: CityTimeState,
    positionNumber: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    settings: UserSettings,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCopyInfo: () -> Unit,
    onClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val isDark = when (settings.theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val cardBackground = when {
        !settings.showDayNight -> MaterialTheme.colorScheme.surface
        state.isDay && !isDark -> DayCardLight
        !state.isDay && !isDark -> NightCardLight
        state.isDay && isDark -> DayCardDark
        else -> NightCardDark
    }

    val a11yDescription = buildString {
        append("${state.city.cityName}, ${state.city.countryName}. ")
        append("Current time ${state.formattedTime}. ")
        append("${state.formattedDate}. ")
        if (settings.showDifference) {
            append("${state.formattedDifference}. ")
        }
        if (settings.showUtcOffset) {
            append("${state.formattedUtcOffset}. ")
        }
        if (settings.showDayNight) {
            append(if (state.isDay) "Daytime. " else "Nighttime. ")
        }
        if (settings.showDstStatus) {
            append(if (state.isDst) "Daylight Saving Time active." else "Standard Time.")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = a11yDescription
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Flag + City Name + Country + Reorder/Remove controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (settings.showPositionNumbers) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = positionNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = state.city.flagEmoji,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = state.city.cityName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.city.countryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Controls: Drag Handle, Move Up/Down, Copy, Remove
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = dragHandleModifier
                            .size(32.dp)
                            .semantics {
                                contentDescription = "Drag to reorder ${state.city.cityName}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (canMoveUp) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier
                                .size(28.dp)
                                .semantics {
                                    contentDescription = "Move ${state.city.cityName} up"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move ${state.city.cityName} up",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (canMoveDown) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier
                                .size(28.dp)
                                .semantics {
                                    contentDescription = "Move ${state.city.cityName} down"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move ${state.city.cityName} down",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopyInfo,
                        modifier = Modifier
                            .size(28.dp)
                            .semantics {
                                contentDescription = "Copy time for ${state.city.cityName}"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy time for ${state.city.cityName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(28.dp)
                            .semantics {
                                contentDescription = "Remove ${state.city.cityName}."
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove ${state.city.cityName}.",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent Time Display + Date + Day crossing badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = state.formattedTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.formattedDayDifference != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = state.formattedDayDifference,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (settings.showDifference) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.formattedDifference,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Badges Row: UTC offset, Day/Night, DST status, IANA ZoneId, Business Hours
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (settings.showUtcOffset) {
                    MetadataBadge(
                        text = state.formattedUtcOffset,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (settings.showDayNight) {
                    val label = if (state.isDay) "☀ Day" else "☾ Night"
                    val accent = if (state.isDay) SunAccent else MoonAccent
                    MetadataBadge(
                        text = label,
                        containerColor = accent.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (settings.showDstStatus) {
                    val dstText = if (state.isDst) "DST Active" else "Standard Time"
                    MetadataBadge(
                        text = dstText,
                        containerColor = if (state.isDst) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (settings.showTimeZoneId) {
                    MetadataBadge(
                        text = state.city.zoneId,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (settings.showBusinessHours) {
                    val statusText = if (state.isWithinBusinessHours) {
                        "BUSINESS HOURS ● Open"
                    } else {
                        "BUSINESS HOURS ○ Outside hours"
                    }
                    val dotColor = if (state.isWithinBusinessHours) BusinessOpenGreen else BusinessClosedMuted
                    MetadataBadge(
                        text = statusText,
                        containerColor = dotColor.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
