package com.krishnajeena.pdfengine

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end engine verification against real pdfium on a device/emulator.
 *
 * The central claims under test:
 *  1. pdfium renders pages (ink lands on the bitmap),
 *  2. glyph boxes from the text layer align with that ink — every glyph's
 *     box must contain dark pixels of the rendered bitmap,
 *  3. hit-testing a glyph's center returns that glyph's char index.
 */
@RunWith(AndroidJUnit4::class)
class PdfEngineInstrumentedTest {

    private lateinit var handle: PdfDocumentHandle
    private lateinit var pdfFile: File

    @Before
    fun open() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        pdfFile = File(context.cacheDir, "engine-test.pdf")
        pdfFile.writeBytes(buildMinimalPdf())
        handle = NativePdfDocumentEngine()
            .open(PdfSource.Document(Uri.fromFile(pdfFile), context, "engine-test.pdf"))
            .getOrThrow()
    }

    @After
    fun close() {
        handle.close()
        pdfFile.delete()
    }

    @Test
    fun documentOpensWithExpectedGeometry() = runBlocking {
        assertEquals(1, handle.pageCount)
        val info = handle.getPageInfo(0)
        assertEquals(612f, info.width, 0.5f)
        assertEquals(792f, info.height, 0.5f)
    }

    @Test
    fun renderProducesInk() = runBlocking {
        val page = handle.renderPage(PdfRenderRequest(0, scale = 2f))
        assertEquals(1224, page.bitmap.width)
        assertEquals(1584, page.bitmap.height)
        assertTrue("expected rendered text ink", countDarkPixels(page.bitmap) > 100)
    }

    @Test
    fun textLayerMatchesContent() = runBlocking {
        val tp = handle.getTextPage(0)
        assertTrue("text was: ${tp.text}", tp.text.contains("Hello ReadX"))
        val wordTexts = tp.words.map { it.text }
        assertTrue("words were: $wordTexts", wordTexts.containsAll(listOf("Hello", "ReadX", "Alignment")))
        assertEquals(2, tp.lines.size)
        // Glyph char indices are the shared index space and must be ordered.
        assertTrue(tp.glyphs.zipWithNext().all { (a, b) -> a.charIndex < b.charIndex })
    }

    /** The alignment proof: every visible glyph's box contains rendered ink. */
    @Test
    fun glyphBoxesContainRenderedInk() = runBlocking {
        val scale = 2f
        val bitmap = handle.renderPage(PdfRenderRequest(0, scale)).bitmap
        val tp = handle.getTextPage(0)
        val visible = tp.glyphs.filter { !Character.isWhitespace(it.unicode) && it.bounds.width > 0f }
        assertTrue("no visible glyphs parsed", visible.isNotEmpty())

        for (glyph in visible) {
            val left = (glyph.bounds.left * scale).toInt() - 1
            val top = (glyph.bounds.top * scale).toInt() - 1
            val right = (glyph.bounds.right * scale).toInt() + 1
            val bottom = (glyph.bounds.bottom * scale).toInt() + 1
            var ink = false
            outer@ for (y in top.coerceAtLeast(0)..bottom.coerceAtMost(bitmap.height - 1)) {
                for (x in left.coerceAtLeast(0)..right.coerceAtMost(bitmap.width - 1)) {
                    if (isDark(bitmap.getPixel(x, y))) {
                        ink = true
                        break@outer
                    }
                }
            }
            assertTrue(
                "glyph '${glyph.char}' (index ${glyph.charIndex}) box ${glyph.bounds} has no ink",
                ink
            )
        }
    }

    /** Hit-testing the center of every glyph box returns that glyph. */
    @Test
    fun hitTestRoundTripsEveryGlyph() = runBlocking {
        val tp = handle.getTextPage(0)
        val visible = tp.glyphs.filter { !Character.isWhitespace(it.unicode) && it.bounds.width > 0f }
        for (glyph in visible) {
            val cx = (glyph.bounds.left + glyph.bounds.right) / 2f
            val cy = (glyph.bounds.top + glyph.bounds.bottom) / 2f
            val hit = handle.hitTestGlyph(0, cx, cy, tolerancePt = 1f)
            assertEquals("hit test at center of '${glyph.char}'", glyph.charIndex, hit)
        }
    }

    @Test
    fun selectionRectsCoverSelectedWord() = runBlocking {
        val tp = handle.getTextPage(0)
        val hello = tp.words.first { it.text == "Hello" }
        val rects = handle.selectionRects(0, hello.charStart, hello.charEnd)
        assertTrue(rects.isNotEmpty())
        val union = rects.reduce { a, b ->
            PdfRect(
                minOf(a.left, b.left), minOf(a.top, b.top),
                maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)
            )
        }
        // Selection quads must cover the word's glyph-union bounds.
        assertTrue(union.left <= hello.bounds.left + 1f)
        assertTrue(union.right >= hello.bounds.right - 1f)
        assertTrue(union.top <= hello.bounds.top + 1f)
        assertTrue(union.bottom >= hello.bounds.bottom - 1f)
    }

    @Test
    fun searchFindsQueryWithBounds() = runBlocking {
        val matches = handle.search("ReadX")
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(0, match.pageIndex)
        assertTrue(match.bounds.isNotEmpty())
        assertEquals("ReadX", handle.textForRange(0, match.start, match.end))
    }

    private fun countDarkPixels(bitmap: Bitmap): Int {
        var dark = 0
        for (y in 0 until bitmap.height step 4) {
            for (x in 0 until bitmap.width step 4) {
                if (isDark(bitmap.getPixel(x, y))) dark++
            }
        }
        return dark
    }

    private fun isDark(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r + g + b < 384
    }

    /**
     * A minimal, valid, uncompressed PDF (ASCII only, so char offsets equal
     * byte offsets) with two Helvetica text lines.
     */
    private fun buildMinimalPdf(): ByteArray {
        val content =
            "BT /F1 24 Tf 72 700 Td (Hello ReadX) Tj ET\n" +
                "BT /F1 18 Tf 72 660 Td (Alignment test) Tj ET"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length ${content.length} >>\nstream\n$content\nendstream"
        )
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = IntArray(objects.size + 1)
        objects.forEachIndexed { i, body ->
            offsets[i + 1] = sb.length
            sb.append(i + 1).append(" 0 obj\n").append(body).append("\nendobj\n")
        }
        val xrefPos = sb.length
        sb.append("xref\n0 ").append(objects.size + 1).append('\n')
        sb.append("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            sb.append(String.format("%010d 00000 n \n", offsets[i]))
        }
        sb.append("trailer\n<< /Size ").append(objects.size + 1)
            .append(" /Root 1 0 R >>\nstartxref\n").append(xrefPos).append("\n%%EOF")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }
}
