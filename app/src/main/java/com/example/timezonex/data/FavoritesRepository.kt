package com.example.timezonex.data

class FavoritesRepository(
    private val store: PreferenceStore
) {
    fun getFavoriteCityIds(): Set<String> {
        return store.getStringSet(KEY_FAVORITE_CITIES, emptySet())
    }

    fun isFavorite(cityId: String): Boolean {
        return getFavoriteCityIds().contains(cityId)
    }

    fun toggleFavorite(cityId: String): Set<String> {
        val current = getFavoriteCityIds().toMutableSet()
        if (current.contains(cityId)) {
            current.remove(cityId)
        } else {
            current.add(cityId)
        }
        store.putStringSet(KEY_FAVORITE_CITIES, current)
        return current
    }

    fun addFavorite(cityId: String): Set<String> {
        val current = getFavoriteCityIds().toMutableSet()
        current.add(cityId)
        store.putStringSet(KEY_FAVORITE_CITIES, current)
        return current
    }

    fun removeFavorite(cityId: String): Set<String> {
        val current = getFavoriteCityIds().toMutableSet()
        current.remove(cityId)
        store.putStringSet(KEY_FAVORITE_CITIES, current)
        return current
    }

    fun clearFavorites() {
        store.remove(KEY_FAVORITE_CITIES)
    }

    companion object {
        private const val KEY_FAVORITE_CITIES = "pref_favorite_city_ids"
    }
}
