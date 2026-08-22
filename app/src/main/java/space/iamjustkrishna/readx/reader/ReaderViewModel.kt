package space.iamjustkrishna.readx.reader

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krishnajeena.pdfengine.NativePdfDocumentEngine
import com.krishnajeena.pdfengine.PdfDocumentHandle
import com.krishnajeena.pdfengine.PdfPageInfo
import com.krishnajeena.pdfengine.PdfRect
import com.krishnajeena.pdfengine.PdfSearchMatch
import com.krishnajeena.pdfengine.PdfSource
import com.krishnajeena.pdfengine.PdfTextPage
import com.krishnajeena.pdfengine.TextSelection
import space.iamjustkrishna.readx.ai.AiService
import space.iamjustkrishna.readx.data.HighlightRepository
import space.iamjustkrishna.readx.data.SettingsRepository
import space.iamjustkrishna.readx.model.Highlight
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data class Home(val recent: RecentDocument?, val recentList: List<RecentDocument> = emptyList()) : ReaderUiState
    data object Loading : ReaderUiState
    data class Document(val document: PdfDocumentHandle) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

data class RecentDocument(
    val uri: Uri,
    val displayName: String,
    val lastOpened: Long = System.currentTimeMillis()
)

data class SelectionUiState(
    val selection: TextSelection,
    /** Per-line highlight quads in page points (top-left origin). */
    val quads: List<PdfRect>
)

data class SearchUiState(
    val matches: List<PdfSearchMatch> = emptyList(),
    val activeIndex: Int = -1,
    val ran: Boolean = false
) {
    val activeMatch: PdfSearchMatch? get() = matches.getOrNull(activeIndex)
}

data class AiResultState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val selectedText: String = ""
)

data class AiChatSessionState(
    val documentUri: Uri,
    val documentTitle: String,
    val pageCount: Int,
    val messages: List<space.iamjustkrishna.readx.ai.AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = NativePdfDocumentEngine()
    val highlightRepo = HighlightRepository(application)
    val settingsRepo = SettingsRepository(application)
    val favoritesRepo = space.iamjustkrishna.readx.data.FavoritesRepository(application)
    val favoritedUris: StateFlow<Set<String>> = favoritesRepo.favorites
    val analyticsRepo = space.iamjustkrishna.readx.data.ReadingAnalyticsRepository(application)
    val weeklyStats = analyticsRepo.weeklyStats
    val aiChatHistoryRepo = space.iamjustkrishna.readx.data.AiChatHistoryRepository(application)
    val savedChatSessions: StateFlow<List<space.iamjustkrishna.readx.data.SavedAiChatSession>> = aiChatHistoryRepo.sessions
    private val aiService = AiService()

    fun toggleFavorite(uriStr: String): Boolean {
        return favoritesRepo.toggleFavorite(uriStr)
    }

    private val pdfScanner = space.iamjustkrishna.readx.data.PdfScanner(application)

    private val _scannedPdfs = MutableStateFlow<List<space.iamjustkrishna.readx.data.ScannedPdf>>(emptyList())
    val scannedPdfs: StateFlow<List<space.iamjustkrishna.readx.data.ScannedPdf>> = _scannedPdfs.asStateFlow()

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Home(loadRecent(), loadRecentList()))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _selection = MutableStateFlow<SelectionUiState?>(null)
    val selection: StateFlow<SelectionUiState?> = _selection.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _showGlyphBoxes = MutableStateFlow(false)
    val showGlyphBoxes: StateFlow<Boolean> = _showGlyphBoxes.asStateFlow()

    private val _aiResult = MutableStateFlow(AiResultState())
    val aiResult: StateFlow<AiResultState> = _aiResult.asStateFlow()

    private val _aiChatSession = MutableStateFlow<AiChatSessionState?>(null)
    val aiChatSession: StateFlow<AiChatSessionState?> = _aiChatSession.asStateFlow()

    private val contextRetriever = space.iamjustkrishna.readx.ai.DocumentContextRetriever()
    private var aiChatDocHandle: PdfDocumentHandle? = null

    private val _jumpToPage = MutableStateFlow<Int?>(null)
    val jumpToPage: StateFlow<Int?> = _jumpToPage.asStateFlow()

    /** Highlights for the currently open document. */
    val highlights: StateFlow<List<Highlight>> = highlightRepo.highlights
    val allSavedHighlights: StateFlow<List<Highlight>> = highlightRepo.allHighlights

    fun clearAppCache(): Long {
        var deletedBytes = 0L
        fun deleteRecursively(file: java.io.File): Long {
            var bytes = 0L
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    bytes += deleteRecursively(child)
                }
                runCatching { file.delete() }
            } else if (file.isFile) {
                bytes += file.length()
                runCatching { file.delete() }
            }
            return bytes
        }

        try {
            val app = getApplication<Application>()
            app.cacheDir?.let { cDir ->
                cDir.listFiles()?.forEach { child ->
                    deletedBytes += deleteRecursively(child)
                }
            }
            app.externalCacheDir?.let { extDir ->
                extDir.listFiles()?.forEach { child ->
                    deletedBytes += deleteRecursively(child)
                }
            }
            app.codeCacheDir?.let { codeDir ->
                codeDir.listFiles()?.forEach { child ->
                    deletedBytes += deleteRecursively(child)
                }
            }
        } catch (_: Exception) {}
        return deletedBytes
    }

    fun clearThumbnailCache(): Long = clearAppCache()

    fun jumpToPage(pageIndex: Int) {
        _jumpToPage.value = pageIndex
    }

    fun clearJumpToPage() {
        _jumpToPage.value = null
    }

    /** Word the initial long-press landed on; anchors word-granular drag expansion. */
    private var anchorWordRange: IntRange? = null
    private var selectionJob: Job? = null
    var currentDocUri: String? = null
        private set
    val currentDocumentUri: String? get() = currentDocUri

    // Session tracking for real-time analytics
    private var sessionStartTime: Long? = null
    private val sessionVisitedPages = mutableSetOf<Int>()
    private var isBookFinishedRecorded = false

    fun onPageVisible(pageIndex: Int, totalPages: Int) {
        sessionVisitedPages.add(pageIndex)
        if (totalPages > 0 && pageIndex >= totalPages - 1) {
            isBookFinishedRecorded = true
        }
    }

    fun flushReadingSession() {
        val start = sessionStartTime ?: return
        val durationSeconds = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0L)
        val pagesCount = sessionVisitedPages.size
        val completedUri = if (isBookFinishedRecorded) currentDocUri else null

        if (durationSeconds > 2 || pagesCount > 0 || completedUri != null) {
            analyticsRepo.logReadingSession(durationSeconds, pagesCount, completedUri)
        }

        sessionStartTime = null
        sessionVisitedPages.clear()
        isBookFinishedRecorded = false
    }

    fun scanDevicePdfs() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pdfs = pdfScanner.scanPdfs()
            _scannedPdfs.value = pdfs
        }
    }

    private val document: PdfDocumentHandle?
        get() = (_uiState.value as? ReaderUiState.Document)?.document

    fun openDocument(uri: Uri) {
        val context = getApplication<Application>()
        closeDocument(returnHome = false)
        _uiState.value = ReaderUiState.Loading
        viewModelScope.launch {
            val name = context.displayNameFor(uri)
            engine.open(PdfSource.Document(uri, context, name)).fold(
                onSuccess = { handle ->
                    currentDocUri = uri.toString()
                    sessionStartTime = System.currentTimeMillis()
                    sessionVisitedPages.clear()
                    isBookFinishedRecorded = false
                    saveRecent(RecentDocument(uri, name))
                    highlightRepo.loadForDocument(uri.toString())
                    _uiState.value = ReaderUiState.Document(handle)
                },
                onFailure = { error ->
                    _uiState.value = ReaderUiState.Error(error.message ?: "Failed to open PDF.")
                }
            )
        }
    }

    fun closeDocument(returnHome: Boolean = true) {
        flushReadingSession()
        document?.close()
        clearSelection()
        _search.value = SearchUiState()
        highlightRepo.clear()
        currentDocUri = null
        clearAiResult()
        if (returnHome) {
            analyticsRepo.refreshStats()
            _uiState.value = ReaderUiState.Home(loadRecent(), loadRecentList())
        }
    }

    fun backToHome() {
        flushReadingSession()
        analyticsRepo.refreshStats()
        _uiState.value = ReaderUiState.Home(loadRecent(), loadRecentList())
    }

    fun toggleGlyphBoxes() {
        _showGlyphBoxes.value = !_showGlyphBoxes.value
    }

    suspend fun pageInfo(pageIndex: Int): PdfPageInfo? =
        document?.let { runCatching { it.getPageInfo(pageIndex) }.getOrNull() }

    suspend fun textPage(pageIndex: Int): PdfTextPage? =
        document?.let { runCatching { it.getTextPage(pageIndex) }.getOrNull() }

    suspend fun renderPage(pageIndex: Int, scale: Float) =
        document?.let { runCatching { it.renderPage(com.krishnajeena.pdfengine.PdfRenderRequest(pageIndex, scale)) } }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    /** Long-press: hit-test the glyph under the finger and snap to its word. */
    fun startSelection(pageIndex: Int, xPt: Float, yPt: Float, onSelected: () -> Unit = {}) {
        val doc = document ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            val charIndex = doc.hitTestGlyph(pageIndex, xPt, yPt)
            if (charIndex < 0) {
                clearSelection()
                return@launch
            }
            val word = wordAt(pageIndex, charIndex) ?: (charIndex until charIndex + 1)
            anchorWordRange = word
            applySelection(TextSelection(pageIndex, word.first, word.last + 1))
            onSelected()
        }
    }

    /** Initial drag after long-press: word-granular expansion from the anchor word. */
    fun dragSelection(pageIndex: Int, xPt: Float, yPt: Float) {
        val doc = document ?: return
        val anchor = anchorWordRange ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            val charIndex = doc.hitTestGlyph(pageIndex, xPt, yPt, tolerancePt = 12f)
            if (charIndex < 0) return@launch
            val word = wordAt(pageIndex, charIndex) ?: (charIndex until charIndex + 1)
            val start = minOf(anchor.first, word.first)
            val end = maxOf(anchor.last + 1, word.last + 1)
            applySelection(TextSelection(pageIndex, start, end))
        }
    }

    /**
     * Handle drag: character-granular. [movingStart] says which end follows
     * the finger; the other end stays fixed.
     */
    fun dragHandle(movingStart: Boolean, xPt: Float, yPt: Float) {
        val doc = document ?: return
        val current = _selection.value?.selection ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            val charIndex = doc.hitTestGlyph(current.pageIndex, xPt, yPt, tolerancePt = 12f)
            if (charIndex < 0) return@launch
            val updated = if (movingStart) {
                TextSelection(
                    current.pageIndex,
                    minOf(charIndex, current.endChar - 1),
                    current.endChar
                )
            } else {
                TextSelection(
                    current.pageIndex,
                    current.startChar,
                    maxOf(charIndex + 1, current.startChar + 1)
                )
            }
            applySelection(updated)
        }
    }

    fun clearSelection() {
        selectionJob?.cancel()
        anchorWordRange = null
        _selection.value = null
    }

    /**
     * Text for a captured selection. Takes the range as a parameter so the
     * result cannot be affected by the selection being cleared concurrently
     * (e.g. by a tap racing the Copy button's coroutine).
     */
    suspend fun textFor(selection: TextSelection): String {
        val doc = document ?: return ""
        return doc.textForRange(selection.pageIndex, selection.startChar, selection.endChar)
    }

    fun selectAllOnPage(pageIndex: Int) {
        val doc = document ?: return
        viewModelScope.launch {
            val tp = doc.getTextPage(pageIndex)
            if (tp.glyphs.isEmpty()) return@launch
            applySelection(TextSelection(pageIndex, 0, tp.glyphs.size))
        }
    }

    private suspend fun applySelection(selection: TextSelection) {
        val doc = document ?: return
        val quads = doc.selectionRects(selection.pageIndex, selection.startChar, selection.endChar)
        _selection.value = SelectionUiState(selection, quads)
    }

    /** The char range of the word containing [charIndex], from the text layout. */
    private suspend fun wordAt(pageIndex: Int, charIndex: Int): IntRange? {
        val tp = textPage(pageIndex) ?: return null
        val words = tp.words
        // Binary search over word charStart, then verify containment.
        var low = 0
        var high = words.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val word = words[mid]
            when {
                charIndex < word.charStart -> high = mid - 1
                charIndex >= word.charEnd -> low = mid + 1
                else -> return word.charStart until word.charEnd
            }
        }
        return null
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    private var searchJob: Job? = null

    fun runSearch(query: String) {
        val doc = document ?: return
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = SearchUiState()
            return
        }
        searchJob = viewModelScope.launch {
            val matches = doc.search(query)
            _search.value = SearchUiState(
                matches = matches,
                activeIndex = if (matches.isNotEmpty()) 0 else -1,
                ran = true
            )
        }
    }

    private fun unusedOldSearch() {
// skipped
// skipped
// skipped
// skipped
// skipped
// skipped
// skipped
// skipped
// skipped
// skipped
    }
    fun clearSearch() {
        _search.value = SearchUiState()
    }

    fun nextMatch() = stepMatch(1)

    fun previousMatch() = stepMatch(-1)

    private fun stepMatch(delta: Int) {
        val current = _search.value
        if (current.matches.isEmpty()) return
        val size = current.matches.size
        _search.value = current.copy(activeIndex = (current.activeIndex + delta + size) % size)
    }

    // ------------------------------------------------------------------
    // Highlights
    // ------------------------------------------------------------------

    /** Highlight the current selection in yellow. */
    fun highlightSelection() {
        val sel = _selection.value?.selection ?: return
        val docUri = currentDocUri ?: return
        viewModelScope.launch {
            val text = textFor(sel)
            if (text.isBlank()) return@launch
            val highlight = Highlight(
                pageIndex = sel.pageIndex,
                startChar = sel.startChar,
                endChar = sel.endChar,
                text = text
            )
            highlightRepo.addHighlight(docUri, highlight)
            clearSelection()
        }
    }

    fun deleteHighlight(highlightId: String) {
        val docUri = currentDocUri ?: return
        highlightRepo.deleteHighlight(docUri, highlightId)
    }

    fun updateHighlightNote(highlightId: String, note: String) {
        val docUri = currentDocUri ?: return
        highlightRepo.updateNote(docUri, highlightId, note)
    }

    /**
     * Extracts text for selection along with surrounding paragraph text context
     * so short phrase selections (1-10 words) have full context for AI.
     */
    suspend fun textWithContextFor(selection: TextSelection): Pair<String, String> {
        val doc = document ?: return "" to ""
        val selectedText = doc.textForRange(selection.pageIndex, selection.startChar, selection.endChar)
        val textPage = textPage(selection.pageIndex) ?: return selectedText to selectedText
        val totalChars = textPage.glyphs.size

        // Extract 200 chars before and after for surrounding context
        val contextStart = (selection.startChar - 200).coerceAtLeast(0)
        val contextEnd = (selection.endChar + 200).coerceAtMost(totalChars)
        val contextText = doc.textForRange(selection.pageIndex, contextStart, contextEnd)

        return selectedText to contextText
    }

    // ------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------

    fun clearAiResult() {
        _aiResult.value = AiResultState()
    }

    fun runAiAction(selectedText: String, systemPrompt: String, contextText: String = "") {
        val provider = settingsRepo.getProvider()
        val apiKey = settingsRepo.getActiveApiKey()
        if (apiKey.isNullOrBlank()) {
            _aiResult.value = AiResultState(error = "Set your ${provider.displayName} API key in Settings")
            return
        }
        val model = settingsRepo.getAiModel()

        val fullPromptText = if (contextText.isNotBlank() && contextText != selectedText) {
            "Selected text: \"$selectedText\"\n\nSurrounding context:\n\"$contextText\""
        } else {
            selectedText
        }

        _aiResult.value = AiResultState(isLoading = true, selectedText = selectedText)
        viewModelScope.launch {
            aiService.prompt(provider, systemPrompt, fullPromptText, apiKey, model).fold(
                onSuccess = { result ->
                    _aiResult.value = AiResultState(result = result, selectedText = selectedText)
                },
                onFailure = { error ->
                    _aiResult.value = AiResultState(
                        error = error.message ?: "AI request failed",
                        selectedText = selectedText
                    )
                }
            )
        }
    }

    fun runCustomAiAction(selectedText: String, customPrompt: String, contextText: String = "") {
        runAiAction(selectedText, AiService.PROMPT_CUSTOM_PREFIX + customPrompt, contextText)
    }

    // ------------------------------------------------------------------
    // Document AI Chat
    // ------------------------------------------------------------------

    fun startAiChat(uri: Uri, title: String, existingMessages: List<space.iamjustkrishna.readx.ai.AiChatMessage> = emptyList()) {
        _aiChatSession.value = AiChatSessionState(
            documentUri = uri,
            documentTitle = title,
            pageCount = 0,
            messages = existingMessages,
            isLoading = true
        )

        viewModelScope.launch {
            val context = getApplication<Application>()
            val source = PdfSource.Document(uri, context, title)
            engine.open(source).fold(
                onSuccess = { handle ->
                    aiChatDocHandle?.close()
                    aiChatDocHandle = handle
                    val pageCount = handle.pageCount
                    _aiChatSession.value = AiChatSessionState(
                        documentUri = uri,
                        documentTitle = title,
                        pageCount = pageCount,
                        messages = existingMessages,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _aiChatSession.value = AiChatSessionState(
                        documentUri = uri,
                        documentTitle = title,
                        pageCount = 0,
                        messages = existingMessages,
                        isLoading = false,
                        error = error.message ?: "Failed to load document for AI Chat."
                    )
                }
            )
        }
    }

    fun resumeAiChat(savedSession: space.iamjustkrishna.readx.data.SavedAiChatSession) {
        val uri = Uri.parse(savedSession.documentUriStr)
        startAiChat(uri, savedSession.documentTitle, savedSession.messages)
    }

    fun deleteAiChatSession(sessionId: String) {
        aiChatHistoryRepo.deleteSession(sessionId)
    }

    fun sendAiChatMessage(userMessage: String) {
        val currentSession = _aiChatSession.value ?: return
        if (userMessage.isBlank() || currentSession.isLoading) return

        val userChatMessage = space.iamjustkrishna.readx.ai.AiChatMessage(role = "user", content = userMessage)
        val updatedMessages = currentSession.messages + userChatMessage
        _aiChatSession.value = currentSession.copy(messages = updatedMessages, isLoading = true, error = null)

        viewModelScope.launch {
            val handle = aiChatDocHandle
            val retrieved = if (handle != null) {
                contextRetriever.retrieveContext(handle, userMessage, maxPages = 4)
            } else {
                space.iamjustkrishna.readx.ai.RetrievedContext("", emptyList())
            }

            val provider = settingsRepo.getProvider()
            val apiKey = settingsRepo.getActiveApiKey()
            val model = settingsRepo.getAiModel()

            if (apiKey.isNullOrBlank()) {
                _aiChatSession.value = _aiChatSession.value?.copy(
                    isLoading = false,
                    error = "Please configure your ${provider.displayName} API key in AI Settings first."
                )
                return@launch
            }

            val systemPrompt = """
                You are ReadX AI Assistant analyzing the PDF document '${currentSession.documentTitle}'.
                Use the following extracted excerpts from the document to answer the user's question accurately.
                Cite the page number like [Page X] when referencing facts from the text.
                If the excerpts do not contain the answer, answer based on general knowledge but state that it's outside the provided excerpt.
                Do not use em-dashes (â€”); use regular hyphens (-) for lists or separators.

                ${retrieved.contextPrompt}
            """.trimIndent()

            aiService.chat(provider, systemPrompt, updatedMessages, apiKey, model).fold(
                onSuccess = { responseText ->
                    // Replace any accidental em dashes with standard hyphens
                    val cleanText = responseText.replace("â€”", " - ").replace("--", " - ")
                    val assistantMsg = space.iamjustkrishna.readx.ai.AiChatMessage(
                        role = "assistant",
                        content = cleanText,
                        referencedPages = retrieved.referencedPages
                    )
                    val finalMessages = updatedMessages + assistantMsg
                    _aiChatSession.value = _aiChatSession.value?.copy(
                        messages = finalMessages,
                        isLoading = false,
                        error = null
                    )
                    // Persist session to history
                    val sessionId = "${currentSession.documentUri}_history"
                    aiChatHistoryRepo.saveSession(
                        sessionId = sessionId,
                        documentUriStr = currentSession.documentUri.toString(),
                        documentTitle = currentSession.documentTitle,
                        messages = finalMessages
                    )
                },
                onFailure = { err ->
                    _aiChatSession.value = _aiChatSession.value?.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to generate AI response."
                    )
                }
            )
        }
    }

    fun closeAiChat() {
        aiChatDocHandle?.close()
        aiChatDocHandle = null
        contextRetriever.clearCache()
        _aiChatSession.value = null
    }


    // ------------------------------------------------------------------
    // Recents
    // ------------------------------------------------------------------

    private fun prefs() =
        getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadRecent(): RecentDocument? {
        return loadRecentList().firstOrNull()
    }

    fun loadRecentList(): List<RecentDocument> {
        val prefs = prefs()
        val raw = prefs.getString(PREF_RECENT_LIST, null)
        if (!raw.isNullOrBlank()) {
            val list = runCatching {
                val jsonArray = org.json.JSONArray(raw)
                val items = mutableListOf<RecentDocument>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val uriStr = obj.optString("uri")
                    val name = obj.optString("name")
                    val time = obj.optLong("time", System.currentTimeMillis())
                    if (uriStr.isNotBlank() && name.isNotBlank()) {
                        items.add(RecentDocument(Uri.parse(uriStr), name, time))
                    }
                }
                items
            }.getOrNull()
            if (!list.isNullOrEmpty()) return list
        }

        val fallbackUri = prefs.getString(PREF_RECENT_URI, null)?.let(Uri::parse)
        val fallbackName = prefs.getString(PREF_RECENT_NAME, null)
        return if (fallbackUri != null && fallbackName != null) {
            listOf(RecentDocument(fallbackUri, fallbackName))
        } else {
            emptyList()
        }
    }

    private fun saveRecent(recent: RecentDocument) {
        val currentList = loadRecentList().toMutableList()
        currentList.removeAll { it.uri.toString() == recent.uri.toString() }
        currentList.add(0, recent)
        val trimmed = currentList.take(15)

        val jsonArray = org.json.JSONArray()
        for (item in trimmed) {
            val obj = org.json.JSONObject()
            obj.put("uri", item.uri.toString())
            obj.put("name", item.displayName)
            obj.put("time", item.lastOpened)
            jsonArray.put(obj)
        }

        prefs().edit()
            .putString(PREF_RECENT_URI, recent.uri.toString())
            .putString(PREF_RECENT_NAME, recent.displayName)
            .putString(PREF_RECENT_LIST, jsonArray.toString())
            .apply()
    }

    override fun onCleared() {
        document?.close()
    }

    suspend fun selectionRects(pageIndex: Int, startChar: Int, endCharExclusive: Int): List<PdfRect> {
        val doc = document ?: return emptyList()
        return doc.selectionRects(pageIndex, startChar, endCharExclusive)
    }

    private companion object {
        const val PREFS_NAME = "readx_reader"
        const val PREF_RECENT_URI = "recent_uri"
        const val PREF_RECENT_NAME = "recent_name"
        const val PREF_RECENT_LIST = "recent_list_json"
    }
}

private fun Context.displayNameFor(uri: Uri): String {
    contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
    return uri.lastPathSegment ?: "Document.pdf"
}

