package com.example.timezonex.domain.calculator

import com.example.timezonex.domain.model.CityOverlapWindow
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.OverlapCalculationResult
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class BusinessHourCalculator(
    private val timeFormatter: TimeFormatter = TimeFormatter()
) {

    /**
     * Checks if a given local time falls within the configured business hours [startTime, endTime).
     * Handles both standard windows (e.g. 09:00 to 17:00) and cross-midnight windows (e.g. 22:00 to 06:00).
     */
    fun isWithinBusinessHours(
        localTime: LocalTime,
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0)
    ): Boolean {
        if (startTime == endTime) return true
        return if (startTime.isBefore(endTime)) {
            !localTime.isBefore(startTime) && localTime.isBefore(endTime)
        } else {
            // Cross-midnight window (e.g. 22:00 to 06:00)
            !localTime.isBefore(startTime) || localTime.isBefore(endTime)
        }
    }

    fun isWithinBusinessHours(
        zonedDateTime: ZonedDateTime,
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0)
    ): Boolean {
        return isWithinBusinessHours(zonedDateTime.toLocalTime(), startTime, endTime)
    }

    /**
     * Finds the overlapping working-hour window across the reference zone and all selected cities
     * over a 24-hour period starting from the reference date's midnight (00:00) in the reference zone.
     *
     * Supports:
     * - Arbitrary working hour windows (including cross-midnight)
     * - Different dates across cities
     * - Daylight Saving Time
     * - Half-hour (30m) and 45-minute (15m) time zones
     */
    fun findOverlap(
        referenceInstant: Instant,
        referenceZoneId: ZoneId,
        referenceCity: CityTimeZone,
        selectedCities: List<CityTimeZone>,
        workingStart: LocalTime = LocalTime.of(9, 0),
        workingEnd: LocalTime = LocalTime.of(17, 0),
        use24HourFormat: Boolean = false
    ): OverlapCalculationResult {
        val allCities = buildList {
            add(referenceCity)
            selectedCities.forEach { city ->
                if (none { it.id == city.id }) {
                    add(city)
                }
            }
        }

        val referenceZoned = ZonedDateTime.ofInstant(referenceInstant, referenceZoneId)
        val dayStartZoned = referenceZoned.toLocalDate().atStartOfDay(referenceZoneId)
        val dayStartInstant = dayStartZoned.toInstant()

        // Step in 15-minute increments across 24 hours (96 slots) to accurately capture 30m and 45m zones
        val slotMinutes = 15
        val totalSlots = (24 * 60) / slotMinutes
        val validSlots = BooleanArray(totalSlots)

        val resolvedZones = allCities.map { city ->
            city to runCatching { ZoneId.of(city.zoneId) }.getOrElse { referenceZoneId }
        }

        for (slot in 0 until totalSlots) {
            val slotInstant = dayStartInstant.plus((slot * slotMinutes).toLong(), ChronoUnit.MINUTES)
            val allInWorkingHours = resolvedZones.all { (_, zoneId) ->
                val cityLocalTime = ZonedDateTime.ofInstant(slotInstant, zoneId).toLocalTime()
                isWithinBusinessHours(cityLocalTime, workingStart, workingEnd)
            }
            validSlots[slot] = allInWorkingHours
        }

        // Find the longest contiguous block of true slots
        var bestStartSlot = -1
        var bestLength = 0
        var currentStart = -1
        var currentLength = 0

        for (slot in 0 until totalSlots) {
            if (validSlots[slot]) {
                if (currentStart == -1) {
                    currentStart = slot
                    currentLength = 1
                } else {
                    currentLength++
                }
                if (currentLength > bestLength) {
                    bestLength = currentLength
                    bestStartSlot = currentStart
                }
            } else {
                currentStart = -1
                currentLength = 0
            }
        }

        if (bestLength == 0 || bestStartSlot == -1) {
            return OverlapCalculationResult(
                hasOverlap = false,
                overlapDurationMinutes = 0,
                referenceFormattedRange = "No common working hours found",
                cityWindows = emptyList()
            )
        }

        val overlapStartInstant = dayStartInstant.plus((bestStartSlot * slotMinutes).toLong(), ChronoUnit.MINUTES)
        val overlapDurationMinutes = bestLength * slotMinutes
        val overlapEndInstant = overlapStartInstant.plus(overlapDurationMinutes.toLong(), ChronoUnit.MINUTES)

        val refStartLocal = ZonedDateTime.ofInstant(overlapStartInstant, referenceZoneId).toLocalTime()
        val refEndLocal = ZonedDateTime.ofInstant(overlapEndInstant, referenceZoneId).toLocalTime()
        val refRangeFormatted = timeFormatter.formatTimeRange(refStartLocal, refEndLocal, use24HourFormat)

        val cityWindows = resolvedZones.map { (city, zoneId) ->
            val startLocal = ZonedDateTime.ofInstant(overlapStartInstant, zoneId).toLocalTime()
            val endLocal = ZonedDateTime.ofInstant(overlapEndInstant, zoneId).toLocalTime()
            CityOverlapWindow(
                city = city,
                startLocalTime = startLocal,
                endLocalTime = endLocal,
                formattedRange = timeFormatter.formatTimeRange(startLocal, endLocal, use24HourFormat)
            )
        }

        return OverlapCalculationResult(
            hasOverlap = true,
            overlapDurationMinutes = overlapDurationMinutes,
            referenceFormattedRange = refRangeFormatted,
            cityWindows = cityWindows
        )
    }
}
