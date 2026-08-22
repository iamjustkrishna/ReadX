package space.iamjustkrishna.readx.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.reader.RecentDocument
import space.iamjustkrishna.readx.data.ScannedPdf
import space.iamjustkrishna.readx.model.Highlight
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 1. Recent Documents Sheet
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDocumentsSheet(
    sheetState: SheetState,
    recentList: List<RecentDocument> = emptyList(),
    scannedPdfs: List<ScannedPdf> = emptyList(),
    favoritedUris: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onSelectDocument: (Uri) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    val displayList = remember(recentList, scannedPdfs) {
        if (recentList.isNotEmpty()) recentList
        else scannedPdfs.map { RecentDocument(it.uri, it.title) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = themeColors.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Recent Documents",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent documents found", color = themeColors.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val list = displayList.take(15)
                    items(list.size) { idx ->
                        val item = list[idx]
                        val uriStr = item.uri.toString()
                        val isFav = favoritedUris.contains(uriStr)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectDocument(item.uri)
                                },
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PdfCoverThumbnail(
                                        uri = item.uri,
                                        title = item.displayName,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier.size(40.dp, 52.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Opened recently",
                                            fontSize = 12.sp,
                                            color = themeColors.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onToggleFavorite(uriStr) }) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) Color(0xFFEF4444) else themeColors.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        onDismiss()
                                        onSelectDocument(item.uri)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Open",
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 2. AI Chat PDF Selection Sheet (Matching Image 1 Flow)
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatPdfPickerSheet(
    sheetState: SheetState,
    scannedPdfs: List<ScannedPdf>,
    recentList: List<space.iamjustkrishna.readx.reader.RecentDocument> = emptyList(),
    onSelectPdfForAi: (Uri, String) -> Unit,
    onOpenFilePicker: () -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = themeColors.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Select Document for AI Chat",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose a document from your device or recent list to start an interactive AI discussion.",
                fontSize = 13.sp,
                color = themeColors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Open File Picker primary button
            Button(
                onClick = {
                    onDismiss()
                    onOpenFilePicker()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.onPrimary)
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = themeColors.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Open File Picker", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = themeColors.onPrimary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent Documents",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Combine recents with scanned fallback
            val displayPdfs: List<Pair<Uri, String>> = if (recentList.isNotEmpty()) {
                recentList.map { it.uri to it.displayName }
            } else {
                scannedPdfs.take(8).map { it.uri to it.title }
            }

            if (displayPdfs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent PDFs found on device.",
                        fontSize = 13.sp,
                        color = themeColors.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayPdfs.size) { idx ->
                        val (docUri, docTitle) = displayPdfs[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectPdfForAi(docUri, docTitle)
                                },
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(themeColors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(docTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("PDF Document", fontSize = 12.sp, color = themeColors.onSurfaceVariant)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSelectPdfForAi(docUri, docTitle)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.onPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Start AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 3. Smart Notes Bottom Sheet (Real Highlights & Notes, No Mocks)
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNotesBottomSheet(
    sheetState: SheetState,
    highlights: List<Highlight> = emptyList(),
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Highlights", "Notes")

    val displayedHighlights = remember(highlights, selectedTab) {
        when (selectedTab) {
            1 -> highlights.filter { it.note.isNotBlank() }
            else -> highlights
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = themeColors.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Notes",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    if (highlights.isNotEmpty()) {
                        Text(
                            text = "${highlights.size} saved ${if (highlights.size == 1) "item" else "items"}",
                            fontSize = 12.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(themeColors.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.StickyNote2, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = themeColors.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = themeColors.primary
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = themeColors.cardBorder)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) themeColors.primary else themeColors.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (displayedHighlights.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                    border = BorderStroke(1.dp, themeColors.cardBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = themeColors.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.StickyNote2,
                                    contentDescription = null,
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 1) "No Notes Added Yet" else "No Highlights Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select any text while reading a PDF and tap 'Highlight' to save highlights and personal notes here.",
                            fontSize = 12.sp,
                            color = themeColors.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayedHighlights.size) { idx ->
                        val hl = displayedHighlights[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(hl.color), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Page ${hl.pageIndex + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "\"${hl.text}\"",
                                    fontSize = 14.sp,
                                    color = themeColors.onSurface,
                                    lineHeight = 20.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (hl.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = themeColors.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, themeColors.cardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Note: ${hl.note}",
                                            fontSize = 12.sp,
                                            color = themeColors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNotesSheet(
    sheetState: SheetState,
    highlights: List<Highlight> = emptyList(),
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    SmartNotesBottomSheet(
        sheetState = sheetState,
        highlights = highlights,
        onDismiss = onDismiss,
        themeColors = themeColors
    )
}

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 4. Favorites Sheet (Unified Single Favorites Flow)
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    sheetState: SheetState,
    scannedPdfs: List<ScannedPdf> = emptyList(),
    favoritedUris: Set<String> = emptySet(),
    onOpenDocument: (Uri) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    val favoritePdfs = remember(scannedPdfs, favoritedUris) {
        scannedPdfs.filter { favoritedUris.contains(it.uri.toString()) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = themeColors.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Favorites",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    if (favoritePdfs.isNotEmpty()) {
                        Text(
                            text = "${favoritePdfs.size} ${if (favoritePdfs.size == 1) "book" else "books"}",
                            fontSize = 12.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(themeColors.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (favoritePdfs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(28.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = themeColors.primaryContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.FavoriteBorder,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Favorites Yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the heart icon on any document or in the reader to add it to your Favorites.",
                                    fontSize = 12.sp,
                                    color = themeColors.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                } else {
                    items(favoritePdfs.size) { idx ->
                        val pdf = favoritePdfs[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenDocument(pdf.uri)
                                },
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PdfCoverThumbnail(
                                        uri = pdf.uri,
                                        title = pdf.title,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier.size(40.dp, 52.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pdf.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pdf.formattedSize,
                                            fontSize = 12.sp,
                                            color = themeColors.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onToggleFavorite(pdf.uri.toString()) }) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Remove from Favorites",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}