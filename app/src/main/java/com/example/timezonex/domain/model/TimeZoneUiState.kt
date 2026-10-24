package com.example.timezonex.domain.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class TimeFormatPreference(val label: String) {
    SYSTEM("System"),
    HOURS_12("12-hour"),
    HOURS_24("24-hour")
}

data class UserSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColor: Boolean = true,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    val showSeconds: Boolean = false,
    val showTimeZoneId: Boolean = false,
    val showUtcOffset: Boolean = true,
    val showDstStatus: Boolean = true,
    val maxCities: Int = 12,
    val showDifference: Boolean = true,
    val showDayNight: Boolean = true,
    val showPositionNumbers: Boolean = false,
    val showBusinessHours: Boolean = false,
    val businessStartHour: Int = 9,
    val businessStartMinute: Int = 0,
    val businessEndHour: Int = 17,
    val businessEndMinute: Int = 0,
    val restoreLastSelection: Boolean = true,
    val hapticsEnabled: Boolean = true
) {
    val businessStartTime: LocalTime
        get() = LocalTime.of(businessStartHour.coerceIn(0, 23), businessStartMinute.coerceIn(0, 59))

    val businessEndTime: LocalTime
        get() = LocalTime.of(businessEndHour.coerceIn(0, 23), businessEndMinute.coerceIn(0, 59))
}

data class CityTimeState(
    val city: CityTimeZone,
    val localDateTime: ZonedDateTime,
    val utcOffsetSeconds: Int,
    val differenceMinutes: Int,
    val dayDifference: Int,
    val isDst: Boolean,
    val isDay: Boolean,
    val isWithinBusinessHours: Boolean = false,
    val formattedTime: String = "",
    val formattedDate: String = "",
    val formattedUtcOffset: String = "",
    val formattedDifference: String = "",
    val formattedDayDifference: String? = null
)

data class CityOverlapWindow(
    val city: CityTimeZone,
    val startLocalTime: LocalTime,
    val endLocalTime: LocalTime,
    val formattedRange: String
)

data class OverlapCalculationResult(
    val hasOverlap: Boolean,
    val overlapDurationMinutes: Int,
    val referenceFormattedRange: String,
    val cityWindows: List<CityOverlapWindow>
)

data class TimeZoneUiState(
    val currentInstant: Instant = Instant.now(),
    val localZoneId: String = ZoneId.systemDefault().id,
    val localCityName: String = "",
    val localCountryName: String = "",
    val localDateTime: ZonedDateTime = ZonedDateTime.now(),
    val localFormattedTime: String = "",
    val localFormattedDate: String = "",
    val localFormattedDayOfWeek: String = "",
    val localFormattedUtcOffset: String = "",
    val localIsDst: Boolean = false,
    val localIsDay: Boolean = true,
    val selectedCities: List<CityTimeState> = emptyList(),
    val allCities: List<CityTimeZone> = emptyList(),
    val favoriteCityIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<CityTimeZone> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val overlapResult: OverlapCalculationResult? = null,
    val is24HourResolved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
