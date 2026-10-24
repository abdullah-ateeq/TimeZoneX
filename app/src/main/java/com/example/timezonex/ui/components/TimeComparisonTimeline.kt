package com.example.timezonex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timezonex.domain.model.CityTimeState
import java.time.LocalTime
import java.time.ZonedDateTime

@Composable
fun TimeComparisonTimeline(
    localCityName: String,
    localDateTime: ZonedDateTime,
    selectedCities: List<CityTimeState>,
    workingStart: LocalTime = LocalTime.of(9, 0),
    workingEnd: LocalTime = LocalTime.of(17, 0),
    modifier: Modifier = Modifier
) {
    if (selectedCities.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBackground = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Visual time zone comparison timeline across ${selectedCities.size + 1} locations."
            },
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
                text = "24-HOUR VISUAL TIMELINE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Daylight & working window relative to your local day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scale header: 12 AM   6 AM   12 PM   6 PM   12 AM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val startHourFloat = workingStart.hour + (workingStart.minute / 60f)
            val endHourFloat = workingEnd.hour + (workingEnd.minute / 60f)
            val currentLocalFraction = ((localDateTime.hour * 60) + localDateTime.minute) / 1440f

            // Reference row (Your local city)
            TimelineCityRow(
                cityName = localCityName.ifBlank { "Your Time" },
                offsetDifferenceMinutes = 0,
                startHourFloat = startHourFloat,
                endHourFloat = endHourFloat,
                currentTimeFraction = currentLocalFraction,
                barColor = primaryColor,
                trackColor = trackBackground,
                markerColor = markerColor
            )

            // Selected cities rows
            selectedCities.forEach { state ->
                Spacer(modifier = Modifier.height(8.dp))
                TimelineCityRow(
                    cityName = state.city.cityName,
                    offsetDifferenceMinutes = state.differenceMinutes,
                    startHourFloat = startHourFloat,
                    endHourFloat = endHourFloat,
                    currentTimeFraction = currentLocalFraction,
                    barColor = primaryColor.copy(alpha = 0.78f),
                    trackColor = trackBackground,
                    markerColor = markerColor
                )
            }
        }
    }
}

@Composable
private fun TimelineCityRow(
    cityName: String,
    offsetDifferenceMinutes: Int,
    startHourFloat: Float,
    endHourFloat: Float,
    currentTimeFraction: Float,
    barColor: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    markerColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cityName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
        ) {
            val width = size.width
            val height = size.height
            val cornerRadius = CornerRadius(height / 2f, height / 2f)

            // Full 24h track background
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(width, height),
                cornerRadius = cornerRadius
            )

            // Convert the city's local [startHourFloat, endHourFloat] into the reference day's timeline [0..24]
            val offsetHours = offsetDifferenceMinutes / 60f
            val durationHours = if (endHourFloat >= startHourFloat) {
                endHourFloat - startHourFloat
            } else {
                (24f - startHourFloat) + endHourFloat
            }.coerceIn(1f, 24f)

            // When city is at startHourFloat, reference local time is (startHourFloat - offsetHours) mod 24
            var refStartHour = (startHourFloat - offsetHours) % 24f
            if (refStartHour < 0f) refStartHour += 24f
            val refEndHour = refStartHour + durationHours

            if (refEndHour <= 24f) {
                val startX = (refStartHour / 24f) * width
                val segWidth = (durationHours / 24f) * width
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(startX, 0f),
                    size = Size(segWidth, height),
                    cornerRadius = cornerRadius
                )
            } else {
                // Wraps across midnight on the reference scale: draw two segments
                val firstStartX = (refStartHour / 24f) * width
                val firstWidth = width - firstStartX
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(firstStartX, 0f),
                    size = Size(firstWidth, height),
                    cornerRadius = cornerRadius
                )

                val secondHours = refEndHour - 24f
                val secondWidth = (secondHours / 24f) * width
                if (secondWidth > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(secondWidth, height),
                        cornerRadius = cornerRadius
                    )
                }
            }

            // Current instant vertical indicator line
            val markerX = (currentTimeFraction.coerceIn(0f, 1f)) * width
            drawLine(
                color = markerColor,
                start = Offset(markerX, -2.dp.toPx()),
                end = Offset(markerX, height + 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
