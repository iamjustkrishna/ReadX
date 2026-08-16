package com.krishnajeena.readx.ui

import android.net.Uri
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
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishnajeena.readx.reader.RecentDocument
import com.krishnajeena.readx.data.ScannedPdf

// ═════════════════════════════════════════════════════════════
// 1. Recent Documents Sheet (Matching Image 1 Left)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDocumentsSheet(
    sheetState: SheetState,
    recentList: List<RecentDocument> = emptyList(),
    scannedPdfs: List<ScannedPdf> = emptyList(),
    onSelectDocument: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val displayList = remember(recentList, scannedPdfs) {
        if (recentList.isNotEmpty()) recentList
        else scannedPdfs.map { RecentDocument(it.uri, it.title) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
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
                color = Color(0xFF1C2230)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent documents found", color = Color(0xFF7A869A))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val list = displayList.take(10)
                    items(list.size) { idx ->
                        val item = list[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectDocument(item.uri)
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                        modifier = Modifier.size(44.dp, 58.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = item.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C2230),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Opened recently",
                                            fontSize = 12.sp,
                                            color = Color(0xFF7A869A)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEBF3FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF2B7FFF),
                                        modifier = Modifier.size(16.dp)
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

// ═════════════════════════════════════════════════════════════
// 2. AI Chat PDF Selection Sheet (Matching Image 1 Flow)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatPdfPickerSheet(
    sheetState: SheetState,
    scannedPdfs: List<ScannedPdf>,
    onSelectPdfForAi: (Uri, String) -> Unit,
    onOpenFilePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
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
                color = Color(0xFF1C2230)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose a document from your device or recent list to start an interactive AI discussion.",
                fontSize = 13.sp,
                color = Color(0xFF7A869A)
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
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Open File Picker", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent PDFs",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C2230)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val list = scannedPdfs.take(5)
                items(list.size) { idx ->
                    val pdf = list[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onSelectPdfForAi(pdf.uri, pdf.title)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FC)),
                        shape = RoundedCornerShape(16.dp)
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
                                        .background(Color(0xFFEAF8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF27AE60), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pdf.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2230), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(pdf.formattedSize, fontSize = 12.sp, color = Color(0xFF8A94A6))
                                }
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    onSelectPdfForAi(pdf.uri, pdf.title)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Start AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════
// 3. Smart Notes Sheet (Matching Image 2)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartNotesSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Notes", "Highlights", "AI Summary")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF6F4EE),
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
                Text(
                    text = "Smart Notes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C2230)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFEF5EA), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.StickyNote2, contentDescription = null, tint = Color(0xFFE67E22), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF6C5CE7)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF6C5CE7), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Design Systems", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
                                Text(" • Page 45", fontSize = 12.sp, color = Color(0xFF8A94A6))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"Atomic design is not a linear process, but a mental model to help us think of our user interfaces as...\"",
                                fontSize = 14.sp,
                                color = Color(0xFF2C3545),
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = Color(0xFFEBF3FE), shape = RoundedCornerShape(8.dp)) {
                                    Text("Highlight", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2B7FFF), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                                Surface(color = Color(0xFFEAF8F0), shape = RoundedCornerShape(8.dp)) {
                                    Text("AI Note", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF27AE60), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                item {
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════
// 4. Favorites Sheet (Matching Image 3 Left)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    sheetState: SheetState,
    scannedPdfs: List<ScannedPdf> = emptyList(),
    favoritedUris: Set<String> = emptySet(),
    onOpenDocument: (Uri) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Starred", "Reading List", "Bookmarks")

    val favoritePdfs = remember(scannedPdfs, favoritedUris) {
        scannedPdfs.filter { favoritedUris.contains(it.uri.toString()) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF6F4EE),
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
                Text(
                    text = "Favorites",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C2230)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF6EFFE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF8E44AD), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF8E44AD)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (favoritePdfs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFDCD6F7), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No Starred Books Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2230))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap the heart icon on any document to add it to your Favorites.", fontSize = 12.sp, color = Color(0xFF7A869A))
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
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    PdfCoverThumbnail(
                                        uri = pdf.uri,
                                        title = pdf.title,
                                        modifier = Modifier.size(42.dp, 56.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2230), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(pdf.formattedSize, fontSize = 12.sp, color = Color(0xFF7A869A))
                                    }
                                }
                                androidx.compose.material3.IconButton(onClick = { onToggleFavorite(pdf.uri.toString()) }) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Unfavorite", tint = Color(0xFFE74C3C), modifier = Modifier.size(22.dp))
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
