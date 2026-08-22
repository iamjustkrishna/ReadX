package space.iamjustkrishna.readx.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

private val HighlightColor = Color(0x664285F4)
private val HandleColor = Color(0xE24285F4)

@Composable
fun SelectionOverlay(
    selectionState: SelectionUiState,
    pageWidthPts: Float,
    viewModel: ReaderViewModel,
    currentScale: Float = 1f
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val currentOnStartDrag by rememberUpdatedState<(Float, Float) -> Unit> { x, y ->
        val toPt = pageWidthPts / canvasSize.width.coerceAtLeast(1)
        viewModel.dragHandle(movingStart = true, xPt = x * toPt, yPt = y * toPt)
    }
    val currentOnEndDrag by rememberUpdatedState<(Float, Float) -> Unit> { x, y ->
        val toPt = pageWidthPts / canvasSize.width.coerceAtLeast(1)
        viewModel.dragHandle(movingStart = false, xPt = x * toPt, yPt = y * toPt)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f) return@Canvas
            val toPx = size.width / pageWidthPts

            val path = Path()
            for (quad in selectionState.quads) {
                path.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = quad.left * toPx,
                            top = quad.top * toPx,
                            right = quad.right * toPx,
                            bottom = quad.bottom * toPx
                        ),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                )
            }
            drawPath(path = path, color = HighlightColor, blendMode = BlendMode.SrcOver)

            val quads = selectionState.quads
            if (quads.isNotEmpty()) {
                val first = quads.first()
                val last = quads.last()
                val radius = 7.dp.toPx()

                drawCircle(
                    color = HandleColor,
                    radius = radius,
                    center = Offset(first.left * toPx, first.top * toPx)
                )

                drawCircle(
                    color = HandleColor,
                    radius = radius,
                    center = Offset(last.right * toPx, last.bottom * toPx)
                )
            }
        }

        val quads = selectionState.quads
        if (quads.isNotEmpty() && canvasSize.width > 0) {
            val toPx = canvasSize.width.toFloat() / pageWidthPts
            val hitPad = 18.dp

            val first = quads.first()
            val startCenter = Offset(first.left * toPx, first.top * toPx)
            Box(
                modifier = Modifier
                    .offset(
                        x = (startCenter.x / currentScale).dp - hitPad,
                        y = (startCenter.y / currentScale).dp - hitPad
                    )
                    .size(hitPad * 2)
                    .pointerInput(currentScale) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var totalOffset = Offset.Zero
                            drag(down.id) { change ->
                                change.consume()
                                totalOffset += change.position
                                currentOnStartDrag(
                                    startCenter.x + totalOffset.x * currentScale,
                                    startCenter.y + totalOffset.y * currentScale
                                )
                            }
                        }
                    }
            )

            val last = quads.last()
            val endCenter = Offset(last.right * toPx, last.bottom * toPx)
            Box(
                modifier = Modifier
                    .offset(
                        x = (endCenter.x / currentScale).dp - hitPad,
                        y = (endCenter.y / currentScale).dp - hitPad
                    )
                    .size(hitPad * 2)
                    .pointerInput(currentScale) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var totalOffset = Offset.Zero
                            drag(down.id) { change ->
                                change.consume()
                                totalOffset += change.position
                                currentOnEndDrag(
                                    endCenter.x + totalOffset.x * currentScale,
                                    endCenter.y + totalOffset.y * currentScale
                                )
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onAi: () -> Unit,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = themeColors.surface,
        border = BorderStroke(1.dp, themeColors.cardBorder),
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
                    color = themeColors.primary,
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
                    text = "Highlight",
                    color = themeColors.primary,
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
                    text = "AI Assistant",
                    color = themeColors.primary,
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
                        tint = themeColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(themeColors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Select All", color = themeColors.onSurface) },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurface),
                        onClick = {
                            menuExpanded = false
                            onSelectAll()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cancel", color = themeColors.onSurfaceVariant) },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurfaceVariant),
                        onClick = {
                            menuExpanded = false
                            onCancel()
                        }
                    )
                }
            }

            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss selection",
                    tint = themeColors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}