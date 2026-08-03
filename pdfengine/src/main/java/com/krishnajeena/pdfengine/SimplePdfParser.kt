package com.krishnajeena.pdfengine

import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.max

internal data class ParsedPdfDocument(
    val pages: List<ParsedPdfPage>
)

internal class ParsedPdfPage(
    val width: Float,
    val height: Float,
    private val pageObject: PdfObject,
    private val objects: Map<Int, PdfObject>
) {
    var text: String = ""
        private set
    var words: List<PdfTextWord> = emptyList()
        private set
    var interpreted: Boolean = false
        internal set

    internal fun extractContentStream(): String {
        val refs = mutableListOf<Int>()
        Regex("""/Contents\s+(\d+)\s+\d+\s+R""").find(pageObject.body)?.let {
            refs += it.groupValues[1].toInt()
        }
        Regex("""/Contents\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(pageObject.body)?.groupValues?.get(1)?.let { array ->
            Regex("""(\d+)\s+\d+\s+R""").findAll(array).forEach {
                refs += it.groupValues[1].toInt()
            }
        }

        return refs.joinToString(separator = "\n") { ref ->
            objects[ref]?.streamText().orEmpty()
        }
    }

    internal fun applyInterpretedData(newText: String, newWords: List<PdfTextWord>) {
        text = newText
        words = newWords
        interpreted = true
    }
}

internal object SimplePdfParser {
    fun parse(bytes: ByteArray): ParsedPdfDocument {
        val source = bytes.toString(Charsets.ISO_8859_1)
        val objects = parseObjects(source)
        val pages = objects.values
            .filter { it.body.contains("/Type") && it.body.contains("/Page") && !it.body.contains("/Pages") }
            .sortedBy { it.number }
            .map { parsePageMeta(it, objects) }

        return ParsedPdfDocument(pages)
    }

    fun interpretPage(page: ParsedPdfPage) {
        if (page.interpreted) return

        val content = page.extractContentStream()
        val interpreter = ContentInterpreter(page.height)
        interpreter.interpret(content)
        
        page.applyInterpretedData(
            newText = interpreter.text.toString().trim(),
            newWords = interpreter.words
        )
    }

    private fun parseObjects(source: String): Map<Int, PdfObject> {
        val objectRegex = Regex("""(?s)(\d+)\s+(\d+)\s+obj\s*(.*?)\s*endobj""")
        return objectRegex.findAll(source).associate { match ->
            val number = match.groupValues[1].toInt()
            number to PdfObject(number, match.groupValues[3])
        }
    }

    private fun parsePageMeta(pageObject: PdfObject, objects: Map<Int, PdfObject>): ParsedPdfPage {
        val mediaBox = parseMediaBox(pageObject.body) ?: floatArrayOf(0f, 0f, 612f, 792f)
        val width = max(1f, mediaBox[2] - mediaBox[0])
        val height = max(1f, mediaBox[3] - mediaBox[1])
        return ParsedPdfPage(
            width = width,
            height = height,
            pageObject = pageObject,
            objects = objects
        )
    }

    private fun parseMediaBox(body: String): FloatArray? {
        val match = Regex("""/MediaBox\s*\[\s*([-.\d]+)\s+([-.\d]+)\s+([-.\d]+)\s+([-.\d]+)\s*]""")
            .find(body) ?: return null
        return floatArrayOf(
            match.groupValues[1].toFloat(),
            match.groupValues[2].toFloat(),
            match.groupValues[3].toFloat(),
            match.groupValues[4].toFloat()
        )
    }
}

internal data class PdfObject(
    val number: Int,
    val body: String
) {
    fun streamText(): String {
        val start = body.indexOf("stream")
        val end = body.lastIndexOf("endstream")
        if (start < 0 || end <= start) return ""

        var stream = body.substring(start + "stream".length, end)
        stream = stream.trimStart('\r', '\n').trimEnd('\r', '\n')
        return if (body.contains("/FlateDecode")) {
            runCatching {
                InflaterInputStream(ByteArrayInputStream(stream.toByteArray(Charsets.ISO_8859_1)))
                    .readBytes()
                    .toString(Charsets.ISO_8859_1)
            }.getOrDefault("")
        } else {
            stream
        }
    }
}

private class ContentInterpreter(
    private val pageHeight: Float
) {
    val text = StringBuilder()
    val words = mutableListOf<PdfTextWord>()

    // Text matrix components (per PDF spec)
    // BT initializes both Tm and Tlm to identity
    private var textX = 0f       // current text rendering X
    private var textY = 0f       // current text rendering Y
    private var lineStartX = 0f  // start of current line X (set by Td/Tm)
    private var lineStartY = 0f  // start of current line Y
    private var fontSize = 12f   // font size from Tf
    private var textLeading = 0f // leading from TL or TD
    private var tmScaleX = 1f    // horizontal scale from Tm matrix
    private var tmScaleY = 1f    // vertical scale from Tm matrix

    // Effective font size for positioning (accounts for Tm scale)
    private val effectiveFontSize: Float get() = fontSize * tmScaleY

    // Average character width as fraction of font size
    // 0.5 is a reasonable average for proportional Latin fonts
    private val charWidth: Float get() = effectiveFontSize * 0.5f

    fun interpret(content: String) {
        val tokens = tokenize(content)
        val stack = mutableListOf<PdfToken>()

        tokens.forEach { token ->
            if (token is PdfToken.Operator) {
                handleOperator(token.value, stack)
                stack.clear()
            } else {
                stack += token
            }
        }
    }

    private fun handleOperator(operator: String, stack: List<PdfToken>) {
        when (operator) {
            "BT" -> {
                // Begin text object: reset text matrix and text line matrix to identity
                textX = 0f
                textY = 0f
                lineStartX = 0f
                lineStartY = 0f
                tmScaleX = 1f
                tmScaleY = 1f
            }
            "ET" -> {
                if (text.isNotEmpty() && !text.endsWith(' ') && !text.endsWith('\n')) {
                    text.append('\n')
                }
            }
            "Tf" -> {
                // Font size is always the last numeric argument before the operator
                stack.numberAt(stack.size - 1)?.let {
                    fontSize = kotlin.math.abs(it)
                    if (fontSize < 1f) fontSize = 12f // safety
                }
            }
            "TL" -> {
                stack.numberAt(0)?.let { textLeading = it }
            }
            "Td" -> {
                // Move to start of next line, offset from start of current line
                val tx = stack.numberAt(0) ?: 0f
                val ty = stack.numberAt(1) ?: 0f
                lineStartX += tx
                lineStartY += ty
                textX = lineStartX
                textY = lineStartY
            }
            "TD" -> {
                // Same as Td but also sets leading to -ty
                val tx = stack.numberAt(0) ?: 0f
                val ty = stack.numberAt(1) ?: 0f
                textLeading = -ty
                lineStartX += tx
                lineStartY += ty
                textX = lineStartX
                textY = lineStartY
            }
            "Tm" -> {
                // Set text matrix directly: [a b c d e f]
                // a = horizontal scale, d = vertical scale, e = x translation, f = y translation
                val a = stack.numberAt(0) ?: 1f
                val d = stack.numberAt(3) ?: 1f
                val e = stack.numberAt(4) ?: textX
                val f = stack.numberAt(5) ?: textY
                tmScaleX = kotlin.math.abs(a)
                tmScaleY = kotlin.math.abs(d)
                textX = e
                textY = f
                lineStartX = e
                lineStartY = f
            }
            "T*" -> {
                // Move to start of next line (equivalent to 0 -TL Td)
                val leading = if (textLeading != 0f) textLeading else effectiveFontSize * 1.2f
                lineStartY -= leading
                textX = lineStartX
                textY = lineStartY
            }
            "Tj" -> drawText(stack.lastOrNull()?.asText().orEmpty())
            "'", "\"" -> {
                // Move to next line then show text
                val leading = if (textLeading != 0f) textLeading else effectiveFontSize * 1.2f
                lineStartY -= leading
                textX = lineStartX
                textY = lineStartY
                drawText(stack.lastOrNull()?.asText().orEmpty())
            }
            "TJ" -> {
                val sb = StringBuilder()
                stack.forEach { token ->
                    when (token) {
                        is PdfToken.Str -> {
                            sb.append(token.value)
                        }
                        is PdfToken.Value -> {
                            val num = token.value.toFloatOrNull()
                            if (num != null) {
                                if (sb.isNotEmpty()) {
                                    drawText(sb.toString(), false)
                                    sb.clear()
                                }
                                // Negative numbers move right, positive move left
                                textX -= (num / 1000f) * effectiveFontSize
                                if (num < -150) {
                                    text.append(' ')
                                }
                            }
                        }
                        else -> {}
                    }
                }
                if (sb.isNotEmpty()) {
                    drawText(sb.toString(), true)
                }
            }
        }
    }

    private fun drawText(value: String, appendTrailingSpace: Boolean = true) {
        if (value.isEmpty()) return

        val normalized = value.replace(Regex("""\s+"""), " ")
        val startInText = text.length

        text.append(normalized)
        if (appendTrailingSpace && !normalized.endsWith(" ")) text.append(' ')

        val cw = charWidth
        val wordMatches = Regex("""\S+""").findAll(normalized)
        wordMatches.forEach { match ->
            val word = match.value
            val wordStartInNormalized = match.range.first
            val wordStart = startInText + wordStartInNormalized
            val wordEnd = wordStart + word.length

            val wordWidth = word.length * cw
            val cursorX = textX + wordStartInNormalized * cw
            val efs = effectiveFontSize

            // Convert PDF coordinates (origin bottom-left) to screen coordinates (origin top-left)
            val screenTop = pageHeight - textY - efs
            val screenBottom = pageHeight - textY

            words += PdfTextWord(
                text = word,
                bounds = PdfRect(cursorX, screenTop, cursorX + wordWidth, screenBottom),
                charStart = wordStart,
                charEnd = wordEnd
            )
        }

        textX += normalized.length * cw
    }

    private fun List<PdfToken>.numberAt(index: Int): Float? = getOrNull(index)?.number()

    private fun tokenize(content: String): List<PdfToken> {
        val tokens = mutableListOf<PdfToken>()
        var index = 0

        while (index < content.length) {
            val char = content[index]
            when {
                char.isWhitespace() -> index++
                char == '%' -> {
                    while (index < content.length && content[index] != '\n') index++
                }
                char == '(' -> {
                    val result = readLiteralString(content, index)
                    tokens += PdfToken.Str(result.first)
                    index = result.second
                }
                char == '<' -> {
                    if (index + 1 < content.length && content[index + 1] == '<') {
                        index += 2
                    } else {
                        val result = readHexString(content, index)
                        tokens += PdfToken.Str(result.first)
                        index = result.second
                    }
                }
                char == '>' -> {
                    if (index + 1 < content.length && content[index + 1] == '>') {
                        index += 2
                    } else {
                        index++
                    }
                }
                char == '[' || char == ']' -> {
                    index++
                }
                else -> {
                    val start = index
                    while (index < content.length && !content[index].isWhitespace() &&
                        content[index] != '[' && content[index] != ']' &&
                        content[index] != '<' && content[index] != '>' &&
                        content[index] != '(' && content[index] != ')'
                    ) {
                        index++
                    }
                    val raw = content.substring(start, index)
                    tokens += if (raw.toFloatOrNull() != null || raw.startsWith("/")) {
                        PdfToken.Value(raw)
                    } else {
                        PdfToken.Operator(raw)
                    }
                }
            }
        }

        return tokens
    }

    private fun readHexString(content: String, start: Int): Pair<String, Int> {
        var index = start + 1
        val hexChars = StringBuilder()
        while (index < content.length && content[index] != '>') {
            val c = content[index]
            if (!c.isWhitespace()) {
                hexChars.append(c)
            }
            index++
        }
        if (index < content.length) index++ // skip '>'

        var hex = hexChars.toString()
        if (hex.length % 2 != 0) {
            hex += "0"
        }

        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until bytes.size) {
            val b = hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: 0
            bytes[i] = b.toByte()
        }
        return bytes.toString(Charsets.ISO_8859_1).normalizePdfText() to index
    }

    private fun readLiteralString(content: String, start: Int): Pair<String, Int> {
        val builder = StringBuilder()
        var depth = 1
        var index = start + 1

        while (index < content.length && depth > 0) {
            val char = content[index]
            when (char) {
                '\\' -> {
                    val next = content.getOrNull(index + 1)
                    when (next) {
                        'n' -> builder.append('\n')
                        'r' -> builder.append('\r')
                        't' -> builder.append('\t')
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000C')
                        '(', ')', '\\' -> builder.append(next)
                        else -> if (next != null) builder.append(next)
                    }
                    index += 2
                }
                '(' -> {
                    depth++
                    builder.append(char)
                    index++
                }
                ')' -> {
                    depth--
                    if (depth > 0) builder.append(char)
                    index++
                }
                else -> {
                    builder.append(char)
                    index++
                }
            }
        }

        return builder.toString().normalizePdfText() to index
    }

    private fun String.normalizePdfText(): String = replace("\u0000", "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private sealed interface PdfToken {
    data class Value(val value: String) : PdfToken
    data class Str(val value: String) : PdfToken
    data class Operator(val value: String) : PdfToken

    fun number(): Float? = when (this) {
        is Value -> value.toFloatOrNull()
        else -> null
    }

    fun asText(): String? = when (this) {
        is Str -> value
        else -> null
    }
}

