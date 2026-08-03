package com.krishnajeena.readx.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private val HighlightColor = Color(0x664285F4)
private val HandleColor = Color(0xE24285F4)

/**
 * Selection quads and start/end drag handles for one page.
 * Quads arrive in page points; everything is drawn with the
 * single px-per-pt scalar.
 *
 * The floating action toolbar (Copy / Highlight / AI / …) is rendered
 * separately in [ReaderScreen] as a screen-level overlay so it is never
 * clipped by the zoom container's [clipToBounds].
 */
@Composable
fun SelectionOverlay(
    selectionState: SelectionUiState,
    pageWidthPts: Float,
    viewModel: ReaderViewModel,
    currentScale: Float = 1f
) {
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
                    val hSizePx = (HANDLE_SIZE_DP * counterScale).dp.toPx()
                    val startCenter = Offset(
                        first.left * dragScale - hSizePx / 2f,
                        first.bottom * dragScale + hSizePx / 2f
                    )
                    val endCenter = Offset(
                        last.right * dragScale + hSizePx / 2f,
                        last.bottom * dragScale + hSizePx / 2f
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
                val hSizePx = (HANDLE_SIZE_DP * counterScale).dp.toPx()
                val handleSize = Size(hSizePx, hSizePx)
                val cornerR = CornerRadius(13.dp.toPx() * counterScale, 13.dp.toPx() * counterScale)

                val first = quads.first()
                val last = quads.last()

                // ── START HANDLE (3 rounded corners, sharp top-right corner at selection start) ──
                val startX = first.left * scale
                val startBottomY = first.bottom * scale
                val startSquareTopLeft = Offset(startX - handleSize.width, startBottomY)

                val startPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(startSquareTopLeft, handleSize),
                            topLeft = cornerR,
                            topRight = CornerRadius.Zero, // sharp top-right corner touching selection start
                            bottomRight = cornerR,
                            bottomLeft = cornerR
                        )
                    )
                }
                drawPath(startPath, color = HandleColor)

                // ── END HANDLE (3 rounded corners, sharp top-left corner at selection end) ──
                val endX = last.right * scale
                val endBottomY = last.bottom * scale
                val endSquareTopLeft = Offset(endX, endBottomY)

                val endPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(endSquareTopLeft, handleSize),
                            topLeft = CornerRadius.Zero, // sharp top-left corner touching selection end
                            topRight = cornerR,
                            bottomRight = cornerR,
                            bottomLeft = cornerR
                        )
                    )
                }
                drawPath(endPath, color = HandleColor)
            }
        }
    }
}

/**
 * The floating action toolbar shown above or below a text selection.
 * Rendered as a screen-level overlay in [ReaderScreen], outside the zoom
 * container, so it is never clipped.
 */
@Composable
fun SelectionToolbar(
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

private const val HANDLE_SIZE_DP = 21f

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 380,
    heightDp = 180,
    name = "Square Selection Handles Preview"
)
@Composable
private fun SelectionHandlesPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Sample text showing square selection handles in Android Studio preview mode.",
                style = MaterialTheme.typography.bodyLarge
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val handleSize = Size(14.dp.toPx(), 14.dp.toPx())

                val startQuadTopLeft = Offset(6.dp.toPx(), 2.dp.toPx())
                val startQuadSize = Size(185.dp.toPx(), 24.dp.toPx())

                val endQuadTopLeft = Offset(6.dp.toPx(), 30.dp.toPx())
                val endQuadSize = Size(150.dp.toPx(), 24.dp.toPx())

                // Draw highlighted text selection background
                drawRect(color = HighlightColor, topLeft = startQuadTopLeft, size = startQuadSize, blendMode = BlendMode.Multiply)
                drawRect(color = HighlightColor, topLeft = endQuadTopLeft, size = endQuadSize, blendMode = BlendMode.Multiply)

                // ── START HANDLE (3 rounded corners, sharp top-right corner at selection start point) ──
                val startX = startQuadTopLeft.x
                val startBottomY = startQuadTopLeft.y + startQuadSize.height
                val startSquareTopLeft = Offset(startX - handleSize.width, startBottomY)

                val cornerR = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                val startPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(startSquareTopLeft, handleSize),
                            topLeft = cornerR,
                            topRight = CornerRadius.Zero, // sharp top-right corner at selection point
                            bottomRight = cornerR,
                            bottomLeft = cornerR
                        )
                    )
                }
                drawPath(startPath, color = HandleColor)

                // ── END HANDLE (3 rounded corners, sharp top-left corner at selection end point) ──
                val endX = endQuadTopLeft.x + endQuadSize.width
                val endBottomY = endQuadTopLeft.y + endQuadSize.height
                val endSquareTopLeft = Offset(endX, endBottomY)

                val endPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(endSquareTopLeft, handleSize),
                            topLeft = CornerRadius.Zero, // sharp top-left corner at selection point
                            topRight = cornerR,
                            bottomRight = cornerR,
                            bottomLeft = cornerR
                        )
                    )
                }
                drawPath(endPath, color = HandleColor)
            }
        }
    }
}
