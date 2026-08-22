package space.iamjustkrishna.readx.ai

import com.krishnajeena.pdfengine.PdfDocumentHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class RetrievedContext(
    val contextPrompt: String,
    val referencedPages: List<Int>
)

class DocumentContextRetriever {

    private val cachedPageText = mutableMapOf<Int, String>()
    private var totalPagesCount: Int = 0

    // Common English stop words to ignore when extracting key concepts
    private val stopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
        "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing",
        "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
        "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
        "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
        "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
        "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours",
        "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should",
        "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
        "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've",
        "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we",
        "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where",
        "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
        "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves",
        "tell", "me", "summarize", "summary", "explain", "give", "brief", "pdf", "book", "document", "describe"
    )

    fun clearCache() {
        cachedPageText.clear()
        totalPagesCount = 0
    }

    /**
     * Extracts and retrieves the most relevant pages for the user's question.
     */
    suspend fun retrieveContext(
        handle: PdfDocumentHandle,
        query: String,
        maxPages: Int = 4
    ): RetrievedContext = withContext(Dispatchers.IO) {
        totalPagesCount = handle.pageCount
        if (totalPagesCount == 0) return@withContext RetrievedContext("", emptyList())

        val normalizedQuery = query.lowercase(Locale.ROOT).trim()

        // Detect if query is a broad/general question (e.g. summary, overview)
        val isBroadQuery = isBroadQuestion(normalizedQuery)

        // Ensure we have loaded some or all pages into cache
        val pagesToInspect = if (totalPagesCount <= 30) {
            (0 until totalPagesCount).toList()
        } else if (isBroadQuery) {
            // For broad summary on long books, inspect beginning pages, middle, and TOC
            val sampled = mutableSetOf<Int>()
            for (p in 0 until minOf(8, totalPagesCount)) sampled.add(p)
            for (p in totalPagesCount - 3 until totalPagesCount) sampled.add(p)
            sampled.toList()
        } else {
            (0 until totalPagesCount).toList()
        }

        for (pageIdx in pagesToInspect) {
            if (!cachedPageText.containsKey(pageIdx)) {
                runCatching {
                    val textPage = handle.getTextPage(pageIdx)
                    cachedPageText[pageIdx] = textPage.text.trim()
                }
            }
        }

        // Score pages based on keywords & proximity
        val keywords = extractKeywords(normalizedQuery)

        val scoredPages = mutableListOf<Pair<Int, Float>>()

        if (isBroadQuery || keywords.isEmpty()) {
            // Pick beginning pages (first 3-4 pages)
            val selected = (0 until minOf(maxPages, totalPagesCount)).toList()
            return@withContext formatResult(selected)
        }

        for ((pageIdx, text) in cachedPageText) {
            if (text.isBlank()) continue
            val score = scorePage(text.lowercase(Locale.ROOT), normalizedQuery, keywords)
            if (score > 0f) {
                scoredPages.add(pageIdx to score)
            }
        }

        val topPages = if (scoredPages.isNotEmpty()) {
            scoredPages.sortedByDescending { it.second }
                .take(maxPages)
                .map { it.first }
                .sorted()
        } else {
            // Fallback to first few pages if no keyword match was found
            (0 until minOf(maxPages, totalPagesCount)).toList()
        }

        formatResult(topPages)
    }

    private fun scorePage(pageText: String, fullQuery: String, keywords: List<String>): Float {
        var score = 0f

        // 1. Exact phrase match bonus
        if (fullQuery.length > 3 && pageText.contains(fullQuery)) {
            score += 15f
        }

        // 2. Keyword frequency & distinct token match coverage
        var matchedKeywords = 0
        for (kw in keywords) {
            val count = countOccurrences(pageText, kw)
            if (count > 0) {
                matchedKeywords++
                score += (count * 2f).coerceAtMost(10f)
            }
        }

        // Exponential coverage bonus: if page matches 3 distinct query terms, boost significantly
        if (matchedKeywords > 1) {
            score += (matchedKeywords * 3f)
        }

        return score
    }

    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var idx = 0
        while (true) {
            idx = text.indexOf(word, idx)
            if (idx != -1) {
                count++
                idx += word.length
            } else break
        }
        return count
    }

    private fun extractKeywords(query: String): List<String> {
        return query.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && !stopWords.contains(it) }
    }

    private fun isBroadQuestion(query: String): Boolean {
        val triggers = listOf("summary", "summarize", "overview", "what is this", "what is the book", "table of contents", "main topic", "introduction")
        return triggers.any { query.contains(it) }
    }

    private fun formatResult(pages: List<Int>): RetrievedContext {
        if (pages.isEmpty()) return RetrievedContext("", emptyList())

        val sb = StringBuilder()
        sb.append("Here are relevant excerpts from the document for reference:\n\n")

        for (pageIdx in pages) {
            val text = cachedPageText[pageIdx] ?: continue
            val trimmedText = if (text.length > 2000) text.take(2000) + "..." else text
            sb.append("--- PAGE ${pageIdx + 1} ---\n")
            sb.append(trimmedText)
            sb.append("\n\n")
        }

        return RetrievedContext(
            contextPrompt = sb.toString(),
            referencedPages = pages.map { it + 1 }
        )
    }
}
