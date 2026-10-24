package com.example.timezonex.domain.model

data class CityTimeZone(
    val id: String,
    val cityName: String,
    val countryName: String,
    val countryCode: String,
    val zoneId: String,
    val searchAliases: List<String> = emptyList()
) {
    val flagEmoji: String
        get() {
            if (countryCode.equals("UN", ignoreCase = true) || countryCode.length != 2) {
                return "🌐"
            }
            val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
}
