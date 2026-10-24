package com.example.timezonex.domain.calculator

import java.time.LocalTime
import java.time.ZonedDateTime

class DayNightCalculator {

    /**
     * Determines day or night from local time using the standard range:
     * 06:00–17:59 -> Day (true)
     * 18:00–05:59 -> Night (false)
     */
    fun isDayTime(localTime: LocalTime): Boolean {
        val hour = localTime.hour
        return hour in 6..17
    }

    fun isDayTime(zonedDateTime: ZonedDateTime): Boolean {
        return isDayTime(zonedDateTime.toLocalTime())
    }

    fun getDayNightLabel(isDay: Boolean): String {
        return if (isDay) "☀ Day" else "☾ Night"
    }
}
