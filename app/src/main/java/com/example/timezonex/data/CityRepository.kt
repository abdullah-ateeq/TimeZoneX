package com.example.timezonex.data

import android.content.Context
import com.example.timezonex.domain.model.CityTimeZone
import com.example.timezonex.domain.model.SelectedCity
import java.time.ZoneId
import java.util.Locale

class CityRepository(
    private val cityDatabase: CityDatabase,
    private val store: PreferenceStore,
    private val context: Context? = null
) {

    fun getAllCities(): List<CityTimeZone> {
        return cityDatabase.loadCities(context)
    }

    fun getCityById(cityId: String): CityTimeZone? {
        return getAllCities().firstOrNull { it.id == cityId }
    }

    /**
     * Resolves a human-friendly local city/country representation for the device's configured ZoneId
     * without requiring location permissions.
     * If the zone matches a city in our database, uses that city's name; otherwise derives a clean
     * name from the IANA zone identifier (e.g., "America/Indiana/Indianapolis" -> "Indianapolis").
     */
    fun resolveLocalCity(localZoneId: String = ZoneId.systemDefault().id): CityTimeZone {
        val allCities = getAllCities()
        val exactMatch = allCities.firstOrNull { it.zoneId.equals(localZoneId, ignoreCase = true) }
        if (exactMatch != null) {
            return exactMatch
        }

        val cleanName = localZoneId
            .substringAfterLast('/')
            .replace('_', ' ')
            .ifBlank { localZoneId }

        val regionName = if (localZoneId.contains('/')) {
            localZoneId.substringBefore('/').replace('_', ' ')
        } else {
            "Local Time Zone"
        }

        return CityTimeZone(
            id = "local_${localZoneId.lowercase(Locale.US).replace('/', '_')}",
            cityName = cleanName,
            countryName = regionName,
            countryCode = "UN",
            zoneId = localZoneId
        )
    }

    /**
     * Searches cities by:
     * - City name
     * - Country name
     * - Time-zone identifier (e.g., "America/New_York", "Europe/London")
     * - Common aliases (e.g., "uk", "nyc", "ist", "est")
     */
    fun searchCities(query: String): List<CityTimeZone> {
        val all = getAllCities()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all

        val normalizedQuery = normalizeForSearch(trimmed)

        return all.filter { city ->
            val cityNorm = normalizeForSearch(city.cityName)
            val countryNorm = normalizeForSearch(city.countryName)
            val zoneNorm = normalizeForSearch(city.zoneId)
            val zoneRawLower = city.zoneId.lowercase(Locale.US)
            val queryRawLower = trimmed.lowercase(Locale.US)

            cityNorm.contains(normalizedQuery) ||
                countryNorm.contains(normalizedQuery) ||
                zoneNorm.contains(normalizedQuery) ||
                zoneRawLower.contains(queryRawLower) ||
                city.countryCode.lowercase(Locale.US) == queryRawLower ||
                city.searchAliases.any { alias ->
                    normalizeForSearch(alias).contains(normalizedQuery)
                }
        }.sortedWith(
            compareBy<CityTimeZone> { city ->
                val cityNorm = normalizeForSearch(city.cityName)
                when {
                    cityNorm == normalizedQuery -> 0
                    cityNorm.startsWith(normalizedQuery) -> 1
                    normalizeForSearch(city.countryName).startsWith(normalizedQuery) -> 2
                    else -> 3
                }
            }.thenBy { it.cityName }
        )
    }

    private fun normalizeForSearch(input: String): String {
        return input.lowercase(Locale.US)
            .replace("ã", "a")
            .replace("á", "a")
            .replace("â", "a")
            .replace("ä", "a")
            .replace("é", "e")
            .replace("è", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ö", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("_", " ")
            .replace(".", "")
            .trim()
    }

    fun getSelectedCities(
        restoreLastSelection: Boolean = true,
        localZoneId: String = ZoneId.systemDefault().id
    ): List<SelectedCity> {
        val hasInitialized = store.getBoolean(KEY_HAS_INITIALIZED_SELECTIONS, false)
        if (!restoreLastSelection || !hasInitialized) {
            val defaults = getDefaultSelectedCities(localZoneId)
            if (!hasInitialized) {
                saveSelectedCities(defaults)
                store.putBoolean(KEY_HAS_INITIALIZED_SELECTIONS, true)
            }
            return defaults
        }

        val serialized = store.getString(KEY_SELECTED_CITIES, null)
        if (serialized.isNullOrBlank()) {
            return emptyList()
        }

        val allCitiesById = getAllCities().associateBy { it.id }
        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<SelectedCity>()

        serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { cityId ->
            if (!seenIds.contains(cityId)) {
                val city = allCitiesById[cityId]
                if (city != null) {
                    seenIds.add(cityId)
                    result.add(
                        SelectedCity(
                            cityId = city.id,
                            zoneId = city.zoneId,
                            displayOrder = result.size
                        )
                    )
                }
            }
        }
        return result
    }

    fun getDefaultSelectedCities(localZoneId: String = ZoneId.systemDefault().id): List<SelectedCity> {
        val allCities = getAllCities()
        val allById = allCities.associateBy { it.id }
        val orderedIds = mutableListOf<String>()

        // Include user's current local city if available in DB
        val localMatch = allCities.firstOrNull { it.zoneId.equals(localZoneId, ignoreCase = true) }
        if (localMatch != null) {
            orderedIds.add(localMatch.id)
        }

        listOf("london", "new_york", "tokyo").forEach { defaultId ->
            if (!orderedIds.contains(defaultId) && allById.containsKey(defaultId)) {
                orderedIds.add(defaultId)
            }
        }

        return orderedIds.mapIndexed { index, id ->
            val city = allById.getValue(id)
            SelectedCity(
                cityId = city.id,
                zoneId = city.zoneId,
                displayOrder = index
            )
        }
    }

    fun saveSelectedCities(selectedCities: List<SelectedCity>) {
        val uniqueOrderedIds = selectedCities
            .sortedBy { it.displayOrder }
            .map { it.cityId }
            .distinct()
        store.putString(KEY_SELECTED_CITIES, uniqueOrderedIds.joinToString(","))
        store.putBoolean(KEY_HAS_INITIALIZED_SELECTIONS, true)
    }

    companion object {
        private const val KEY_SELECTED_CITIES = "pref_selected_city_ids_ordered"
        private const val KEY_HAS_INITIALIZED_SELECTIONS = "pref_has_initialized_selections"
    }
}
