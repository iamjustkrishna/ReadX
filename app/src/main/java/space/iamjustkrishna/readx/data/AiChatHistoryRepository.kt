package space.iamjustkrishna.readx.data

import android.content.Context
import android.content.SharedPreferences
import space.iamjustkrishna.readx.ai.AiChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedAiChatSession(
    val sessionId: String,
    val documentUriStr: String,
    val documentTitle: String,
    val lastUpdated: Long,
    val messages: List<AiChatMessage>
)

class AiChatHistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow<List<SavedAiChatSession>>(emptyList())
    val sessions: StateFlow<List<SavedAiChatSession>> = _sessions.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val jsonStr = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val list = mutableListOf<SavedAiChatSession>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val msgArr = obj.getJSONArray("messages")
                val msgs = mutableListOf<AiChatMessage>()
                for (j in 0 until msgArr.length()) {
                    val mObj = msgArr.getJSONObject(j)
                    val refArr = mObj.optJSONArray("referencedPages")
                    val refs = mutableListOf<Int>()
                    if (refArr != null) {
                        for (k in 0 until refArr.length()) {
                            refs.add(refArr.getInt(k))
                        }
                    }
                    msgs.add(
                        AiChatMessage(
                            role = mObj.optString("role", "user"),
                            content = mObj.optString("content", ""),
                            referencedPages = refs
                        )
                    )
                }
                list.add(
                    SavedAiChatSession(
                        sessionId = obj.getString("sessionId"),
                        documentUriStr = obj.getString("documentUriStr"),
                        documentTitle = obj.getString("documentTitle"),
                        lastUpdated = obj.getLong("lastUpdated"),
                        messages = msgs
                    )
                )
            }
        } catch (_: Exception) {}

        _sessions.value = list.sortedByDescending { it.lastUpdated }
    }

    fun saveSession(
        sessionId: String,
        documentUriStr: String,
        documentTitle: String,
        messages: List<AiChatMessage>
    ) {
        if (messages.isEmpty()) return
        val current = _sessions.value.toMutableList()
        current.removeAll { it.sessionId == sessionId || it.documentUriStr == documentUriStr }

        val cleanMessages = messages.filter { it.content.isNotBlank() }
        if (cleanMessages.isEmpty()) return

        val newSession = SavedAiChatSession(
            sessionId = sessionId,
            documentUriStr = documentUriStr,
            documentTitle = documentTitle,
            lastUpdated = System.currentTimeMillis(),
            messages = cleanMessages
        )
        current.add(0, newSession)
        persistSessions(current)
    }

    fun deleteSession(sessionId: String) {
        val current = _sessions.value.toMutableList()
        current.removeAll { it.sessionId == sessionId }
        persistSessions(current)
    }

    private fun persistSessions(list: List<SavedAiChatSession>) {
        val arr = JSONArray()
        list.take(20).forEach { session ->
            val obj = JSONObject()
            obj.put("sessionId", session.sessionId)
            obj.put("documentUriStr", session.documentUriStr)
            obj.put("documentTitle", session.documentTitle)
            obj.put("lastUpdated", session.lastUpdated)
            val msgArr = JSONArray()
            session.messages.forEach { m ->
                val mObj = JSONObject()
                mObj.put("role", m.role)
                mObj.put("content", m.content)
                val refArr = JSONArray()
                m.referencedPages.forEach { refArr.put(it) }
                mObj.put("referencedPages", refArr)
                msgArr.put(mObj)
            }
            obj.put("messages", msgArr)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
        _sessions.value = list
    }

    companion object {
        private const val PREFS_NAME = "readx_ai_chat_history"
        private const val KEY_SESSIONS = "chat_sessions"
    }
}
