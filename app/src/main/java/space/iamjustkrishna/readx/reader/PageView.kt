package space.iamjustkrishna.readx.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.krishnajeena.pdfengine.PdfPageInfo
import com.krishnajeena.pdfengine.PdfRect
import com.krishnajeena.pdfengine.PdfRenderedPage
import com.krishnajeena.pdfengine.PdfSearchMatch
import com.krishnajeena.pdfengine.PdfTextPage
import space.iamjustkrishna.readx.model.Highlight

/**
 * One page: rendered bitmap + overlay. All overlay geometry comes from the
 * engine in page points; the only transform is the scalar
 * `canvasPx / pageInfo.width` (see PdfEngine.kt coordinate contract).
 */
@Composable
fun PageView(
    pageIndex: Int,
    viewModel: ReaderViewModel,
    selectionState: SelectionUiState?,
    searchMatchesOnPage: List<PdfSearchMatch>,
    activeMatchOnPage: PdfSearchMatch?,
    showGlyphBoxes: Boolean,
    currentScale: Float = 1f,
    highlightsOnPage: List<Highlight> = emptyList()
) {

    var renderedPage by remember(pageIndex) { mutableStateOf<PdfRenderedPage?>(null) }
    var error by remember(pageIndex) { mutableStateOf<String?>(null) }
    var textPage by remember(pageIndex) { mutableStateOf<PdfTextPage?>(null) }
    var pageInfo by remember(pageIndex) { mutableStateOf<PdfPageInfo?>(null) }
    var highlightRectsMap by remember(pageIndex) { mutableStateOf<Map<String, List<PdfRect>>>(emptyMap()) }

    val haptics = LocalHapticFeedback.current

    LaunchedEffect(pageIndex) {
        pageInfo = viewModel.pageInfo(pageIndex)
        viewModel.renderPage(pageIndex, scale = 2f)?.fold(
            onSuccess = { renderedPage = it },
            onFailure = { error = it.message ?: "Page render failed." }
        )
        textPage = viewModel.textPage(pageIndex)
    }

    // Load rects for highlights on this page
    LaunchedEffect(highlightsOnPage) {
        val map = mutableMapOf<String, List<PdfRect>>()
        highlightsOnPage.forEach { hl ->
            val rects = viewModel.selectionRects(hl.pageIndex, hl.startChar, hl.endChar)
            map[hl.id] = rects
        }
        highlightRectsMap = map
    }

    val pageWidthPts = pageInfo?.width ?: 612f
    val pageHeightPts = pageInfo?.height ?: 792f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageWidthPts / pageHeightPts)
                    .background(Color(0xFFECECEC))
                    .pointerInput(pageIndex, pageWidthPts) {
                        // One handler for tap-to-clear and long-press selection
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val longPress = awaitLongPressOrCancellation(down.id)
                            val toPt = pageWidthPts / size.width
                            if (longPress != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startSelection(
                                    pageIndex,
                                    longPress.position.x * toPt,
                                    longPress.position.y * toPt
                                )
                                drag(longPress.id) { change ->
                                    change.consume()
                                    viewModel.dragSelection(
                                        pageIndex,
                                        change.position.x * toPt,
                                        change.position.y * toPt
                                    )
                                }
                            } else if (
                                currentEvent.changes.none { it.pressed } &&
                                currentEvent.changes.none { it.isConsumed }
                            ) {
                                viewModel.clearSelection()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val page = renderedPage
                when {
                    page != null -> Image(
                        bitmap = page.bitmap.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth()
                    )
                    error != null -> Text(
                        text = error!!,
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }

                if (page != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val toPx = size.width / pageWidthPts

                        // 1. Draw saved Yellow Highlights
                        highlightsOnPage.forEach { hl ->
                            val rects = highlightRectsMap[hl.id] ?: emptyList()
                            val color = Color(hl.color)
                            rects.forEach { rect ->
                                drawRect(
                                    color = color.copy(alpha = 0.4f),
                                    topLeft = Offset(rect.left * toPx, rect.top * toPx),
                                    size = Size(rect.width * toPx, rect.height * toPx),
                                    blendMode = BlendMode.Multiply
                                )
                            }
                        }

                        // 2. Search matches
                        searchMatchesOnPage.forEach { match ->
                            val color = if (match == activeMatchOnPage) Color(0x80FFA500)
                            else Color(0x30FFFF00)
                            match.bounds.forEach { rect ->
                                drawRect(
                                    color = color,
                                    topLeft = Offset(rect.left * toPx, rect.top * toPx),
                                    size = Size(rect.width * toPx, rect.height * toPx)
                                )
                            }
                        }

                        // 3. Glyph boxes (debug mode)
                        if (showGlyphBoxes) {
                            textPage?.glyphs?.forEach { glyph ->
                                drawRect(
                                    color = Color(0xAAFF00AA),
                                    topLeft = Offset(glyph.bounds.left * toPx, glyph.bounds.top * toPx),
                                    size = Size(glyph.bounds.width * toPx, glyph.bounds.height * toPx),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                    }

                    if (selectionState != null) {
                        SelectionOverlay(
                            selectionState = selectionState,
                            pageWidthPts = pageWidthPts,
                            viewModel = viewModel,
                            currentScale = currentScale
                        )
                    }

                }
            }
        }
    }
}

