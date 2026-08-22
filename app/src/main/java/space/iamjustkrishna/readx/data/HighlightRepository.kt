package space.iamjustkrishna.readx.data

import android.content.Context
import android.content.SharedPreferences
import space.iamjustkrishna.readx.model.Highlight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [Highlight]s per-document in SharedPreferences as JSON arrays,
 * keyed by a hash of the document URI.
 */
class HighlightRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    private val _allHighlights = MutableStateFlow<List<Highlight>>(emptyList())
    val allHighlights: StateFlow<List<Highlight>> = _allHighlights.asStateFlow()

    private var currentDocKey: String? = null

    init {
        refreshAllHighlights()
    }

    fun loadForDocument(documentUri: String) {
        currentDocKey = docKey(documentUri)
        _highlights.value = readHighlights(currentDocKey!!)
        refreshAllHighlights()
    }

    fun addHighlight(documentUri: String, highlight: Highlight) {
        val key = docKey(documentUri)
        val list = readHighlights(key).toMutableList()
        list.add(highlight)
        writeHighlights(key, list)
        if (key == currentDocKey) _highlights.value = list
        refreshAllHighlights()
    }

    fun deleteHighlight(documentUri: String, highlightId: String) {
        val key = docKey(documentUri)
        val list = readHighlights(key).filter { it.id != highlightId }
        writeHighlights(key, list)
        if (key == currentDocKey) _highlights.value = list
        refreshAllHighlights()
    }

    fun updateNote(documentUri: String, highlightId: String, note: String) {
        val key = docKey(documentUri)
        val list = readHighlights(key).map {
            if (it.id == highlightId) it.copy(note = note) else it
        }
        writeHighlights(key, list)
        if (key == currentDocKey) _highlights.value = list
        refreshAllHighlights()
    }

    fun clear() {
        currentDocKey = null
        _highlights.value = emptyList()
    }

    fun refreshAllHighlights() {
        val allList = mutableListOf<Highlight>()
        try {
            prefs.all.forEach { (key, value) ->
                if (key.startsWith("hl_") && value is String) {
                    try {
                        val arr = JSONArray(value)
                        for (i in 0 until arr.length()) {
                            allList.add(arr.getJSONObject(i).toHighlight())
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        _allHighlights.value = allList.sortedByDescending { it.createdAt }
    }

    // ---- Serialization ----

    private fun readHighlights(key: String): List<Highlight> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toHighlight() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeHighlights(key: String, highlights: List<Highlight>) {
        val arr = JSONArray()
        highlights.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun Highlight.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pageIndex", pageIndex)
        put("startChar", startChar)
        put("endChar", endChar)
        put("text", text)
        put("color", color)
        put("note", note)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toHighlight(): Highlight = Highlight(
        id = getString("id"),
        pageIndex = getInt("pageIndex"),
        startChar = getInt("startChar"),
        endChar = getInt("endChar"),
        text = getString("text"),
        color = optLong("color", 0xFFFFEB3B),
        note = optString("note", ""),
        createdAt = optLong("createdAt", 0L)
    )

    private fun docKey(uri: String): String = "hl_${uri.hashCode()}"

    companion object {
        private const val PREFS_NAME = "readx_highlights"
    }
}