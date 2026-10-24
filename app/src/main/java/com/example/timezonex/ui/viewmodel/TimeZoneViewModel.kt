package com.example.timezonex.ui.viewmodel

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.timezonex.data.CityDatabase
import com.example.timezonex.data.CityRepository
import com.example.timezonex.data.FavoritesRepository
import com.example.timezonex.data.SettingsRepository
import com.example.timezonex.data.SharedPreferencesStore
import com.example.timezonex.domain.calculator.TimeZoneCalculator
import com.example.timezonex.domain.model.CityTimeState
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.OverlapCalculationResult
import com.example.timezonex.domain.model.SelectedCity
import com.example.timezonex.domain.model.ThemePreference
import com.example.timezonex.domain.model.TimeFormatPreference
import com.example.timezonex.domain.model.TimeZoneUiState
import com.example.timezonex.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeZoneViewModel(
    private val cityRepository: CityRepository,
    private val settingsRepository: SettingsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val calculator: TimeZoneCalculator = TimeZoneCalculator(),
    private val systemIs24HourProvider: () -> Boolean = { false },
    private val localZoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
    initialInstant: Instant = Instant.now()
) : ViewModel() {

    private var selectedCityEntries: MutableList<SelectedCity> = mutableListOf()
    private val _uiState = MutableStateFlow(TimeZoneUiState(currentInstant = initialInstant, isLoading = true))
    val uiState: StateFlow<TimeZoneUiState> = _uiState.asStateFlow()

    init {
        initializeState(initialInstant)
    }

    private fun initializeState(instant: Instant) {
        val settings = settingsRepository.loadSettings()
        val allCities = cityRepository.getAllCities()
        val favorites = favoritesRepository.getFavoriteCityIds()
        val localZone = localZoneIdProvider()
        val savedSelections = cityRepository.getSelectedCities(
            restoreLastSelection = settings.restoreLastSelection,
            localZoneId = localZone.id
        )

        selectedCityEntries = savedSelections
            .distinctBy { it.cityId }
            .take(settings.maxCities)
            .mapIndexed { index, item -> item.copy(displayOrder = index) }
            .toMutableList()

        recalculateState(
            instant = instant,
            settings = settings,
            allCities = allCities,
            favorites = favorites,
            searchQuery = "",
            errorMessage = null
        )
    }

    fun updateCurrentInstant(instant: Instant = calculator.getCurrentInstant()) {
        val current = _uiState.value
        recalculateState(
            instant = instant,
            settings = current.settings,
            allCities = current.allCities,
            favorites = current.favoriteCityIds,
            searchQuery = current.searchQuery,
            errorMessage = current.errorMessage
        )
    }

    fun refreshNow() {
        updateCurrentInstant(calculator.getCurrentInstant())
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Adds a city to the comparison list.
     * Prevents duplicates and enforces the maximum limit (default 12 cities).
     * Returns true if added, false if rejected (already selected or limit reached).
     */
    fun addCity(cityId: String): Boolean {
        val current = _uiState.value
        if (selectedCityEntries.any { it.cityId == cityId }) {
            // Duplicate prevention: do not add a second copy
            return false
        }

        val maxLimit = current.settings.maxCities.coerceIn(1, 12)
        if (selectedCityEntries.size >= maxLimit) {
            _uiState.value = current.copy(
                errorMessage = "You can compare up to $maxLimit cities."
            )
            return false
        }

        val city = cityRepository.getCityById(cityId) ?: return false
        selectedCityEntries.add(
            SelectedCity(
                cityId = city.id,
                zoneId = city.zoneId,
                displayOrder = selectedCityEntries.size
            )
        )
        persistSelections()
        recalculateState(
            instant = current.currentInstant,
            settings = current.settings,
            allCities = current.allCities,
            favorites = current.favoriteCityIds,
            searchQuery = current.searchQuery,
            errorMessage = null
        )
        return true
    }

    fun removeCity(cityId: String) {
        val current = _uiState.value
        val removed = selectedCityEntries.removeAll { it.cityId == cityId }
        if (removed) {
            normalizeOrderAndPersist()
            recalculateState(
                instant = current.currentInstant,
                settings = current.settings,
                allCities = current.allCities,
                favorites = current.favoriteCityIds,
                searchQuery = current.searchQuery,
                errorMessage = null
            )
        }
    }

    fun reorderCity(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in selectedCityEntries.indices || toIndex !in selectedCityEntries.indices) {
            return
        }
        if (fromIndex == toIndex) return

        val current = _uiState.value
        val item = selectedCityEntries.removeAt(fromIndex)
        selectedCityEntries.add(toIndex, item)
        normalizeOrderAndPersist()
        recalculateState(
            instant = current.currentInstant,
            settings = current.settings,
            allCities = current.allCities,
            favorites = current.favoriteCityIds,
            searchQuery = current.searchQuery,
            errorMessage = current.errorMessage
        )
    }

    fun moveCityUp(cityId: String) {
        val index = selectedCityEntries.indexOfFirst { it.cityId == cityId }
        if (index > 0) {
            reorderCity(index, index - 1)
        }
    }

    fun moveCityDown(cityId: String) {
        val index = selectedCityEntries.indexOfFirst { it.cityId == cityId }
        if (index != -1 && index < selectedCityEntries.lastIndex) {
            reorderCity(index, index + 1)
        }
    }

    fun toggleFavorite(cityId: String) {
        val current = _uiState.value
        val updatedFavorites = favoritesRepository.toggleFavorite(cityId)
        _uiState.value = current.copy(favoriteCityIds = updatedFavorites)
    }

    fun clearFavorites() {
        val current = _uiState.value
        favoritesRepository.clearFavorites()
        _uiState.value = current.copy(favoriteCityIds = emptySet())
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value
        val results = cityRepository.searchCities(query)
        _uiState.value = current.copy(
            searchQuery = query,
            searchResults = results
        )
    }

    fun calculateBusinessHourOverlap(): OverlapCalculationResult {
        val current = _uiState.value
        val localZone = runCatching { ZoneId.of(current.localZoneId) }.getOrElse { localZoneIdProvider() }
        val localCity = cityRepository.resolveLocalCity(localZone.id)
        val selectedCities = current.selectedCities.map { it.city }

        val overlap = calculator.businessHourCalculator.findOverlap(
            referenceInstant = current.currentInstant,
            referenceZoneId = localZone,
            referenceCity = localCity,
            selectedCities = selectedCities,
            workingStart = current.settings.businessStartTime,
            workingEnd = current.settings.businessEndTime,
            use24HourFormat = current.is24HourResolved
        )
        _uiState.value = current.copy(overlapResult = overlap)
        return overlap
    }

    // Settings updates
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val current = _uiState.value
        val updated = transform(current.settings)
        settingsRepository.saveSettings(updated)

        // If maxCities was reduced below current count, keep up to maxCities
        if (selectedCityEntries.size > updated.maxCities) {
            selectedCityEntries = selectedCityEntries.take(updated.maxCities).toMutableList()
            normalizeOrderAndPersist()
        }

        recalculateState(
            instant = current.currentInstant,
            settings = updated,
            allCities = current.allCities,
            favorites = current.favoriteCityIds,
            searchQuery = current.searchQuery,
            errorMessage = null
        )
    }

    fun setThemePreference(theme: ThemePreference) = updateSettings { it.copy(theme = theme) }
    fun setDynamicColor(enabled: Boolean) = updateSettings { it.copy(dynamicColor = enabled) }
    fun setTimeFormat(format: TimeFormatPreference) = updateSettings { it.copy(timeFormat = format) }
    fun setShowSeconds(enabled: Boolean) = updateSettings { it.copy(showSeconds = enabled) }
    fun setShowTimeZoneId(enabled: Boolean) = updateSettings { it.copy(showTimeZoneId = enabled) }
    fun setShowUtcOffset(enabled: Boolean) = updateSettings { it.copy(showUtcOffset = enabled) }
    fun setShowDstStatus(enabled: Boolean) = updateSettings { it.copy(showDstStatus = enabled) }
    fun setMaxCities(max: Int) = updateSettings { it.copy(maxCities = max.coerceIn(1, 12)) }
    fun setShowDifference(enabled: Boolean) = updateSettings { it.copy(showDifference = enabled) }
    fun setShowDayNight(enabled: Boolean) = updateSettings { it.copy(showDayNight = enabled) }
    fun setShowPositionNumbers(enabled: Boolean) = updateSettings { it.copy(showPositionNumbers = enabled) }
    fun setShowBusinessHours(enabled: Boolean) = updateSettings { it.copy(showBusinessHours = enabled) }
    fun setBusinessHours(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) = updateSettings {
        it.copy(
            businessStartHour = startHour.coerceIn(0, 23),
            businessStartMinute = startMinute.coerceIn(0, 59),
            businessEndHour = endHour.coerceIn(0, 23),
            businessEndMinute = endMinute.coerceIn(0, 59)
        )
    }
    fun setRestoreLastSelection(enabled: Boolean) = updateSettings { it.copy(restoreLastSelection = enabled) }

    fun resetSettings() {
        val current = _uiState.value
        val reset = settingsRepository.resetSettings()
        recalculateState(
            instant = current.currentInstant,
            settings = reset,
            allCities = current.allCities,
            favorites = current.favoriteCityIds,
            searchQuery = current.searchQuery,
            errorMessage = null
        )
    }

    private fun normalizeOrderAndPersist() {
        selectedCityEntries = selectedCityEntries.mapIndexed { index, entry ->
            entry.copy(displayOrder = index)
        }.toMutableList()
        persistSelections()
    }

    private fun persistSelections() {
        cityRepository.saveSelectedCities(selectedCityEntries)
    }

    private fun resolveUse24Hour(preference: TimeFormatPreference): Boolean {
        return when (preference) {
            TimeFormatPreference.SYSTEM -> systemIs24HourProvider()
            TimeFormatPreference.HOURS_12 -> false
            TimeFormatPreference.HOURS_24 -> true
        }
    }

    private fun recalculateState(
        instant: Instant,
        settings: UserSettings,
        allCities: List<CityTimeZone>,
        favorites: Set<String>,
        searchQuery: String,
        errorMessage: String?
    ) {
        val localZone = localZoneIdProvider()
        val localCity = cityRepository.resolveLocalCity(localZone.id)
        val localZonedDateTime = ZonedDateTime.ofInstant(instant, localZone)
        val use24Hour = resolveUse24Hour(settings.timeFormat)

        val localFormattedTime = calculator.timeFormatter.formatTime(
            zonedDateTime = localZonedDateTime,
            use24Hour = use24Hour,
            showSeconds = settings.showSeconds
        )
        val localFormattedDate = calculator.timeFormatter.formatFullDate(localZonedDateTime.toLocalDate())
        val localFormattedDayOfWeek = calculator.timeFormatter.formatDayOfWeek(localZonedDateTime.toLocalDate())
        val localFormattedUtcOffset = calculator.timeFormatter.formatUtcOffset(localZonedDateTime.offset.totalSeconds)
        val localIsDst = calculator.dstCalculator.isDstActive(instant, localZone)
        val localIsDay = calculator.dayNightCalculator.isDayTime(localZonedDateTime)

        val citiesById = allCities.associateBy { it.id }
        val cityStates = mutableListOf<CityTimeState>()

        for (selected in selectedCityEntries) {
            val city = citiesById[selected.cityId] ?: continue
            val state = calculator.calculateCityTimeState(
                instant = instant,
                referenceZonedDateTime = localZonedDateTime,
                city = city,
                use24HourFormat = use24Hour,
                showSeconds = settings.showSeconds,
                businessStart = settings.businessStartTime,
                businessEnd = settings.businessEndTime
            )
            if (state != null) {
                cityStates.add(state)
            }
        }

        val overlap = calculator.businessHourCalculator.findOverlap(
            referenceInstant = instant,
            referenceZoneId = localZone,
            referenceCity = localCity,
            selectedCities = cityStates.map { it.city },
            workingStart = settings.businessStartTime,
            workingEnd = settings.businessEndTime,
            use24HourFormat = use24Hour
        )

        val searchResults = cityRepository.searchCities(searchQuery)

        _uiState.value = TimeZoneUiState(
            currentInstant = instant,
            localZoneId = localZone.id,
            localCityName = localCity.cityName,
            localCountryName = localCity.countryName,
            localDateTime = localZonedDateTime,
            localFormattedTime = localFormattedTime,
            localFormattedDate = localFormattedDate,
            localFormattedDayOfWeek = localFormattedDayOfWeek,
            localFormattedUtcOffset = localFormattedUtcOffset,
            localIsDst = localIsDst,
            localIsDay = localIsDay,
            selectedCities = cityStates,
            allCities = allCities,
            favoriteCityIds = favorites,
            searchQuery = searchQuery,
            searchResults = searchResults,
            settings = settings,
            overlapResult = overlap,
            is24HourResolved = use24Hour,
            isLoading = false,
            errorMessage = errorMessage
        )
    }

    companion object {
        private const val PREFS_NAME = "timezonex_preferences"

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val store = SharedPreferencesStore(sharedPrefs)
                    val cityDatabase = CityDatabase()
                    val cityRepository = CityRepository(cityDatabase, store, appContext)
                    val settingsRepository = SettingsRepository(store)
                    val favoritesRepository = FavoritesRepository(store)
                    return TimeZoneViewModel(
                        cityRepository = cityRepository,
                        settingsRepository = settingsRepository,
                        favoritesRepository = favoritesRepository,
                        calculator = TimeZoneCalculator(),
                        systemIs24HourProvider = { DateFormat.is24HourFormat(appContext) },
                        localZoneIdProvider = { ZoneId.systemDefault() }
                    ) as T
                }
            }
        }
    }
}
