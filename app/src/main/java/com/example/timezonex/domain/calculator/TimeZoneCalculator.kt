package com.example.timezonex.domain.calculator

import com.example.timezonex.domain.model.CityTimeState
import com.example.timezonex.domain.model.CityTimeZone
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Core Date/Time Engine for TimeZoneX.
 *
 * CRITICAL RULE:
 * Never calculate another city's time by adding/subtracting hours from a local clock.
 * Always convert the exact same [Instant] into each target [ZoneId] using [ZonedDateTime.ofInstant].
 */
class TimeZoneCalculator(
    val timeDifferenceCalculator: TimeDifferenceCalculator = TimeDifferenceCalculator(),
    val dstCalculator: DstCalculator = DstCalculator(),
    val dayNightCalculator: DayNightCalculator = DayNightCalculator(),
    val businessHourCalculator: BusinessHourCalculator = BusinessHourCalculator(),
    val timeFormatter: TimeFormatter = TimeFormatter()
) {
    private val zoneCache = mutableMapOf<String, ZoneId>()

    fun getCurrentInstant(): Instant = Instant.now()

    fun getSystemZoneId(): ZoneId = ZoneId.systemDefault()

    fun resolveZoneId(zoneIdString: String): ZoneId? {
        return synchronized(zoneCache) {
            zoneCache[zoneIdString] ?: runCatching {
                ZoneId.of(zoneIdString)
            }.getOrNull()?.also { resolved ->
                zoneCache[zoneIdString] = resolved
            }
        }
    }

    fun convertInstantToZone(instant: Instant, zoneId: ZoneId): ZonedDateTime {
        return ZonedDateTime.ofInstant(instant, zoneId)
    }

    fun convertInstantToZone(instant: Instant, zoneIdString: String): ZonedDateTime {
        val zoneId = resolveZoneId(zoneIdString)
            ?: throw IllegalArgumentException("Invalid time zone ID: $zoneIdString")
        return ZonedDateTime.ofInstant(instant, zoneId)
    }

    fun calculateUtcOffsetSeconds(instant: Instant, zoneId: ZoneId): Int {
        return convertInstantToZone(instant, zoneId).offset.totalSeconds
    }

    fun calculateTimeDifferenceMinutes(
        instant: Instant,
        referenceZoneId: ZoneId,
        targetZoneId: ZoneId
    ): Int {
        val refZoned = convertInstantToZone(instant, referenceZoneId)
        val targetZoned = convertInstantToZone(instant, targetZoneId)
        return timeDifferenceCalculator.calculateOffsetDifferenceMinutes(refZoned, targetZoned)
    }

    fun calculateDayDifference(
        instant: Instant,
        referenceZoneId: ZoneId,
        targetZoneId: ZoneId
    ): Int {
        val refZoned = convertInstantToZone(instant, referenceZoneId)
        val targetZoned = convertInstantToZone(instant, targetZoneId)
        return timeDifferenceCalculator.calculateDayDifference(refZoned, targetZoned)
    }

    fun isDst(instant: Instant, zoneId: ZoneId): Boolean {
        return dstCalculator.isDstActive(instant, zoneId)
    }

    fun isDay(instant: Instant, zoneId: ZoneId): Boolean {
        val zoned = convertInstantToZone(instant, zoneId)
        return dayNightCalculator.isDayTime(zoned)
    }

    /**
     * Calculates the complete [CityTimeState] for a single [CityTimeZone] from the given [instant]
     * and reference [referenceZonedDateTime].
     */
    fun calculateCityTimeState(
        instant: Instant,
        referenceZonedDateTime: ZonedDateTime,
        city: CityTimeZone,
        use24HourFormat: Boolean = false,
        showSeconds: Boolean = false,
        businessStart: LocalTime = LocalTime.of(9, 0),
        businessEnd: LocalTime = LocalTime.of(17, 0)
    ): CityTimeState? {
        val targetZoneId = resolveZoneId(city.zoneId) ?: return null
        val targetZonedDateTime = ZonedDateTime.ofInstant(instant, targetZoneId)
        val utcOffsetSeconds = targetZonedDateTime.offset.totalSeconds
        val differenceMinutes = timeDifferenceCalculator.calculateOffsetDifferenceMinutes(
            referenceOffsetSeconds = referenceZonedDateTime.offset.totalSeconds,
            targetOffsetSeconds = utcOffsetSeconds
        )
        val dayDifference = timeDifferenceCalculator.calculateDayDifference(
            referenceDate = referenceZonedDateTime.toLocalDate(),
            targetDate = targetZonedDateTime.toLocalDate()
        )
        val isDst = dstCalculator.isDstActive(instant, targetZoneId)
        val isDay = dayNightCalculator.isDayTime(targetZonedDateTime)
        val isWithinBusiness = businessHourCalculator.isWithinBusinessHours(
            zonedDateTime = targetZonedDateTime,
            startTime = businessStart,
            endTime = businessEnd
        )

        return CityTimeState(
            city = city,
            localDateTime = targetZonedDateTime,
            utcOffsetSeconds = utcOffsetSeconds,
            differenceMinutes = differenceMinutes,
            dayDifference = dayDifference,
            isDst = isDst,
            isDay = isDay,
            isWithinBusinessHours = isWithinBusiness,
            formattedTime = timeFormatter.formatTime(targetZonedDateTime, use24HourFormat, showSeconds),
            formattedDate = timeFormatter.formatCardDate(targetZonedDateTime.toLocalDate()),
            formattedUtcOffset = timeFormatter.formatUtcOffset(utcOffsetSeconds),
            formattedDifference = timeDifferenceCalculator.formatDifference(differenceMinutes),
            formattedDayDifference = timeDifferenceCalculator.formatDayDifferenceBadge(dayDifference)
        )
    }
}
