package com.example.timezonex.domain.calculator

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class TimeFormatter {

    private val time12Formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val time12SecondsFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US)
    private val time24Formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val time24SecondsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.US)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.US)
    private val detailDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)

    fun formatTime(
        localTime: LocalTime,
        use24Hour: Boolean,
        showSeconds: Boolean = false
    ): String {
        val formatter = when {
            use24Hour && showSeconds -> time24SecondsFormatter
            use24Hour && !showSeconds -> time24Formatter
            !use24Hour && showSeconds -> time12SecondsFormatter
            else -> time12Formatter
        }
        return localTime.format(formatter)
    }

    fun formatTime(
        zonedDateTime: ZonedDateTime,
        use24Hour: Boolean,
        showSeconds: Boolean = false
    ): String {
        return formatTime(zonedDateTime.toLocalTime(), use24Hour, showSeconds)
    }

    fun formatShortTime(localTime: LocalTime, use24Hour: Boolean): String {
        return if (use24Hour) {
            localTime.format(time24Formatter)
        } else {
            val pattern = if (localTime.minute == 0) "h:mm a" else "h:mm a"
            localTime.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        }
    }

    fun formatTimeRange(
        start: LocalTime,
        end: LocalTime,
        use24Hour: Boolean
    ): String {
        return "${formatShortTime(start, use24Hour)} – ${formatShortTime(end, use24Hour)}"
    }

    fun formatCardDate(date: LocalDate): String {
        return date.format(shortDateFormatter)
    }

    fun formatFullDate(date: LocalDate): String {
        return date.format(fullDateFormatter)
    }

    fun formatDayOfWeek(date: LocalDate): String {
        return date.format(dayOfWeekFormatter)
    }

    fun formatDetailDate(date: LocalDate): String {
        return date.format(detailDateFormatter)
    }

    /**
     * Formats UTC offset in seconds to standard representation:
     * Examples:
     * 0 -> "UTC +00:00"
     * 18000 -> "UTC +05:00"
     * -14400 -> "UTC -04:00"
     * 19800 -> "UTC +05:30"
     * 20700 -> "UTC +05:45"
     */
    fun formatUtcOffset(offsetSeconds: Int): String {
        val sign = if (offsetSeconds >= 0) "+" else "-"
        val absSeconds = abs(offsetSeconds)
        val hours = absSeconds / 3600
        val minutes = (absSeconds % 3600) / 60
        return String.format(Locale.US, "UTC %s%02d:%02d", sign, hours, minutes)
    }
}
