package com.example.timezonex.data

import com.example.timezonex.domain.model.ThemePreference
import com.example.timezonex.domain.model.TimeFormatPreference
import com.example.timezonex.domain.model.UserSettings

class SettingsRepository(
    private val store: PreferenceStore
) {
    fun loadSettings(): UserSettings {
        val themeName = store.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        val theme = runCatching { ThemePreference.valueOf(themeName ?: ThemePreference.SYSTEM.name) }
            .getOrDefault(ThemePreference.SYSTEM)

        val timeFormatName = store.getString(KEY_TIME_FORMAT, TimeFormatPreference.SYSTEM.name)
        val timeFormat = runCatching { TimeFormatPreference.valueOf(timeFormatName ?: TimeFormatPreference.SYSTEM.name) }
            .getOrDefault(TimeFormatPreference.SYSTEM)

        return UserSettings(
            theme = theme,
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, true),
            timeFormat = timeFormat,
            showSeconds = store.getBoolean(KEY_SHOW_SECONDS, false),
            showTimeZoneId = store.getBoolean(KEY_SHOW_TIME_ZONE_ID, false),
            showUtcOffset = store.getBoolean(KEY_SHOW_UTC_OFFSET, true),
            showDstStatus = store.getBoolean(KEY_SHOW_DST_STATUS, true),
            maxCities = store.getInt(KEY_MAX_CITIES, 12).coerceIn(1, 12),
            showDifference = store.getBoolean(KEY_SHOW_DIFFERENCE, true),
            showDayNight = store.getBoolean(KEY_SHOW_DAY_NIGHT, true),
            showPositionNumbers = store.getBoolean(KEY_SHOW_POSITION_NUMBERS, false),
            showBusinessHours = store.getBoolean(KEY_SHOW_BUSINESS_HOURS, false),
            businessStartHour = store.getInt(KEY_BUSINESS_START_HOUR, 9).coerceIn(0, 23),
            businessStartMinute = store.getInt(KEY_BUSINESS_START_MINUTE, 0).coerceIn(0, 59),
            businessEndHour = store.getInt(KEY_BUSINESS_END_HOUR, 17).coerceIn(0, 23),
            businessEndMinute = store.getInt(KEY_BUSINESS_END_MINUTE, 0).coerceIn(0, 59),
            restoreLastSelection = store.getBoolean(KEY_RESTORE_LAST_SELECTION, true),
            hapticsEnabled = store.getBoolean(KEY_HAPTICS_ENABLED, true)
        )
    }

    fun saveSettings(settings: UserSettings) {
        store.putString(KEY_THEME, settings.theme.name)
        store.putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
        store.putString(KEY_TIME_FORMAT, settings.timeFormat.name)
        store.putBoolean(KEY_SHOW_SECONDS, settings.showSeconds)
        store.putBoolean(KEY_SHOW_TIME_ZONE_ID, settings.showTimeZoneId)
        store.putBoolean(KEY_SHOW_UTC_OFFSET, settings.showUtcOffset)
        store.putBoolean(KEY_SHOW_DST_STATUS, settings.showDstStatus)
        store.putInt(KEY_MAX_CITIES, settings.maxCities.coerceIn(1, 12))
        store.putBoolean(KEY_SHOW_DIFFERENCE, settings.showDifference)
        store.putBoolean(KEY_SHOW_DAY_NIGHT, settings.showDayNight)
        store.putBoolean(KEY_SHOW_POSITION_NUMBERS, settings.showPositionNumbers)
        store.putBoolean(KEY_SHOW_BUSINESS_HOURS, settings.showBusinessHours)
        store.putInt(KEY_BUSINESS_START_HOUR, settings.businessStartHour.coerceIn(0, 23))
        store.putInt(KEY_BUSINESS_START_MINUTE, settings.businessStartMinute.coerceIn(0, 59))
        store.putInt(KEY_BUSINESS_END_HOUR, settings.businessEndHour.coerceIn(0, 23))
        store.putInt(KEY_BUSINESS_END_MINUTE, settings.businessEndMinute.coerceIn(0, 59))
        store.putBoolean(KEY_RESTORE_LAST_SELECTION, settings.restoreLastSelection)
        store.putBoolean(KEY_HAPTICS_ENABLED, settings.hapticsEnabled)
    }

    fun resetSettings(): UserSettings {
        val defaults = UserSettings()
        saveSettings(defaults)
        return defaults
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_TIME_FORMAT = "pref_time_format"
        private const val KEY_SHOW_SECONDS = "pref_show_seconds"
        private const val KEY_SHOW_TIME_ZONE_ID = "pref_show_tz_id"
        private const val KEY_SHOW_UTC_OFFSET = "pref_show_utc_offset"
        private const val KEY_SHOW_DST_STATUS = "pref_show_dst_status"
        private const val KEY_MAX_CITIES = "pref_max_cities"
        private const val KEY_SHOW_DIFFERENCE = "pref_show_difference"
        private const val KEY_SHOW_DAY_NIGHT = "pref_show_day_night"
        private const val KEY_SHOW_POSITION_NUMBERS = "pref_show_position_numbers"
        private const val KEY_SHOW_BUSINESS_HOURS = "pref_show_business_hours"
        private const val KEY_BUSINESS_START_HOUR = "pref_business_start_hour"
        private const val KEY_BUSINESS_START_MINUTE = "pref_business_start_minute"
        private const val KEY_BUSINESS_END_HOUR = "pref_business_end_hour"
        private const val KEY_BUSINESS_END_MINUTE = "pref_business_end_minute"
        private const val KEY_RESTORE_LAST_SELECTION = "pref_restore_last_selection"
        private const val KEY_HAPTICS_ENABLED = "pref_haptics_enabled"
    }
}
