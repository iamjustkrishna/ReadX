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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.krishnajeena.readx.ui.PdfCoverThumbnail
import com.krishnajeena.readx.ui.AboutAppDialog
import com.krishnajeena.readx.ui.StorageInfoDialog
import com.krishnajeena.readx.ui.PreferencesDialog
import com.krishnajeena.readx.ui.AiSettingsDialog
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishnajeena.readx.data.ScannedPdf

// ═════════════════════════════════════════════════════════════
// 1. Library Screen (Matching Image 3 Right)
// ═════════════════════════════════════════════════════════════

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    scannedPdfs: List<ScannedPdf>,
    onOpenDocument: (Uri) -> Unit,
    onOpenPicker: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf("Latest") }
    val filters = listOf("All", "PDF", "Folders")

    val filteredPdfs = remember(scannedPdfs, searchQuery, selectedFilterIndex, selectedSort) {
        var list = if (searchQuery.isBlank()) scannedPdfs
        else scannedPdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }

        list = when (selectedSort) {
            "Latest" -> list.sortedByDescending { it.dateModified }
            "Oldest" -> list.sortedBy { it.dateModified }
            "Largest" -> list.sortedByDescending { it.sizeBytes }
            "Smallest" -> list.sortedBy { it.sizeBytes }
            else -> list
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F4EE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title Header
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C2230)
                )
            )
            Text(
                text = "${scannedPdfs.size} documents",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF7A869A))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search input field with filter button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search documents...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7A869A)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )

                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort Options", tint = Color(0xFF2C3545))
                        }
                    }

                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        listOf("Latest", "Oldest", "Largest", "Smallest").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedSort = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters.size) { idx ->
                    val isSelected = selectedFilterIndex == idx
                    Surface(
                        onClick = { selectedFilterIndex = idx },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF2C3545) else Color.White,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Text(
                            text = filters[idx],
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF7A869A),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Document Grid
            if (filteredPdfs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No documents found in Library", color = Color(0xFF7A869A))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredPdfs.size) { idx ->
                        val pdf = filteredPdfs[idx]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(pdf.uri) }
                        ) {
                        PdfCoverThumbnail(
                            uri = pdf.uri,
                            title = pdf.title,
                            cornerRadius = 10.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pdf.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C2230),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pdf.formattedSize,
                                fontSize = 12.sp,
                                color = Color(0xFF8A94A6)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// 2. Profile Screen (Matching Image 4)
// ═════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(
    padding: PaddingValues,
    scannedPdfs: List<ScannedPdf> = emptyList(),
    onOpenSettings: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) AboutAppDialog(onDismiss = { showAboutDialog = false })
    if (showStorageDialog) StorageInfoDialog(scannedPdfs = scannedPdfs, onDismiss = { showStorageDialog = false })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F4EE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top half purple gradient banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF6C5CE7), Color(0xFF8A70D6))
                        )
                    )
            )

            // User Info & Content area with avatar overlapping purple/light split boundary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-40).dp)
            ) {
                // Profile Avatar Badge
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF6C5CE7),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "Krish",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C2230)
                        )
                        Surface(
                            color = Color(0xFF6C5CE7).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Pro Reader",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6C5CE7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Row Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(Modifier.weight(1f), "${scannedPdfs.size}", "Books\nIndexed", Color(0xFF2B7FFF))
                    ProfileStatCard(Modifier.weight(1f), "0h", "Read\nTime", Color(0xFF27AE60))
                    ProfileStatCard(Modifier.weight(1f), "1", "Active\nStreak", Color(0xFFE67E22))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Menu items: Preferences, AI Settings, Storage, About
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        val totalMb = String.format("%.1f MB used", scannedPdfs.sumOf { it.sizeBytes } / (1024f * 1024f))
                        ProfileMenuItem("AI Settings", Icons.Outlined.ChatBubbleOutline, Color(0xFFEAF8F0), Color(0xFF27AE60), onClick = onOpenSettings)
                        ProfileMenuItem("Storage", Icons.Outlined.SdStorage, Color(0xFFFEF5EA), Color(0xFFE67E22), trailingText = totalMb, onClick = { showStorageDialog = true })
                        ProfileMenuItem("About", Icons.Outlined.Info, Color(0xFFF6EFFE), Color(0xFF8E44AD), onClick = { showAboutDialog = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = valueColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7A869A))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2230))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(text = trailingText, fontSize = 12.sp, color = Color(0xFF7A869A))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9AA3AF), modifier = Modifier.size(20.dp))
        }
    }
}
