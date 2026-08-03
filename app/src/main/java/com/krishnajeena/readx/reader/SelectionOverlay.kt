package com.krishnajeena.readx.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val HighlightColor = Color(0x664285F4)
private val HandleColor = Color(0xFF4285F4)

/**
 * Selection quads, start/end drag handles, and the floating action toolbar
 * for one page. Quads arrive in page points; everything is drawn with the
 * single px-per-pt scalar.
 */
@Composable
fun SelectionOverlay(
    selectionState: SelectionUiState,
    pageWidthPts: Float,
    viewModel: ReaderViewModel,
    onCopy: (String) -> Unit,
    currentScale: Float = 1f,
    onShowAiDialog: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val toPx = if (boxSize.width > 0) boxSize.width / pageWidthPts else 0f

    val quads = selectionState.quads
    val currentQuads = rememberUpdatedState(quads)
    val currentToPx = rememberUpdatedState(toPx)

    val counterScale = if (currentScale > 0f) 1f / currentScale else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                // Handle dragging: consume only gestures that start on a handle.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val dragScale = currentToPx.value
                    val qs = currentQuads.value
                    if (dragScale <= 0f || qs.isEmpty()) return@awaitEachGesture

                    val touchRadius = 28.dp.toPx()
                    val first = qs.first()
                    val last = qs.last()
                    val startCenter = Offset(
                        first.left * dragScale,
                        first.bottom * dragScale + HANDLE_RADIUS_DP.dp.toPx()
                    )
                    val endCenter = Offset(
                        last.right * dragScale,
                        last.bottom * dragScale + HANDLE_RADIUS_DP.dp.toPx()
                    )
                    val dStart = (down.position - startCenter).getDistanceSquared()
                    val dEnd = (down.position - endCenter).getDistanceSquared()
                    val r2 = touchRadius * touchRadius
                    val movingStart = when {
                        dStart > r2 && dEnd > r2 -> return@awaitEachGesture
                        else -> dStart <= dEnd
                    }
                    down.consume()
                    val aimAbove = 16.dp.toPx()
                    drag(down.id) { change ->
                        change.consume()
                        viewModel.dragHandle(
                            movingStart = movingStart,
                            xPt = change.position.x / dragScale,
                            yPt = (change.position.y - aimAbove) / dragScale
                        )
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.width / pageWidthPts
            quads.forEach { quad ->
                drawRect(
                    color = HighlightColor,
                    topLeft = Offset(quad.left * scale, quad.top * scale),
                    size = Size(quad.width * scale, quad.height * scale),
                    blendMode = BlendMode.Multiply
                )
            }
            if (quads.isNotEmpty()) {
                val r = (HANDLE_RADIUS_DP * counterScale).dp.toPx()
                val first = quads.first()
                val last = quads.last()

                val c1 = Offset(first.left * scale, first.bottom * scale + r)
                val c2 = Offset(last.right * scale, last.bottom * scale + r)
                val shadowOffset = Offset(0f, 2.dp.toPx())

                // Start Handle (perky halo + primary circle + white inner core)
                drawCircle(color = Color(0x40000000), radius = r + 3.dp.toPx(), center = c1 + shadowOffset)
                drawCircle(color = HandleColor, radius = r, center = c1)
                drawCircle(color = Color.White, radius = r * 0.45f, center = c1)

                // End Handle (perky halo + primary circle + white inner core)
                drawCircle(color = Color(0x40000000), radius = r + 3.dp.toPx(), center = c2 + shadowOffset)
                drawCircle(color = HandleColor, radius = r, center = c2)
                drawCircle(color = Color.White, radius = r * 0.45f, center = c2)
            }
        }

        var showInlineAi by remember { mutableStateOf(false) }

        if (toPx > 0f && quads.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .offset {
                        val margin = 12.dp.toPx()
                        val edge = 8.dp.toPx()
                        val firstQuad = quads.first()

                        val startX = firstQuad.left * toPx
                        val startY = firstQuad.top * toPx

                        val maxX = (boxSize.width - 280.dp.toPx() - edge).coerceAtLeast(edge)
                        val x = (startX - 20.dp.toPx()).coerceIn(edge, maxX)

                        val desiredY = startY - 48.dp.toPx() - margin
                        val y = if (desiredY >= edge) desiredY
                        else (quads.last().bottom * toPx + margin).coerceIn(edge, (boxSize.height - 100.dp.toPx()).coerceAtLeast(edge))

                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                    .graphicsLayer {
                        scaleX = counterScale
                        scaleY = counterScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                SelectionToolbar(
                    selectionState = selectionState,
                    boxSize = boxSize,
                    toPx = toPx,
                    counterScale = 1f,
                    onCopy = {
                        val selection = selectionState.selection
                        scope.launch {
                            onCopy(viewModel.textFor(selection))
                            viewModel.clearSelection()
                        }
                    },
                    onHighlight = {
                        viewModel.highlightSelection()
                    },
                    onAi = {
                        showInlineAi = !showInlineAi
                    },
                    onSelectAll = { viewModel.selectAllOnPage(selectionState.selection.pageIndex) },
                    onCancel = {
                        showInlineAi = false
                        viewModel.clearSelection()
                    }
                )

                if (showInlineAi) {
                    InlineAiCard(
                        selectionState = selectionState,
                        viewModel = viewModel,
                        onClose = { showInlineAi = false },
                        onNavigateToSettings = {
                            showInlineAi = false
                            onShowAiDialog("")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    selectionState: SelectionUiState,
    boxSize: IntSize,
    toPx: Float,
    counterScale: Float,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onAi: () -> Unit,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Copy",
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            TextButton(
                onClick = onHighlight,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Highlight 📌",
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            TextButton(
                onClick = onAi,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI ✨",
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select all") },
                        onClick = {
                            menuExpanded = false
                            onSelectAll()
                        }
                    )
                }
            }

            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel selection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private const val HANDLE_RADIUS_DP = 11f


