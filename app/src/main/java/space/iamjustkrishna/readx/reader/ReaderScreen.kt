package space.iamjustkrishna.readx.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.krishnajeena.pdfengine.PdfDocumentHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onCloseSearch: () -> Unit = {},
    themeColors: ThemeColors = MinimalistLightColors
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
    var toolbarSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedPageWidthPts by remember { mutableFloatStateOf(612f) }

    val searchState by viewModel.search.collectAsState()
    val selectionState by viewModel.selection.collectAsState()
    val showGlyphBoxes by viewModel.showGlyphBoxes.collectAsState()
    val highlights by viewModel.highlights.collectAsState()

    // Real-time debounced search (250ms delay, cancels prior job)
    LaunchedEffect(query) {
        if (query.isBlank()) {
            viewModel.clearSearch()
        } else if (query.trim().length >= 2) {
            delay(250L)
            viewModel.runSearch(query.trim())
        }
    }

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
            delay(2_000L)
            isScrubberVisible = false
        }
    }

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

    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(currentPage) {
        viewModel.onPageVisible(currentPage, document.pageCount)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushReadingSession()
        }
    }

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var handleHeightPx by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val scrollFraction = if (document.pageCount > 1) {
        currentPage.toFloat() / (document.pageCount - 1)
    } else 0f
    val activeFraction = dragFraction ?: scrollFraction
    val travelPx = (trackHeightPx - handleHeightPx).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar: visible only when search is clicked in the TopBar
            androidx.compose.animation.AnimatedVisibility(visible = isSearchExpanded) {
                Surface(
                    color = themeColors.surface,
                    border = BorderStroke(1.dp, themeColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                placeholder = { Text("Search document in real-time...", color = themeColors.onSurfaceVariant) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = themeColors.primary
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = {
                                            query = ""
                                            viewModel.clearSearch()
                                        }) {
                                            Icon(
                                                Icons.Filled.Clear,
                                                contentDescription = "Clear search",
                                                tint = themeColors.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = themeColors.surfaceVariant,
                                    unfocusedContainerColor = themeColors.surfaceVariant,
                                    focusedTextColor = themeColors.onSurface,
                                    unfocusedTextColor = themeColors.onSurface,
                                    focusedBorderColor = themeColors.primary,
                                    unfocusedBorderColor = themeColors.cardBorder,
                                    cursorColor = themeColors.primary
                                ),
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
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (searchState.matches.isEmpty()) "No matches found"
                                    else " of  matches",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (searchState.matches.isEmpty()) themeColors.onSurfaceVariant else themeColors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (searchState.matches.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { viewModel.previousMatch() }) {
                                            Text("Prev", color = themeColors.primary, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { viewModel.nextMatch() }) {
                                            Text("Next", color = themeColors.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wrapper Box holding Zoom Container and floating Selection Overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Zoom container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColors.background)
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

                // Selection Toolbar + Inline AI Card
                val sel = selectionState
                if (sel != null && sel.quads.isNotEmpty()) {
                    Box(
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

                                val contentX = hPadPx + firstQuad.left * toPx
                                val contentY = pageLayoutInfo.offset + firstQuad.top * toPx
                                val contentBottomY = pageLayoutInfo.offset + lastQuad.bottom * toPx

                                val screenX = contentX * scale + offsetX
                                val screenY = contentY * scale + offsetY
                                val screenBottomY = contentBottomY * scale + offsetY

                                val toolbarMargin = 8.dp.toPx()
                                val overlayW = toolbarSize.width.toFloat().coerceAtLeast(1f)
                                val overlayH = toolbarSize.height.toFloat().coerceAtLeast(1f)
                                val containerW = zoomContainerSize.width.toFloat()
                                val containerH = zoomContainerSize.height.toFloat()

                                val selectionVisible = screenBottomY > 0f && screenY < containerH
                                if (!selectionVisible) return@offset IntOffset(-10000, -10000)

                                val clampedX = screenX
                                    .coerceIn(toolbarMargin, (containerW - overlayW - toolbarMargin).coerceAtLeast(toolbarMargin))

                                val aboveY = screenY - overlayH - toolbarMargin
                                val belowY = screenBottomY + toolbarMargin

                                val clampedY = if (aboveY >= toolbarMargin) {
                                    aboveY
                                } else if (belowY + overlayH <= containerH - toolbarMargin) {
                                    belowY
                                } else {
                                    aboveY.coerceAtLeast(toolbarMargin)
                                }

                                IntOffset(clampedX.roundToInt(), clampedY.roundToInt())
                            }
                            .onSizeChanged { newSize ->
                                if (newSize.width > 0 && newSize.height > 0 && newSize != toolbarSize) {
                                    toolbarSize = newSize
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
                            },
                            themeColors = themeColors
                        )
                    }

                    if (showInlineAi) {
                        val density = LocalDensity.current
                        val hPadPx = with(density) { 12.dp.toPx() }
                        val pageContentWidth = (zoomContainerSize.width - 2 * hPadPx).coerceAtLeast(1f)
                        val pageIndex = sel.selection.pageIndex
                        val pageLayoutInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == pageIndex }
                        val toPx = pageContentWidth / selectedPageWidthPts
                        val firstQuad = sel.quads.firstOrNull()
                        val contentY = (pageLayoutInfo?.offset?.toFloat() ?: 0f) + (firstQuad?.top ?: 0f) * toPx
                        val screenY = contentY * scale + offsetY
                        val isSelectionInLowerHalf = screenY > (zoomContainerSize.height.toFloat() * 0.52f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = if (isSelectionInLowerHalf) 60.dp else 12.dp,
                                    bottom = if (isSelectionInLowerHalf) 12.dp else 24.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                            contentAlignment = if (isSelectionInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showInlineAi,
                                enter = fadeIn() + slideInVertically { if (isSelectionInLowerHalf) -it / 2 else it / 2 },
                                exit = fadeOut() + slideOutVertically { if (isSelectionInLowerHalf) -it / 2 else it / 2 }
                            ) {
                                InlineAiCard(
                                    selectionState = sel,
                                    viewModel = viewModel,
                                    onClose = { showInlineAi = false },
                                    onNavigateToSettings = {
                                        showInlineAi = false
                                        onShowAiDialog("")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 440.dp),
                                    themeColors = themeColors
                                )
                            }
                        }
                    }
                }
            }
        }

        // Draggable Page Scrubber on Right Edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .fillMaxHeight(0.8f)
                .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(
                        color = themeColors.cardBorder.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrubberVisible,
                enter = fadeIn(),
                exit = fadeOut(),
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
                                    val base = dragFraction ?: (currentPage.toFloat() / (document.pageCount - 1))
                                    val newFraction = (base + dragAmount / travel).coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    val targetPage = (newFraction * (document.pageCount - 1)).roundToInt()
                                    scope.launch { listState.scrollToItem(targetPage) }
                                }
                            }
                        )
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (themeColors.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                        border = BorderStroke(1.dp, if (themeColors.isDark) Color(0xFF334155) else themeColors.cardBorder),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "${currentPage + 1} / ${document.pageCount}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (themeColors.isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp),
                        color = if (themeColors.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                        border = BorderStroke(1.dp, if (themeColors.isDark) Color(0xFF334155) else themeColors.cardBorder),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp
                            ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            SixDotGrip(
                                tint = if (isDraggingScrubber) themeColors.primary else themeColors.onSurfaceVariant
                            )
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
                            .background(tint, shape = RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}