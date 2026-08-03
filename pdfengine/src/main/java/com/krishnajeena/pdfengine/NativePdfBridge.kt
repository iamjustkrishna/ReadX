package com.krishnajeena.pdfengine

/**
 * Raw JNI surface over pdfium (see pdfium_bridge.cpp).
 *
 * Every function here MUST be called on the [Pdfium] dispatcher thread —
 * pdfium has global mutable state and is not thread-safe, even across
 * documents. All coordinates crossing this boundary are in "API page space":
 * PDF points with a top-left origin, y growing downward.
 */
internal object NativePdfBridge {
    init {
        System.loadLibrary("readx_pdf_engine")
    }

    external fun initLibrary()
    external fun destroyLibrary()

    /** Returns a document handle, or 0 on failure (check [lastError]). */
    external fun openDocumentFd(fd: Int, length: Long, password: String?): Long
    external fun lastError(): Int
    external fun closeDocument(doc: Long)

    external fun getPageCount(doc: Long): Int

    /** Fills [out] with [widthPts, heightPts] (post-/Rotate). */
    external fun getPageSize(doc: Long, index: Int, out: FloatArray): Boolean

    external fun openPage(doc: Long, index: Int): Long
    external fun closePage(page: Long)

    /**
     * Renders a region of [page] into [bitmap] (must be ARGB_8888). The page
     * is laid out at fullWidthPx x fullHeightPx; (startX, startY) is the
     * offset of the bitmap within that layout. Full-page render: offsets 0,
     * bitmap sized fullWidthPx x fullHeightPx.
     */
    external fun renderPageRegion(
        page: Long,
        bitmap: android.graphics.Bitmap,
        startX: Int,
        startY: Int,
        fullWidthPx: Int,
        fullHeightPx: Int
    ): Boolean

    external fun loadTextPage(page: Long): Long
    external fun closeTextPage(tp: Long)
    external fun countChars(tp: Long): Int

    /**
     * Batched per-char data in one JNI call. Fills [unicodes] (n), [boxes]
     * (l,t,r,b — 4n), [origins] (baseline x,y — 2n), [fontSizes] (n).
     * Returns the number of chars written.
     */
    external fun getCharData(
        tp: Long,
        pageHeightPts: Float,
        unicodes: IntArray,
        boxes: FloatArray,
        origins: FloatArray,
        fontSizes: FloatArray
    ): Int

    /** Char index at a point, or -1 for a miss. */
    external fun charIndexAtPos(
        tp: Long,
        xPt: Float,
        yPtTopLeft: Float,
        tolX: Float,
        tolY: Float,
        pageHeightPts: Float
    ): Int

    /** Per-line merged selection rects for a char range, flat [l,t,r,b]*k. */
    external fun getRectsForRange(tp: Long, start: Int, count: Int, pageHeightPts: Float): FloatArray

    external fun getTextRange(tp: Long, start: Int, count: Int): String

    /** All matches as flat [start0, count0, start1, count1, ...]. */
    external fun findAll(tp: Long, query: String, matchCase: Boolean): IntArray
}

internal object PdfError {
    const val SUCCESS = 0
    const val UNKNOWN = 1
    const val FILE = 2
    const val FORMAT = 3
    const val PASSWORD = 4
    const val SECURITY = 5
    const val PAGE = 6
}
