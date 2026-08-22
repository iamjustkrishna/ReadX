package space.iamjustkrishna.readx.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists favorited PDF document URIs in SharedPreferences.
 * Provides a reactive StateFlow for Compose UI observation.
 */
class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow<Set<String>>(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
    }

    fun isFavorite(uriStr: String): Boolean {
        return _favorites.value.contains(uriStr)
    }

    fun toggleFavorite(uriStr: String): Boolean {
        val current = _favorites.value.toMutableSet()
        val isNowFavorite = if (current.contains(uriStr)) {
            current.remove(uriStr)
            false
        } else {
            current.add(uriStr)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        _favorites.value = current
        return isNowFavorite
    }

    companion object {
        private const val PREFS_NAME = "readx_favorites"
        private const val KEY_FAVORITES = "favorite_uris_set"
    }
}
