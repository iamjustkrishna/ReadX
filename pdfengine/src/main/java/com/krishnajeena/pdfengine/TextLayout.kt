package com.krishnajeena.pdfengine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Groups raw pdfium glyphs into words and lines. Pure Kotlin so it is
 * unit-testable without a device.
 *
 * pdfium returns chars in reading order and inserts "\r\n" at line ends;
 * those generated chars carry no useful geometry and act only as breaks.
 */
internal object TextLayout {

    data class Result(val words: List<PdfTextWord>, val lines: List<PdfTextLine>)

    fun layout(glyphs: List<PdfGlyph>): Result {
        val words = mutableListOf<PdfTextWord>()
        val lines = mutableListOf<PdfTextLine>()

        var lineStartGlyph: PdfGlyph? = null
        var lineBounds: PdfRect? = null
        var lineLastGlyph: PdfGlyph? = null

        var wordGlyphs = mutableListOf<PdfGlyph>()

        fun flushWord() {
            if (wordGlyphs.isEmpty()) return
            val bounds = wordGlyphs
                .map { it.bounds }
                .reduce { acc, r -> acc.union(r) }
            words += PdfTextWord(
                text = buildString {
                    wordGlyphs.forEach { appendCodePoint(it.unicode) }
                },
                bounds = bounds,
                charStart = wordGlyphs.first().charIndex,
                charEnd = wordGlyphs.last().charIndex + 1
            )
            wordGlyphs = mutableListOf()
        }

        fun flushLine() {
            flushWord()
            val start = lineStartGlyph
            val last = lineLastGlyph
            val bounds = lineBounds
            if (start != null && last != null && bounds != null) {
                lines += PdfTextLine(
                    charStart = start.charIndex,
                    charEnd = last.charIndex + 1,
                    bounds = bounds
                )
            }
            lineStartGlyph = null
            lineBounds = null
            lineLastGlyph = null
        }

        for (glyph in glyphs) {
            if (glyph.isWhitespace) {
                // Whitespace (including pdfium's generated \r\n) ends the
                // current word; line breaks end the line.
                if (glyph.unicode == '\r'.code || glyph.unicode == '\n'.code) flushLine()
                else flushWord()
                continue
            }

            val prev = lineLastGlyph
            if (prev != null) {
                val fontSize = if (glyph.fontSize > 0f) glyph.fontSize else 12f
                val baselineJump = abs(glyph.origin.y - prev.origin.y) > 0.5f * fontSize
                val xRegression = glyph.bounds.left < prev.bounds.left - fontSize
                if (baselineJump || xRegression) {
                    flushLine()
                } else {
                    val gap = glyph.bounds.left - prev.bounds.right
                    if (gap > 0.3f * fontSize) flushWord()
                }
            }

            if (lineStartGlyph == null) lineStartGlyph = glyph
            lineBounds = lineBounds?.union(glyph.bounds) ?: glyph.bounds
            lineLastGlyph = glyph
            wordGlyphs.add(glyph)
        }
        flushLine()

        return Result(words, lines)
    }

    private val PdfGlyph.isWhitespace: Boolean
        get() = unicode == 0 || (unicode in 0..0x10FFFF && Character.isWhitespace(unicode)) ||
            unicode == 0x00A0

    private fun PdfRect.union(other: PdfRect): PdfRect = PdfRect(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom)
    )
}
