package com.krishnajeena.pdfengine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextLayoutTest {

    private var nextIndex = 0

    private fun glyph(
        char: Char,
        x: Float,
        baselineY: Float,
        width: Float = 6f,
        fontSize: Float = 12f
    ): PdfGlyph = PdfGlyph(
        charIndex = nextIndex++,
        unicode = char.code,
        bounds = PdfRect(x, baselineY - fontSize, x + width, baselineY),
        origin = PdfPoint(x, baselineY),
        fontSize = fontSize
    )

    @Test
    fun splitsWordsOnWhitespace() {
        val glyphs = listOf(
            glyph('H', 0f, 100f), glyph('i', 6f, 100f),
            glyph(' ', 12f, 100f),
            glyph('y', 18f, 100f), glyph('o', 24f, 100f)
        )
        val result = TextLayout.layout(glyphs)

        assertEquals(2, result.words.size)
        assertEquals("Hi", result.words[0].text)
        assertEquals(0, result.words[0].charStart)
        assertEquals(2, result.words[0].charEnd)
        assertEquals("yo", result.words[1].text)
        assertEquals(3, result.words[1].charStart)
        assertEquals(5, result.words[1].charEnd)
        assertEquals(1, result.lines.size)
        assertEquals(0, result.lines[0].charStart)
        assertEquals(5, result.lines[0].charEnd)
    }

    @Test
    fun splitsWordsOnHorizontalGap() {
        // No space char, but a gap wider than 0.3 * fontSize between glyphs.
        val glyphs = listOf(
            glyph('a', 0f, 100f), glyph('b', 6f, 100f),
            glyph('c', 30f, 100f), glyph('d', 36f, 100f)
        )
        val result = TextLayout.layout(glyphs)

        assertEquals(2, result.words.size)
        assertEquals("ab", result.words[0].text)
        assertEquals("cd", result.words[1].text)
    }

    @Test
    fun splitsLinesOnGeneratedLineBreaks() {
        val glyphs = listOf(
            glyph('a', 0f, 100f),
            glyph('\r', 6f, 100f, width = 0f),
            glyph('\n', 6f, 100f, width = 0f),
            glyph('b', 0f, 120f)
        )
        val result = TextLayout.layout(glyphs)

        assertEquals(2, result.lines.size)
        assertEquals(2, result.words.size)
        assertEquals(0, result.lines[0].charStart)
        assertEquals(1, result.lines[0].charEnd)
        assertEquals(3, result.lines[1].charStart)
        assertEquals(4, result.lines[1].charEnd)
    }

    @Test
    fun splitsLinesOnBaselineJumpWithoutBreakChars() {
        val glyphs = listOf(
            glyph('a', 0f, 100f), glyph('b', 6f, 100f),
            glyph('c', 0f, 120f), glyph('d', 6f, 120f)
        )
        val result = TextLayout.layout(glyphs)

        assertEquals(2, result.lines.size)
        assertEquals("ab", result.words[0].text)
        assertEquals("cd", result.words[1].text)
    }

    @Test
    fun keepsSuperscriptOnSameLine() {
        // Baseline shifted up by less than half the font size (e.g. x^2).
        val glyphs = listOf(
            glyph('x', 0f, 100f),
            glyph('2', 6f, 96f, width = 4f, fontSize = 8f)
        )
        val result = TextLayout.layout(glyphs)

        assertEquals(1, result.lines.size)
        assertEquals(1, result.words.size)
        assertEquals("x2", result.words[0].text)
    }

    @Test
    fun wordBoundsAreUnionOfGlyphBounds() {
        val glyphs = listOf(
            glyph('a', 0f, 100f), glyph('b', 6f, 100f)
        )
        val result = TextLayout.layout(glyphs)

        val bounds = result.words[0].bounds
        assertEquals(0f, bounds.left, 1e-4f)
        assertEquals(88f, bounds.top, 1e-4f)
        assertEquals(12f, bounds.right, 1e-4f)
        assertEquals(100f, bounds.bottom, 1e-4f)
    }

    @Test
    fun emptyInputYieldsEmptyLayout() {
        val result = TextLayout.layout(emptyList())
        assertEquals(0, result.words.size)
        assertEquals(0, result.lines.size)
    }
}
