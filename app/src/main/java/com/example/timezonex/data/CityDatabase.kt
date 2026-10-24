package com.example.timezonex.data

import android.content.Context
import android.util.Log
import com.example.timezonex.domain.model.CityTimeZone
import org.json.JSONArray
import java.time.ZoneId

class CityDatabase(
    private val jsonProvider: (() -> String)? = null
) {
    @Volatile
    private var cachedCities: List<CityTimeZone>? = null

    fun loadCities(context: Context? = null): List<CityTimeZone> {
        cachedCities?.let { return it }

        synchronized(this) {
            cachedCities?.let { return it }

            val rawJson = try {
                when {
                    jsonProvider != null -> jsonProvider.invoke()
                    context != null -> context.assets.open("cities.json").bufferedReader().use { it.readText() }
                    else -> DEFAULT_FALLBACK_JSON
                }
            } catch (e: Exception) {
                logWarning("Failed to read cities.json, falling back to bundled defaults", e)
                DEFAULT_FALLBACK_JSON
            }

            val parsed = parseCitiesJson(rawJson)
            cachedCities = parsed
            return parsed
        }
    }

    fun parseCitiesJson(jsonString: String): List<CityTimeZone> {
        val result = mutableListOf<CityTimeZone>()
        try {
            val jsonArray = JSONArray(jsonString)
            val seenIds = mutableSetOf<String>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val cityName = obj.optString("cityName", "").trim()
                val countryName = obj.optString("countryName", "").trim()
                val countryCode = obj.optString("countryCode", "").trim()
                val zoneId = obj.optString("zoneId", "").trim()

                if (id.isEmpty() || cityName.isEmpty() || zoneId.isEmpty()) {
                    logWarning("Skipping incomplete city entry at index $i")
                    continue
                }

                if (seenIds.contains(id)) {
                    logWarning("Skipping duplicate city ID: $id")
                    continue
                }

                // Validate IANA ZoneId so invalid zone IDs never crash the application
                val isValidZone = try {
                    ZoneId.of(zoneId)
                    true
                } catch (e: Exception) {
                    logWarning("Skipping city '$cityName' with invalid IANA ZoneId '$zoneId'", e)
                    false
                }

                if (!isValidZone) continue

                val aliasesArray = obj.optJSONArray("searchAliases")
                val aliases = mutableListOf<String>()
                if (aliasesArray != null) {
                    for (j in 0 until aliasesArray.length()) {
                        val alias = aliasesArray.optString(j, "").trim()
                        if (alias.isNotEmpty()) {
                            aliases.add(alias)
                        }
                    }
                }

                seenIds.add(id)
                result.add(
                    CityTimeZone(
                        id = id,
                        cityName = cityName,
                        countryName = countryName,
                        countryCode = countryCode.ifEmpty { "UN" },
                        zoneId = zoneId,
                        searchAliases = aliases
                    )
                )
            }
        } catch (e: Exception) {
            logWarning("Error parsing cities JSON", e)
        }
        return result
    }

    private fun logWarning(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w("CityDatabase", message, throwable)
            } else {
                Log.w("CityDatabase", message)
            }
        } catch (_: Throwable) {
            // In pure JVM unit tests where android.util.Log is not mocked
        }
    }

    companion object {
        private const val DEFAULT_FALLBACK_JSON = """
            [
              {"id": "karachi", "cityName": "Karachi", "countryName": "Pakistan", "countryCode": "PK", "zoneId": "Asia/Karachi", "searchAliases": ["pakistan", "khi"]},
              {"id": "london", "cityName": "London", "countryName": "United Kingdom", "countryCode": "GB", "zoneId": "Europe/London", "searchAliases": ["uk", "united kingdom", "gmt", "bst"]},
              {"id": "new_york", "cityName": "New York", "countryName": "United States", "countryCode": "US", "zoneId": "America/New_York", "searchAliases": ["usa", "nyc", "est", "edt"]},
              {"id": "tokyo", "cityName": "Tokyo", "countryName": "Japan", "countryCode": "JP", "zoneId": "Asia/Tokyo", "searchAliases": ["japan", "tyo", "jst"]}
            ]
        """
    }
}
