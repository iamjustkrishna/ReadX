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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var isScrubberVisible by remember { mutableStateOf(false) }
    var isReaderTouching by remember { mutableStateOf(false) }
    var showInlineAi by remember { mutableStateOf(false) }
    var zoomContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var selectedPageWidthPts by remember { mutableFloatStateOf(612f) }

    val searchState by viewModel.search.collectAsState()
    val selectionState by viewModel.selection.collectAsState()
    val showGlyphBoxes by viewModel.showGlyphBoxes.collectAsState()
    val highlights by viewModel.highlights.collectAsState()

    // Reset inline AI state when selection is cleared
    LaunchedEffect(selectionState) {
        if (selectionState == null) showInlineAi = false
    }

    // Fetch page width in points for selection overlay position mapping
    val selPageIndex = selectionState?.selection?.pageIndex
    LaunchedEffect(selPageIndex) {
        if (selPageIndex != null) {
            val info = viewModel.pageInfo(selPageIndex)
            if (info != null) selectedPageWidthPts = info.width
        }
    }

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

    LaunchedEffect(isScrubberVisible, isDraggingScrubber, isReaderTouching, listState.isScrollInProgress) {
        if (isScrubberVisible && !isDraggingScrubber && !isReaderTouching && !listState.isScrollInProgress) {
            kotlinx.coroutines.delay(2_000L)
            isScrubberVisible = false
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

    // Scrubber state, hoisted here because the scrubber is now rendered as a sibling
    // overlay outside the padded Column and outside the zoom container below — so it
    // sticks to the true screen edge and isn't affected by pinch-zoom scaling/panning.
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var handleHeightPx by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val scrollFraction = if (document.pageCount > 1) {
        currentPage.toFloat() / (document.pageCount - 1)
    } else 0f
    val activeFraction = dragFraction ?: scrollFraction
    val travelPx = (trackHeightPx - handleHeightPx).coerceAtLeast(0f)

    Box(modifier = Modifier.fillMaxSize()) {
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
            }            // Wrapper Box holding Zoom Container and floating Selection Overlay
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Zoom container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReaderBackground)
                        .clipToBounds()
                        .onSizeChanged { zoomContainerSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isScrubberVisible = true
                                    isReaderTouching = true
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        isReaderTouching = false
                                    }
                                },
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
                        userScrollEnabled = true,
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
                                currentScale = scale,
                                highlightsOnPage = highlights.filter { it.pageIndex == pageIndex }
                            )
                        }
                    }
                }
                // ── Selection Toolbar + Inline AI card ──────────────────────────
                // Rendered as a sibling overlay OUTSIDE the zoom container but
                // INSIDE the same wrapper Box so their top-left origins match 1:1.
                // Position calculations are performed inside the .offset { } lambda
                // (Layout phase) so layout changes don't trigger recomposition.
                val sel = selectionState  // local capture for smart-cast
                if (sel != null && sel.quads.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .offset {
                                val quads = sel.quads
                                if (quads.isEmpty()) return@offset IntOffset(-10000, -10000)

                                val firstQuad = quads.first()
                                val lastQuad = quads.last()

                                val hPadPx = 12.dp.toPx()
                                val pageContentWidth = (zoomContainerSize.width - 2 * hPadPx).coerceAtLeast(1f)

                                val pageIndex = sel.selection.pageIndex
                                val pageLayoutInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == pageIndex }
                                    ?: return@offset IntOffset(-10000, -10000)

                                val toPx = pageContentWidth / selectedPageWidthPts

                                // Content-pixel position within zoom container (before zoom transform)
                                val contentX = hPadPx + firstQuad.left * toPx
                                val contentY = pageLayoutInfo.offset + firstQuad.top * toPx
                                val contentBottomY = pageLayoutInfo.offset + lastQuad.bottom * toPx

                                // Map through zoom transform (origin 0,0)
                                val screenX = contentX * scale + offsetX
                                val screenY = contentY * scale + offsetY
                                val screenBottomY = contentBottomY * scale + offsetY

                                val toolbarMargin = 8.dp.toPx()
                                val scaleFactor = 0.92f
                                val overlayW = (overlaySize.width.toFloat() * scaleFactor).coerceAtLeast(1f)
                                val overlayH = (overlaySize.height.toFloat() * scaleFactor).coerceAtLeast(1f)
                                val containerW = zoomContainerSize.width.toFloat()
                                val containerH = zoomContainerSize.height.toFloat()

                                // Hide if selection is scrolled completely off-screen
                                val selectionVisible = screenBottomY > 0f && screenY < containerH
                                if (!selectionVisible) return@offset IntOffset(-10000, -10000)

                                // X: align with selection start, clamped to viewport
                                val clampedX = screenX
                                    .coerceIn(toolbarMargin, (containerW - overlayW - toolbarMargin).coerceAtLeast(toolbarMargin))

                                // Y: prefer above selection; if no room, place below
                                val aboveY = screenY - overlayH - toolbarMargin
                                val belowY = screenBottomY + toolbarMargin

                                val fitsAbove = aboveY >= toolbarMargin
                                val fitsBelow = belowY + overlayH <= containerH - toolbarMargin

                                val clampedY = if (fitsAbove) {
                                    aboveY
                                } else if (fitsBelow) {
                                    belowY
                                } else {
                                    // Doesn't perfectly fit above or below. 
                                    // Avoid overlapping the text by placing it on the side with more space.
                                    // It will extend off-screen, and the user can pan/scroll to see it.
                                    if (screenY > containerH - screenBottomY) aboveY else belowY
                                }

                                IntOffset(clampedX.roundToInt(), clampedY.roundToInt())
                            }
                            .graphicsLayer {
                                scaleX = 0.92f
                                scaleY = 0.92f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .onSizeChanged { newSize ->
                                if (newSize.width > 0 && newSize.height > 0 && newSize != overlaySize) {
                                    overlaySize = newSize
                                }
                            }
                    ) {
                        SelectionToolbar(
                            onCopy = {
                                val selection = sel.selection
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
                            onSelectAll = { viewModel.selectAllOnPage(sel.selection.pageIndex) },
                            onCancel = {
                                showInlineAi = false
                                viewModel.clearSelection()
                            }
                        )

                        if (showInlineAi) {
                            InlineAiCard(
                                selectionState = sel,
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

        // Interactive Draggable Page Scrubber on Right Edge.
        // Sibling of the padded Column above (outside its .padding and outside the zoom
        // container's graphicsLayer), so it sticks to the true screen edge and never
        // scales/pans along with pinch-zoom.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .fillMaxHeight(0.8f)
                .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
        ) {
            // Decorative track line, same styling as the preview.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrubberVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .onGloballyPositioned { handleHeightPx = it.size.height.toFloat() }
                    .offset { IntOffset(0, (activeFraction * travelPx).roundToInt()) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.pointerInput(document.pageCount) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isScrubberVisible = true
                                isDraggingScrubber = true
                                dragFraction = if (document.pageCount > 1) {
                                    currentPage.toFloat() / (document.pageCount - 1)
                                } else 0f
                            },
                            onDragEnd = {
                                isDraggingScrubber = false
                                dragFraction = null
                            },
                            onDragCancel = {
                                isDraggingScrubber = false
                                dragFraction = null
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val travel = (trackHeightPx - handleHeightPx).coerceAtLeast(0f)
                                if (travel > 0f && document.pageCount > 1) {
                                    val base = dragFraction
                                        ?: (currentPage.toFloat() / (document.pageCount - 1))
                                    val newFraction = (base + dragAmount / travel).coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    val targetPage =
                                        (newFraction * (document.pageCount - 1)).roundToInt()
                                    scope.launch { listState.scrollToItem(targetPage) }
                                }
                            }
                        )
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "${currentPage + 1}/${document.pageCount}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp
                            ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            SixDotGrip(
                                tint = if (isDraggingScrubber) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
    name = "Reader viewport"
)
@Composable
private fun ReaderViewportPreview() {
    MaterialTheme {
        Surface(color = ReaderBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReaderBackground)
                    .clipToBounds()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) { pageIndex ->
                        PreviewPageCard(pageNumber = pageIndex + 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.95f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = "4/2683",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(start = 12.dp, top = 12.dp,
                                    bottom = 12.dp, end = 6.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                SixDotGrip(
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }


                    }
                }
            }
        }
    }
}

@Composable
private fun SixDotGrip(tint: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(tint, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPageCard(pageNumber: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Page $pageNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This preview mirrors the zoom container layout used by the reader screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFECECEC), RoundedCornerShape(10.dp))
            )
        }
    }
}