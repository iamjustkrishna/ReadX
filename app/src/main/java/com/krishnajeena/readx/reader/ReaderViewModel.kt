package com.krishnajeena.readx.reader

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
import com.krishnajeena.readx.ai.AiService
import com.krishnajeena.readx.data.HighlightRepository
import com.krishnajeena.readx.data.SettingsRepository
import com.krishnajeena.readx.model.Highlight
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data class Home(val recent: RecentDocument?) : ReaderUiState
    data object Loading : ReaderUiState
    data class Document(val document: PdfDocumentHandle) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

data class RecentDocument(val uri: Uri, val displayName: String)

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

/** AI action result state. */
data class AiResultState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val selectedText: String = ""
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = NativePdfDocumentEngine()
    val highlightRepo = HighlightRepository(application)
    val settingsRepo = SettingsRepository(application)
    private val aiService = AiService()

    private val pdfScanner = com.krishnajeena.readx.data.PdfScanner(application)

    private val _scannedPdfs = MutableStateFlow<List<com.krishnajeena.readx.data.ScannedPdf>>(emptyList())
    val scannedPdfs: StateFlow<List<com.krishnajeena.readx.data.ScannedPdf>> = _scannedPdfs.asStateFlow()

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Home(loadRecent()))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _selection = MutableStateFlow<SelectionUiState?>(null)
    val selection: StateFlow<SelectionUiState?> = _selection.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _showGlyphBoxes = MutableStateFlow(false)
    val showGlyphBoxes: StateFlow<Boolean> = _showGlyphBoxes.asStateFlow()

    private val _aiResult = MutableStateFlow(AiResultState())
    val aiResult: StateFlow<AiResultState> = _aiResult.asStateFlow()

    private val _jumpToPage = MutableStateFlow<Int?>(null)
    val jumpToPage: StateFlow<Int?> = _jumpToPage.asStateFlow()

    /** Highlights for the currently open document. */
    val highlights: StateFlow<List<Highlight>> = highlightRepo.highlights

    fun jumpToPage(pageIndex: Int) {
        _jumpToPage.value = pageIndex
    }

    fun clearJumpToPage() {
        _jumpToPage.value = null
    }


    /** Word the initial long-press landed on; anchors word-granular drag expansion. */
    private var anchorWordRange: IntRange? = null
    private var selectionJob: Job? = null
    private var currentDocUri: String? = null

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
        document?.close()
        clearSelection()
        _search.value = SearchUiState()
        highlightRepo.clear()
        currentDocUri = null
        clearAiResult()
        if (returnHome) _uiState.value = ReaderUiState.Home(loadRecent())
    }

    fun backToHome() {
        _uiState.value = ReaderUiState.Home(loadRecent())
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

    fun runSearch(query: String) {
        val doc = document ?: return
        viewModelScope.launch {
            val matches = if (query.isBlank()) emptyList() else doc.search(query)
            _search.value = SearchUiState(
                matches = matches,
                activeIndex = if (matches.isNotEmpty()) 0 else -1,
                ran = query.isNotBlank()
            )
        }
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
    // Recents
    // ------------------------------------------------------------------

    private fun prefs() =
        getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadRecent(): RecentDocument? {
        val prefs = prefs()
        val uri = prefs.getString(PREF_RECENT_URI, null)?.let(Uri::parse) ?: return null
        val name = prefs.getString(PREF_RECENT_NAME, null) ?: return null
        return RecentDocument(uri, name)
    }

    private fun saveRecent(recent: RecentDocument) {
        prefs().edit()
            .putString(PREF_RECENT_URI, recent.uri.toString())
            .putString(PREF_RECENT_NAME, recent.displayName)
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

