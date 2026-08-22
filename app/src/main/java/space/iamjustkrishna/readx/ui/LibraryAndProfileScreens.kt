package space.iamjustkrishna.readx.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.R
import space.iamjustkrishna.readx.data.ScannedPdf
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 1. Library Screen
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    scannedPdfs: List<ScannedPdf>,
    favoritedUris: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onOpenDocument: (Uri) -> Unit,
    onShowFileInfo: (space.iamjustkrishna.readx.FileInfoState) -> Unit = {},
    onSharePdf: (Uri, String) -> Unit = { _, _ -> },
    themeColors: ThemeColors = MinimalistLightColors
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortOptions = listOf("Recently Added", "Name (A-Z)", "File Size (Largest)")

    val filteredPdfs = remember(scannedPdfs, searchQuery, selectedSortOption) {
        val list = if (searchQuery.isBlank()) scannedPdfs
        else scannedPdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }

        when (selectedSortOption) {
            1 -> list.sortedBy { it.title.lowercase() }
            2 -> list.sortedByDescending { it.sizeBytes }
            else -> list.sortedByDescending { it.dateModified }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Library Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground
                        )
                    )
                    Text(
                        text = "${scannedPdfs.size} documents indexed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = themeColors.onSurfaceVariant
                        )
                    )
                }

                // Sort Dropdown Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = themeColors.surfaceVariant,
                        border = BorderStroke(1.dp, themeColors.cardBorder),
                        modifier = Modifier.clickable { showSortMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort",
                                tint = themeColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sortOptions[selectedSortOption],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColors.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(themeColors.surface)
                    ) {
                        sortOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontWeight = if (selectedSortOption == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOption == index) themeColors.primary else themeColors.onSurface
                                    )
                                },
                                trailingIcon = if (selectedSortOption == index) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = themeColors.primary) }
                                } else null,
                                colors = MenuDefaults.itemColors(
                                    textColor = themeColors.onSurface
                                ),
                                onClick = {
                                    selectedSortOption = index
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar with High Contrast
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search documents in library...",
                        color = themeColors.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = themeColors.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PDF Grid List
            if (filteredPdfs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No documents matching \"$searchQuery\"" else "No PDF documents found",
                        color = themeColors.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredPdfs.size) { idx ->
                        val pdf = filteredPdfs[idx]
                        val isFav = favoritedUris.contains(pdf.uri.toString())
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(pdf.uri) },
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    PdfCoverThumbnail(
                                        uri = pdf.uri,
                                        title = pdf.title,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Favorite badge top-right
                                    IconButton(
                                        onClick = { onToggleFavorite(pdf.uri.toString()) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .background(
                                                color = themeColors.surface.copy(alpha = 0.88f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = if (isFav) "Remove Favorite" else "Add Favorite",
                                            tint = if (isFav) Color(0xFFEF4444) else themeColors.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = pdf.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onSurface,
                                    minLines = 2,
                                    maxLines = 2,
                                    lineHeight = 17.sp,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = pdf.formattedSize,
                                    fontSize = 11.sp,
                                    color = themeColors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”
// 2. Profile Screen
// â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”

@Composable
fun ProfileScreen(
    padding: PaddingValues,
    scannedPdfs: List<ScannedPdf> = emptyList(),
    currentTheme: space.iamjustkrishna.readx.ui.theme.AppTheme = space.iamjustkrishna.readx.ui.theme.AppTheme.SYSTEM_DEFAULT,
    onThemeSelected: (space.iamjustkrishna.readx.ui.theme.AppTheme) -> Unit = {},
    onOpenSettings: () -> Unit,
    onClearCache: () -> Long = { 0L }
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeColors = space.iamjustkrishna.readx.ui.theme.getThemeColors(currentTheme)

    if (showAboutDialog) AboutAppDialog(onDismiss = { showAboutDialog = false }, themeColors = themeColors)
    if (showStorageDialog) StorageInfoDialog(scannedPdfs = scannedPdfs, onDismiss = { showStorageDialog = false }, themeColors = themeColors)
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
            onDismiss = { showThemeDialog = false },
            themeColors = themeColors
        )
    }

    val totalBytes = scannedPdfs.sumOf { it.sizeBytes }
    val totalStorageFormatted = if (totalBytes >= 1024L * 1024L * 1024L) {
        String.format("%.2f GB PDFs", totalBytes / (1024f * 1024f * 1024f))
    } else {
        String.format("%.1f MB PDFs", totalBytes / (1024f * 1024f))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reader Identity Banner with Glowing 3D App Icon
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.cardBorder),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.readx_logo),
                            contentDescription = "ReadX Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private Reader",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "On-Device Storage \u2022 Zero Cloud Tracking",
                            fontSize = 12.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preferences & Menu items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.cardBorder),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    ProfileMenuItem(
                        title = "App Theme",
                        icon = Icons.Outlined.AutoStories,
                        bgColor = themeColors.primaryContainer,
                        contentColor = themeColors.primary,
                        textColor = themeColors.onSurface,
                        secondaryColor = themeColors.onSurfaceVariant,
                        trailingText = currentTheme.displayName,
                        onClick = { showThemeDialog = true }
                    )
                    ProfileMenuItem(
                        title = "AI Settings",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        bgColor = if (themeColors.isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                        contentColor = if (themeColors.isDark) Color(0xFF34D399) else Color(0xFF059669),
                        textColor = themeColors.onSurface,
                        secondaryColor = themeColors.onSurfaceVariant,
                        onClick = onOpenSettings
                    )
                    ProfileMenuItem(
                        title = "Storage Usage",
                        icon = Icons.Outlined.SdStorage,
                        bgColor = if (themeColors.isDark) Color(0xFF451A03) else Color(0xFFFFFBEB),
                        contentColor = if (themeColors.isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        textColor = themeColors.onSurface,
                        secondaryColor = themeColors.onSurfaceVariant,
                        trailingText = totalStorageFormatted,
                        onClick = { showStorageDialog = true }
                    )
                    ProfileMenuItem(
                        title = "About ReadX",
                        icon = Icons.Outlined.Info,
                        bgColor = themeColors.primaryContainer,
                        contentColor = themeColors.primary,
                        textColor = themeColors.onSurface,
                        secondaryColor = themeColors.onSurfaceVariant,
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extended Utilities Card (Fills empty bottom space with useful features)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.cardBorder),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = themeColors.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CleaningServices,
                                        contentDescription = null,
                                        tint = themeColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Maintenance & Utilities",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onSurface
                                )
                                Text(
                                    text = "Clean cached document previews & thumbnails",
                                    fontSize = 11.sp,
                                    color = themeColors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val freed = onClearCache()
                                val freedFormatted = if (freed >= 1024L * 1024L) String.format("%.1f MB", freed.toFloat() / (1024f * 1024f)) else if (freed > 0) "${freed / 1024} KB" else "0 B"; Toast.makeText(context, "Cache cleared ($freedFormatted freed)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primaryContainer,
                                contentColor = themeColors.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cache", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "ReadX - Lightning-Fast, Private AI-Powered PDF Reader for Android.\n\nExperience seamless reading, instant translation, smart highlights, and context-aware AI chat.\n\nDownload ReadX on Google Play: https://play.google.com/store/apps/details?id=space.iamjustkrishna.readx"
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share ReadX")
                                context.startActivity(shareIntent)
                            },
                            border = BorderStroke(1.dp, themeColors.cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = themeColors.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share App", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = themeColors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Private by design \u2022 No telemetry \u2022 100% on-device",
                            fontSize = 11.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    textColor: Color,
    secondaryColor: Color,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 13.sp,
                    color = secondaryColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = secondaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
