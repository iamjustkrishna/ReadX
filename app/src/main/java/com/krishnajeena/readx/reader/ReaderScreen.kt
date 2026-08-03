package com.krishnajeena.readx.reader

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight



import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.krishnajeena.pdfengine.PdfDocumentHandle
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ReaderBackground = Color(0xFFE6E8EB)
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

@Composable
fun ReaderScreen(
    padding: PaddingValues,
    document: PdfDocumentHandle,
    viewModel: ReaderViewModel,
    onCopy: (String) -> Unit,
    onShowAiDialog: (String) -> Unit = {},
    isSearchExpanded: Boolean = false,
    onCloseSearch: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var query by remember { mutableStateOf("") }

    val searchState by viewModel.search.collectAsState()
    val selectionState by viewModel.selection.collectAsState()
    val showGlyphBoxes by viewModel.showGlyphBoxes.collectAsState()
    val highlights by viewModel.highlights.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val jumpToPage by viewModel.jumpToPage.collectAsState()

    LaunchedEffect(searchState.activeIndex) {
        searchState.activeMatch?.let { listState.animateScrollToItem(it.pageIndex) }
    }

    LaunchedEffect(jumpToPage) {
        jumpToPage?.let { pageIndex ->
            listState.animateScrollToItem(pageIndex)
            viewModel.clearJumpToPage()
        }
    }


    // Animates scale/offset to a target; used by double-tap zoom.
    suspend fun animateZoom(targetScale: Float, targetX: Float, targetY: Float) {
        val fromScale = scale
        val fromX = offsetX
        val fromY = offsetY
        animate(0f, 1f, animationSpec = tween(durationMillis = 220)) { t, _ ->
            scale = lerp(fromScale, targetScale, t)
            offsetX = lerp(fromX, targetX, t)
            offsetY = lerp(fromY, targetY, t)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Search bar: visible only when search is clicked in the TopBar
        androidx.compose.animation.AnimatedVisibility(visible = isSearchExpanded) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.clearSearch()
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Search in document") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        query = ""
                                        viewModel.clearSearch()
                                    }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                                IconButton(onClick = {
                                    query = ""
                                    viewModel.clearSearch()
                                    onCloseSearch()
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.runSearch(query)
                            focusManager.clearFocus()
                        })
                    )
                }

                if (searchState.ran) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchState.matches.isEmpty()) "No matches"
                            else "${searchState.activeIndex + 1} of ${searchState.matches.size} matches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchState.matches.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.previousMatch() }) { Text("Prev") }
                                TextButton(onClick = { viewModel.nextMatch() }) { Text("Next") }
                            }
                        }
                    }
                }
            }
        }

        // Zoom container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReaderBackground)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            scope.launch {
                                if (scale < 1.5f) {
                                    val k = DOUBLE_TAP_SCALE / scale
                                    val tx = (tap.x - (tap.x - offsetX) * k)
                                        .coerceIn(size.width * (1f - DOUBLE_TAP_SCALE), 0f)
                                    val ty = (tap.y - (tap.y - offsetY) * k)
                                        .coerceIn(size.height * (1f - DOUBLE_TAP_SCALE), 0f)
                                    animateZoom(DOUBLE_TAP_SCALE, tx, ty)
                                } else {
                                    animateZoom(1f, 0f, 0f)
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var isPanning = false
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }

                            if (pointerCount >= 2) {
                                val zoom = event.calculateZoom()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val pan = event.calculatePan()
                                if (zoom != 1f || pan != Offset.Zero) {
                                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                    val k = newScale / scale
                                    offsetX = centroid.x - (centroid.x - offsetX) * k + pan.x
                                    offsetY = centroid.y - (centroid.y - offsetY) * k + pan.y
                                    scale = newScale
                                    offsetX = offsetX.coerceIn(size.width * (1f - scale), 0f)
                                    offsetY = offsetY.coerceIn(size.height * (1f - scale), 0f)
                                    event.changes.forEach { it.consume() }
                                    isPanning = true
                                }
                            } else if (pointerCount == 1 && scale > 1.01f) {
                                val change = event.changes.firstOrNull { it.pressed } ?: continue
                                val delta = change.positionChange()

                                if (!isPanning && (abs(delta.x) > 1f || abs(delta.y) > 1f)) {
                                    isPanning = true
                                }

                                if (isPanning) {
                                    val minX = size.width * (1f - scale)
                                    val minY = size.height * (1f - scale)

                                    val newX = (offsetX + delta.x).coerceIn(minX, 0f)
                                    val newY = (offsetY + delta.y).coerceIn(minY, 0f)

                                    val consumedX = newX != offsetX
                                    val consumedY = newY != offsetY

                                    if (consumedX || consumedY) {
                                        offsetX = newX
                                        offsetY = newY
                                        if (consumedX) {
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items((0 until document.pageCount).toList()) { pageIndex ->
                    PageView(
                        pageIndex = pageIndex,
                        viewModel = viewModel,
                        selectionState = selectionState?.takeIf { it.selection.pageIndex == pageIndex },
                        searchMatchesOnPage = searchState.matches.filter { it.pageIndex == pageIndex },
                        activeMatchOnPage = searchState.activeMatch?.takeIf { it.pageIndex == pageIndex },
                        showGlyphBoxes = showGlyphBoxes,
                        onCopy = onCopy,
                        currentScale = scale,
                        highlightsOnPage = highlights.filter { it.pageIndex == pageIndex },
                        onShowAiDialog = onShowAiDialog
                    )
                }
            }

            // Interactive Draggable Page Scrubber on Right Edge
            val currentPage by remember {
                derivedStateOf { listState.firstVisibleItemIndex }
            }
            var isDraggingScrubber by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .fillMaxHeight(0.6f)
                    .pointerInput(document.pageCount) {
                        detectVerticalDragGestures(
                            onDragStart = { isDraggingScrubber = true },
                            onDragEnd = { isDraggingScrubber = false },
                            onDragCancel = { isDraggingScrubber = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val totalHeight = this@pointerInput.size.height.toFloat()
                                if (totalHeight > 0f && document.pageCount > 1) {

                                    val touchY = change.position.y.coerceIn(0f, totalHeight)
                                    val fraction = touchY / totalHeight
                                    val targetPage = (fraction * (document.pageCount - 1)).roundToInt()
                                    scope.launch { listState.scrollToItem(targetPage) }
                                }
                            }
                        )
                    }
            ) {
                // Background Track Line
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(
                            color = if (isDraggingScrubber) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                // Draggable Scrubber Pill Handle
                val handleOffsetFraction = if (document.pageCount > 1) {
                    currentPage.toFloat() / (document.pageCount - 1)
                } else 0f

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = with(LocalDensity.current) {
                            (handleOffsetFraction * 200.dp.toPx()).dp
                        }),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDraggingScrubber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = if (isDraggingScrubber) 8.dp else 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Drag to scroll pages",
                            tint = if (isDraggingScrubber) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Page ${currentPage + 1} / ${document.pageCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDraggingScrubber) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}



