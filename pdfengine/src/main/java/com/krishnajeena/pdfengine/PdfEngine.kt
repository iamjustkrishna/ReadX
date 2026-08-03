package com.krishnajeena.pdfengine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.ceil

/**
 * # Coordinate contract
 *
 * All geometry in this API ([PdfRect], [PdfPoint], [PdfGlyph] etc.) is in
 * **API page space**: PDF points, origin at the page's top-left corner, y
 * growing downward. pdfium's internal bottom-left/y-up space is converted at
 * the JNI boundary and never leaks out.
 *
 * Mapping to pixels is a single scalar:
 * - bitmap pixel space: `px = pt * scale` where `scale = bitmapWidthPx / pageInfo.width`
 * - UI canvas space:    `px = pt * (layoutWidthPx / pageInfo.width)`
 *
 * Because the rasterizer lays the page out with that exact same scalar, a
 * glyph box scaled into canvas space lands on the glyph's pixels by
 * construction.
 */
interface PdfDocumentEngine {
    suspend fun open(source: PdfSource): Result<PdfDocumentHandle>
}

sealed interface PdfSource {
    data class Document(
        val uri: Uri,
        val context: Context,
        val displayName: String,
        val password: String? = null
    ) : PdfSource
}

/** Thrown by [PdfDocumentEngine.open] when the document needs a (correct) password. */
class PdfPasswordRequiredException : Exception("This PDF requires a password.")

interface PdfDocumentHandle : Closeable {
    val pageCount: Int
    val displayName: String

    suspend fun getPageInfo(pageIndex: Int): PdfPageInfo
    suspend fun renderPage(request: PdfRenderRequest): PdfRenderedPage
    suspend fun getTextPage(pageIndex: Int): PdfTextPage
    suspend fun search(query: String): List<PdfSearchMatch>

    /** Char index of the glyph at (xPt, yPt) in API page space, or -1. */
    suspend fun hitTestGlyph(pageIndex: Int, xPt: Float, yPt: Float, tolerancePt: Float = 8f): Int

    /** Per-line merged highlight quads for [startChar, endCharExclusive). */
    suspend fun selectionRects(pageIndex: Int, startChar: Int, endCharExclusive: Int): List<PdfRect>

    /** The text for [startChar, endCharExclusive) from pdfium's own text buffer. */
    suspend fun textForRange(pageIndex: Int, startChar: Int, endCharExclusive: Int): String
}

data class PdfPageInfo(
    val pageIndex: Int,
    /** Page width in PDF points (post-/Rotate). */
    val width: Float,
    /** Page height in PDF points (post-/Rotate). */
    val height: Float,
    val rotation: Int = 0
)

data class PdfRenderRequest(
    val pageIndex: Int,
    /** Pixels per point: bitmap width = ceil(pageInfo.width * scale). */
    val scale: Float = 1f
)

data class PdfRenderedPage(
    val pageIndex: Int,
    val scale: Float,
    val bitmap: Bitmap
)

data class PdfPoint(val x: Float, val y: Float)

data class PdfRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class PdfGlyph(
    /** pdfium text-page char index — the shared index space for selection/search/copy. */
    val charIndex: Int,
    val unicode: Int,
    /** Tight glyph bounding box, API page space. */
    val bounds: PdfRect,
    /** Baseline origin, API page space. */
    val origin: PdfPoint,
    val fontSize: Float
) {
    val char: Char get() = if (unicode in 0..0xFFFF) unicode.toChar() else '�'
}

data class PdfTextWord(
    val text: String,
    val bounds: PdfRect,
    val charStart: Int = 0,
    val charEnd: Int = 0
)

data class PdfTextLine(
    val charStart: Int,
    val charEnd: Int,
    val bounds: PdfRect
)

data class PdfTextPage(
    val pageIndex: Int,
    /** pdfium's own text; string indices match glyph charIndex values. */
    val text: String,
    val glyphs: List<PdfGlyph>,
    val words: List<PdfTextWord>,
    val lines: List<PdfTextLine>
)

data class PdfSearchMatch(
    val pageIndex: Int,
    val start: Int,
    val end: Int,
    val bounds: List<PdfRect>
)

/** A normalized char-range selection on a single page (start <= end). */
data class TextSelection(
    val pageIndex: Int,
    val startChar: Int,
    val endChar: Int
) {
    val length: Int get() = endChar - startChar
}

class NativePdfDocumentEngine : PdfDocumentEngine {
    override suspend fun open(source: PdfSource): Result<PdfDocumentHandle> = runCatching {
        when (source) {
            is PdfSource.Document -> {
                val pfd = withContext(Dispatchers.IO) {
                    source.context.contentResolver.openFileDescriptor(source.uri, "r")
                } ?: error("Unable to open file descriptor for ${source.uri}")

                val docPtr = Pdfium.run {
                    NativePdfBridge.openDocumentFd(pfd.fd, pfd.statSize, source.password)
                }
                if (docPtr == 0L) {
                    val errorCode = Pdfium.run { NativePdfBridge.lastError() }
                    pfd.close()
                    when (errorCode) {
                        PdfError.PASSWORD -> throw PdfPasswordRequiredException()
                        PdfError.FORMAT -> error("This file is not a valid PDF.")
                        else -> error("Unable to open PDF (pdfium error $errorCode).")
                    }
                }
                val pageCount = Pdfium.run { NativePdfBridge.getPageCount(docPtr) }
                NativePdfDocumentHandle(
                    displayName = source.displayName,
                    docPtr = docPtr,
                    pfd = pfd,
                    pageCount = pageCount
                )
            }
        }
    }
}

private class NativePdfDocumentHandle(
    override val displayName: String,
    private val docPtr: Long,
    private val pfd: ParcelFileDescriptor,
    override val pageCount: Int
) : PdfDocumentHandle {

    @Volatile
    private var closed = false

    private val closeScope = CoroutineScope(SupervisorJob() + Pdfium.dispatcher)

    private class PageEntry(val pagePtr: Long) {
        var textPagePtr: Long = 0L
    }

    // Only touched on the pdfium thread — no extra synchronization needed.
    private val openPages = object : LinkedHashMap<Int, PageEntry>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, PageEntry>): Boolean {
            if (size <= MAX_OPEN_PAGES) return false
            closeEntry(eldest.value)
            return true
        }
    }

    // Parsed text pages are pure Kotlin data; cached so repeated selection
    // work doesn't re-run per-char extraction.
    private val textPageCache = object : LinkedHashMap<Int, PdfTextPage>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, PdfTextPage>): Boolean =
            size > MAX_CACHED_TEXT_PAGES
    }

    private fun closeEntry(entry: PageEntry) {
        if (entry.textPagePtr != 0L) NativePdfBridge.closeTextPage(entry.textPagePtr)
        if (entry.pagePtr != 0L) NativePdfBridge.closePage(entry.pagePtr)
    }

    /** Must be called on the pdfium thread (inside Pdfium.run). */
    private fun pageEntry(pageIndex: Int): PageEntry =
        openPages.getOrPut(pageIndex) { PageEntry(NativePdfBridge.openPage(docPtr, pageIndex)) }

    /** Must be called on the pdfium thread (inside Pdfium.run). */
    private fun textPagePtr(pageIndex: Int): Long {
        val entry = pageEntry(pageIndex)
        if (entry.textPagePtr == 0L) {
            entry.textPagePtr = NativePdfBridge.loadTextPage(entry.pagePtr)
        }
        return entry.textPagePtr
    }

    private fun pageHeightPts(pageIndex: Int): Float {
        val size = FloatArray(2)
        NativePdfBridge.getPageSize(docPtr, pageIndex, size)
        return size[1]
    }

    private suspend fun <T> withEngine(pageIndex: Int, block: () -> T): T {
        checkPageIndex(pageIndex)
        checkOpen()
        return Pdfium.run {
            checkOpen()
            block()
        }
    }

    override suspend fun getPageInfo(pageIndex: Int): PdfPageInfo = withEngine(pageIndex) {
        val size = FloatArray(2)
        check(NativePdfBridge.getPageSize(docPtr, pageIndex, size)) {
            "Unable to read size of page $pageIndex."
        }
        PdfPageInfo(pageIndex = pageIndex, width = size[0], height = size[1])
    }

    override suspend fun renderPage(request: PdfRenderRequest): PdfRenderedPage {
        checkPageIndex(request.pageIndex)
        checkOpen()
        // Allocate the bitmap off the pdfium thread; only the native render is confined.
        val size = Pdfium.run {
            FloatArray(2).also { NativePdfBridge.getPageSize(docPtr, request.pageIndex, it) }
        }
        val widthPx = ceil(size[0] * request.scale).toInt().coerceAtLeast(1)
        val heightPx = ceil(size[1] * request.scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

        val ok = Pdfium.run {
            checkOpen()
            val entry = pageEntry(request.pageIndex)
            NativePdfBridge.renderPageRegion(entry.pagePtr, bitmap, 0, 0, widthPx, heightPx)
        }
        check(ok) { "pdfium failed to render page ${request.pageIndex}." }
        return PdfRenderedPage(request.pageIndex, request.scale, bitmap)
    }

    override suspend fun getTextPage(pageIndex: Int): PdfTextPage = withEngine(pageIndex) {
        textPageCache[pageIndex]?.let { return@withEngine it }

        val tp = textPagePtr(pageIndex)
        val pageHeight = pageHeightPts(pageIndex)
        val count = NativePdfBridge.countChars(tp)
        if (count <= 0) {
            return@withEngine PdfTextPage(pageIndex, "", emptyList(), emptyList(), emptyList())
                .also { textPageCache[pageIndex] = it }
        }

        val unicodes = IntArray(count)
        val boxes = FloatArray(count * 4)
        val origins = FloatArray(count * 2)
        val fontSizes = FloatArray(count)
        val n = NativePdfBridge.getCharData(tp, pageHeight, unicodes, boxes, origins, fontSizes)

        val glyphs = ArrayList<PdfGlyph>(n)
        for (i in 0 until n) {
            glyphs += PdfGlyph(
                charIndex = i,
                unicode = unicodes[i],
                bounds = PdfRect(boxes[i * 4], boxes[i * 4 + 1], boxes[i * 4 + 2], boxes[i * 4 + 3]),
                origin = PdfPoint(origins[i * 2], origins[i * 2 + 1]),
                fontSize = fontSizes[i]
            )
        }
        val text = NativePdfBridge.getTextRange(tp, 0, n)
        val grouped = TextLayout.layout(glyphs)
        PdfTextPage(pageIndex, text, glyphs, grouped.words, grouped.lines)
            .also { textPageCache[pageIndex] = it }
    }

    override suspend fun search(query: String): List<PdfSearchMatch> {
        if (query.isBlank()) return emptyList()
        checkOpen()
        val matches = mutableListOf<PdfSearchMatch>()
        for (pageIndex in 0 until pageCount) {
            val pageMatches = Pdfium.run {
                if (closed) return@run emptyList()
                val tp = textPagePtr(pageIndex)
                val pageHeight = pageHeightPts(pageIndex)
                val flat = NativePdfBridge.findAll(tp, query, false)
                buildList {
                    for (m in flat.indices step 2) {
                        val start = flat[m]
                        val count = flat[m + 1]
                        val rects = NativePdfBridge.getRectsForRange(tp, start, count, pageHeight)
                        add(
                            PdfSearchMatch(
                                pageIndex = pageIndex,
                                start = start,
                                end = start + count,
                                bounds = rects.toPdfRects()
                            )
                        )
                    }
                }
            }
            matches += pageMatches
        }
        return matches
    }

    override suspend fun hitTestGlyph(
        pageIndex: Int,
        xPt: Float,
        yPt: Float,
        tolerancePt: Float
    ): Int = withEngine(pageIndex) {
        NativePdfBridge.charIndexAtPos(
            textPagePtr(pageIndex), xPt, yPt, tolerancePt, tolerancePt, pageHeightPts(pageIndex)
        )
    }

    override suspend fun selectionRects(
        pageIndex: Int,
        startChar: Int,
        endCharExclusive: Int
    ): List<PdfRect> = withEngine(pageIndex) {
        if (endCharExclusive <= startChar) return@withEngine emptyList()
        NativePdfBridge.getRectsForRange(
            textPagePtr(pageIndex), startChar, endCharExclusive - startChar, pageHeightPts(pageIndex)
        ).toPdfRects()
    }

    override suspend fun textForRange(
        pageIndex: Int,
        startChar: Int,
        endCharExclusive: Int
    ): String = withEngine(pageIndex) {
        if (endCharExclusive <= startChar) return@withEngine ""
        NativePdfBridge.getTextRange(textPagePtr(pageIndex), startChar, endCharExclusive - startChar)
    }

    override fun close() {
        if (closed) return
        closed = true
        closeScope.launch {
            openPages.values.forEach { closeEntry(it) }
            openPages.clear()
            textPageCache.clear()
            NativePdfBridge.closeDocument(docPtr)
            runCatching { pfd.close() }
        }
    }

    private fun checkOpen() {
        check(!closed) { "Document \"$displayName\" is closed." }
    }

    private fun checkPageIndex(pageIndex: Int) {
        require(pageIndex in 0 until pageCount) {
            "Page index $pageIndex is outside 0..${pageCount - 1}."
        }
    }

    private fun FloatArray.toPdfRects(): List<PdfRect> = buildList {
        for (i in 0 until this@toPdfRects.size step 4) {
            add(
                PdfRect(
                    this@toPdfRects[i],
                    this@toPdfRects[i + 1],
                    this@toPdfRects[i + 2],
                    this@toPdfRects[i + 3]
                )
            )
        }
    }

    private companion object {
        const val MAX_OPEN_PAGES = 4
        const val MAX_CACHED_TEXT_PAGES = 8
    }
}
