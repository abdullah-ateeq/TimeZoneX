package com.example.timezonex.domain.calculator

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class TimeDifferenceCalculator {

    /**
     * Calculates the difference in minutes between the target city's current UTC offset
     * and the reference (local) UTC offset at the exact same Instant.
     *
     * Positive means target is ahead of reference.
     * Negative means target is behind reference.
     */
    fun calculateOffsetDifferenceMinutes(
        referenceOffsetSeconds: Int,
        targetOffsetSeconds: Int
    ): Int {
        return (targetOffsetSeconds - referenceOffsetSeconds) / 60
    }

    fun calculateOffsetDifferenceMinutes(
        referenceDateTime: ZonedDateTime,
        targetDateTime: ZonedDateTime
    ): Int {
        return calculateOffsetDifferenceMinutes(
            referenceOffsetSeconds = referenceDateTime.offset.totalSeconds,
            targetOffsetSeconds = targetDateTime.offset.totalSeconds
        )
    }

    /**
     * Calculates the calendar day difference between the reference local date and the target local date.
     * For example:
     * +1 if target is tomorrow relative to reference
     * -1 if target is yesterday relative to reference
     * 0 if both are on the same calendar date
     */
    fun calculateDayDifference(
        referenceDate: LocalDate,
        targetDate: LocalDate
    ): Int {
        return ChronoUnit.DAYS.between(referenceDate, targetDate).toInt()
    }

    fun calculateDayDifference(
        referenceDateTime: ZonedDateTime,
        targetDateTime: ZonedDateTime
    ): Int {
        return calculateDayDifference(
            referenceDate = referenceDateTime.toLocalDate(),
            targetDate = targetDateTime.toLocalDate()
        )
    }

    /**
     * Formats the minute difference into human-readable strings:
     * - "Same time"
     * - "5 hours ahead"
     * - "9 hours behind"
     * - "45 minutes ahead"
     * - "5 hours 30 minutes ahead"
     * - "1 hour ahead"
     */
    fun formatDifference(differenceMinutes: Int): String {
        if (differenceMinutes == 0) {
            return "Same time"
        }

        val direction = if (differenceMinutes > 0) "ahead" else "behind"
        val totalMinutes = abs(differenceMinutes)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val hourPart = when (hours) {
            0 -> ""
            1 -> "1 hour"
            else -> "$hours hours"
        }

        val minutePart = when (minutes) {
            0 -> ""
            1 -> "1 minute"
            else -> "$minutes minutes"
        }

        return when {
            hours > 0 && minutes > 0 -> "$hourPart $minutePart $direction"
            hours > 0 -> "$hourPart $direction"
            else -> "$minutePart $direction"
        }
    }

    fun formatDayDifferenceBadge(dayDifference: Int): String? {
        return when {
            dayDifference == 0 -> null
            dayDifference == 1 -> "+1 day"
            dayDifference == -1 -> "-1 day"
            dayDifference > 1 -> "+$dayDifference days"
            else -> "$dayDifference days"
        }
    }
}
