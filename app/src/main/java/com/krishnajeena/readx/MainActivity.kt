package com.krishnajeena.readx

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.krishnajeena.readx.ui.OnboardingScreen
import com.krishnajeena.readx.ui.ScanningScreen
import com.krishnajeena.readx.ui.RecentDocumentsSheet
import com.krishnajeena.readx.ui.AiChatPdfPickerSheet
import com.krishnajeena.readx.ui.SmartNotesSheet
import androidx.compose.material.icons.filled.Favorite
import com.krishnajeena.readx.ui.FavoritesSheet
import com.krishnajeena.readx.ui.LibraryScreen
import com.krishnajeena.readx.ui.ProfileScreen
import com.krishnajeena.readx.ui.PdfCoverThumbnail
import com.krishnajeena.readx.ui.AiChatScreen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krishnajeena.readx.data.ScannedPdf
import com.krishnajeena.readx.reader.AiDialog
import com.krishnajeena.readx.reader.NotesSheet
import com.krishnajeena.readx.reader.ReaderScreen
import com.krishnajeena.readx.reader.ReaderUiState
import com.krishnajeena.readx.reader.ReaderViewModel
import com.krishnajeena.readx.reader.RecentDocument
import com.krishnajeena.readx.ui.AnimatedSplashScreen
import com.krishnajeena.readx.ui.SettingsScreen
import com.krishnajeena.readx.ui.theme.ReadXTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            )
            slideUp.interpolator = AnticipateInterpolator()
            slideUp.duration = 350L
            slideUp.doOnEnd { splashScreenView.remove() }
            slideUp.start()
        }

        setContent {
            ReadXTheme {
                ReadXApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadXApp(viewModel: ReaderViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scannedPdfs by viewModel.scannedPdfs.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val aiChatSession by viewModel.aiChatSession.collectAsState()

    var hasStoragePermission by remember {
        mutableStateOf(context.hasDocPermissions())
    }
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(!hasStoragePermission) }
    var showSettings by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showRecentSheet by remember { mutableStateOf(false) }
    var showAiPickerSheet by remember { mutableStateOf(false) }
    var showSmartNotesSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var favoritedUris by remember { mutableStateOf(setOf<String>()) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var aiDialogText by remember { mutableStateOf<String?>(null) }
    var fileInfoState by remember { mutableStateOf<FileInfoState?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val notesSheetState = rememberModalBottomSheetState()
    val recentSheetState = rememberModalBottomSheetState()
    val aiPickerSheetState = rememberModalBottomSheetState()
    val smartNotesSheetState = rememberModalBottomSheetState()
    val favoritesSheetState = rememberModalBottomSheetState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.settingsRepo.fetchDynamicConfig()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasStoragePermission = granted
        if (granted) {
            isScanning = true
            viewModel.scanDevicePdfs()
            coroutineScope.launch {
                kotlinx.coroutines.delay(1800)
                isScanning = false
            }
        }
    }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                context.persistReadPermission(uri)
                viewModel.openDocument(uri)
            }
        }

    val openDocumentForAiLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                context.persistReadPermission(uri)
                val fileName = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null
                } ?: uri.lastPathSegment ?: "Document.pdf"
                viewModel.startAiChat(uri, fileName)
            }
        }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val granted = context.hasDocPermissions()
                hasStoragePermission = granted
                if (granted) {
                    viewModel.scanDevicePdfs()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    if (showSplash) {
        AnimatedSplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    if (showOnboarding && !hasStoragePermission) {
        OnboardingScreen(
            onOnboardingFinished = { showOnboarding = false }
        )
        return
    }

    if (isScanning) {
        ScanningScreen(scannedCount = scannedPdfs.size)
        return
    }

    if (showSettings) {
        SettingsScreen(
            settingsRepo = viewModel.settingsRepo,
            onBack = { showSettings = false }
        )
        return
    }

    val currentAiChat = aiChatSession
    if (currentAiChat != null) {
        AiChatScreen(
            session = currentAiChat,
            onSendMessage = { query -> viewModel.sendAiChatMessage(query) },
            onOpenReader = { uri ->
                viewModel.closeAiChat()
                viewModel.openDocument(uri)
            },
            onOpenSettings = { showSettings = true },
            onBack = { viewModel.closeAiChat() }
        )
        return
    }

    if (showNotesSheet) {
        NotesSheet(
            viewModel = viewModel,
            sheetState = notesSheetState,
            onDismiss = { showNotesSheet = false },
            onSelectHighlight = { highlight ->
                showNotesSheet = false
                viewModel.jumpToPage(highlight.pageIndex)
            }
        )
    }

    if (fileInfoState != null) {
        FileInfoDialog(
            info = fileInfoState!!,
            onDismiss = { fileInfoState = null }
        )
    }

    if (aiDialogText != null) {
        AiDialog(
            selectedText = aiDialogText!!,
            viewModel = viewModel,
            onDismiss = { aiDialogText = null },
            onNavigateToSettings = {
                aiDialogText = null
                showSettings = true
            }
        )
    }


    BackHandler(enabled = state is ReaderUiState.Document) {
        isSearchExpanded = false
        viewModel.closeDocument()
    }

    Scaffold(
        topBar = {
            if (state is ReaderUiState.Document) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchExpanded = false
                            viewModel.closeDocument()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Close document",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    title = {
                        Text(
                            text = (state as ReaderUiState.Document).document.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search document",
                                tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showNotesSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.StickyNote2,
                                contentDescription = "Notes & Highlights",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        when (val current = state) {
            is ReaderUiState.Home -> {
                if (showPermissionRationale) {
                    PermissionRationaleDialog(
                        onAllow = {
                            showPermissionRationale = false
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            } else {
                                permissionLauncher.launch(getRequiredPermissions())
                            }
                        },
                        onDeny = {
                            showPermissionRationale = false
                        }
                    )
                }

                if (showRecentSheet) {
                    RecentDocumentsSheet(
                        sheetState = recentSheetState,
                        recentList = (state as? ReaderUiState.Home)?.recentList ?: emptyList(),
                        scannedPdfs = scannedPdfs,
                        onSelectDocument = { uri -> viewModel.openDocument(uri) },
                        onDismiss = { showRecentSheet = false }
                    )
                }

                if (showAiPickerSheet) {
                    AiChatPdfPickerSheet(
                        sheetState = aiPickerSheetState,
                        scannedPdfs = scannedPdfs,
                        onSelectPdfForAi = { uri, title ->
                            viewModel.startAiChat(uri, title)
                        },
                        onOpenFilePicker = { openDocumentForAiLauncher.launch(arrayOf("application/pdf")) },
                        onDismiss = { showAiPickerSheet = false }
                    )
                }

                if (showSmartNotesSheet) {
                    SmartNotesSheet(
                        sheetState = smartNotesSheetState,
                        onDismiss = { showSmartNotesSheet = false }
                    )
                }

                if (showFavoritesSheet) {
                    FavoritesSheet(
                        sheetState = favoritesSheetState,
                        scannedPdfs = scannedPdfs,
                        favoritedUris = favoritedUris,
                        onOpenDocument = { uri -> viewModel.openDocument(uri) },
                        onToggleFavorite = { uriStr ->
                            favoritedUris = if (favoritedUris.contains(uriStr)) favoritedUris - uriStr else favoritedUris + uriStr
                        },
                        onDismiss = { showFavoritesSheet = false }
                    )
                }

                Scaffold(
                    bottomBar = {
                        HomeBottomNavBar(
                            selectedTab = selectedBottomTab,
                            onTabSelected = { selectedBottomTab = it }
                        )
                    }
                ) { innerPadding ->
                    when (selectedBottomTab) {
                        0 -> HomeScreen(
                            padding = innerPadding,
                            recent = current.recent,
                            recentList = current.recentList,
                            scannedPdfs = scannedPdfs,
                            weeklyStats = weeklyStats,
                            hasStoragePermission = hasStoragePermission,
                            onRequestPermission = { showPermissionRationale = true },
                            onOpenPicker = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                            onOpenDocument = { uri -> viewModel.openDocument(uri) },
                            onViewNotes = { recent ->
                                viewModel.openDocument(recent.uri)
                                showNotesSheet = true
                            },
                            onShowFileInfo = { info -> fileInfoState = info },
                            onSharePdf = { uri, title -> sharePdf(context, uri, title) },
                            onOpenSettings = { showSettings = true },
                            onOpenRecentSheet = { showRecentSheet = true },
                            onOpenAiPickerSheet = { showAiPickerSheet = true },
                            onOpenSmartNotesSheet = { showSmartNotesSheet = true },
                            onOpenFavoritesSheet = { showFavoritesSheet = true },
                            favoritedUris = favoritedUris,
                            onToggleFavorite = { uriStr ->
                                favoritedUris = if (favoritedUris.contains(uriStr)) favoritedUris - uriStr else favoritedUris + uriStr
                            }
                        )
                        1 -> LibraryScreen(
                            padding = innerPadding,
                            scannedPdfs = scannedPdfs,
                            onOpenDocument = { uri -> viewModel.openDocument(uri) },
                            onOpenPicker = { openDocumentLauncher.launch(arrayOf("application/pdf")) }
                        )
                        2 -> ProfileScreen(
                            padding = innerPadding,
                            scannedPdfs = scannedPdfs,
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }


            ReaderUiState.Loading -> LoadingScreen(padding)

            is ReaderUiState.Error -> ErrorScreen(
                padding = padding,
                message = current.message,
                onOpenPicker = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                onBack = { viewModel.backToHome() }
            )

            is ReaderUiState.Document -> ReaderScreen(
                padding = padding,
                document = current.document,
                viewModel = viewModel,
                onCopy = { text ->
                    context.copyToClipboard(text)
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
                onShowAiDialog = { text ->
                    aiDialogText = text
                },
                isSearchExpanded = isSearchExpanded,
                onCloseSearch = { isSearchExpanded = false }
            )
        }
    }
}

data class FileInfoState(
    val title: String,
    val uriString: String,
    val formattedSize: String,
    val formattedDate: String
)

@Composable
private fun FileInfoDialog(
    info: FileInfoState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("File Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(info.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("File Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(info.formattedSize, style = MaterialTheme.typography.bodyMedium)
                }
                if (info.formattedDate.isNotBlank()) {
                    Column {
                        Text("Last Modified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(info.formattedDate, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column {
                    Text("Location / URI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(info.uriString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun sharePdf(context: Context, uri: Uri, title: String) {
    runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
    }.onFailure {
        Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun PermissionRationaleDialog(
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        text = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF23252E),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6C5CE7), Color(0xFF8A70D6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ViewInAr,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Allow ReadX to access files?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ReadX needs access to your device storage to find and organize your documents.",
                        fontSize = 13.sp,
                        color = Color(0xFF9AA3AF),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onAllow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Text("Allow", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDeny,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deny", fontSize = 14.sp, color = Color(0xFF9AA3AF))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color.Transparent
    )
}

private fun getGreetingMessage(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning, Reader"
        in 12..16 -> "Good afternoon, Reader"
        in 17..22 -> "Good evening, Reader"
        else -> "Good night, Reader"
    }
}

@Composable
private fun QuickCategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3B4A5C)
            )
        }
    }
}

@Composable
private fun HomeBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair("Home", Icons.Outlined.Description),
                Pair("Library", Icons.Outlined.FolderSpecial),
                Pair("Profile", Icons.Outlined.Person)
            )

            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(index) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF2B3545) else Color(0xFF9AA3AF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF2B3545) else Color(0xFF9AA3AF)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    recent: RecentDocument?,
    recentList: List<RecentDocument> = emptyList(),
    scannedPdfs: List<ScannedPdf>,
    weeklyStats: com.krishnajeena.readx.data.WeeklyAnalyticsData,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenDocument: (Uri) -> Unit,
    onViewNotes: (RecentDocument) -> Unit,
    onShowFileInfo: (FileInfoState) -> Unit,
    onSharePdf: (Uri, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecentSheet: () -> Unit,
    onOpenAiPickerSheet: () -> Unit,
    onOpenSmartNotesSheet: () -> Unit,
    onOpenFavoritesSheet: () -> Unit,
    favoritedUris: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    val filteredScannedPdfs = remember(scannedPdfs, searchQuery) {
        if (searchQuery.isBlank()) scannedPdfs
        else scannedPdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val displayRecents = remember(recent, recentList) {
        if (recentList.isNotEmpty()) recentList
        else listOfNotNull(recent)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EE))) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Header (ReadX + Greeting + Lens Search & Settings)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "ReadX", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF1C2230)))
                                Text(text = getGreetingMessage(), style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF7A869A)))
                            }

                            Surface(
                                onClick = { isSearchVisible = !isSearchVisible },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSearchVisible) Color(0xFF6C5CE7) else Color(0xFFEDE9E1),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Lens",
                                        tint = if (isSearchVisible) Color.White else Color(0xFF2C3545),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        if (isSearchVisible) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search documents on device...") },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7A869A)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "Recent Scanned Documents" else "Search Results (${filteredScannedPdfs.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7A869A)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (filteredScannedPdfs.isEmpty()) {
                                        Text("No matching documents found", fontSize = 13.sp, color = Color(0xFF9AA3AF), modifier = Modifier.padding(vertical = 12.dp))
                                    } else {
                                        filteredScannedPdfs.take(5).forEach { pdf ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        isSearchVisible = false
                                                        onOpenDocument(pdf.uri)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                PdfCoverThumbnail(uri = pdf.uri, title = pdf.title, modifier = Modifier.size(36.dp, 48.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(pdf.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2230), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(pdf.formattedSize, fontSize = 12.sp, color = Color(0xFF8A94A6))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Category Pills (AI Chat, Notes, Favorites - removed redundant Recent)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickCategoryCard(Modifier.weight(1f).clickable { onOpenAiPickerSheet() }, "AI Chat", Icons.Outlined.ChatBubbleOutline, Color(0xFFEAF8F0), Color(0xFF27AE60))
                        QuickCategoryCard(Modifier.weight(1f).clickable { onOpenSmartNotesSheet() }, "Notes", Icons.Outlined.StickyNote2, Color(0xFFFEF5EA), Color(0xFFE67E22))
                        QuickCategoryCard(Modifier.weight(1f).clickable { onOpenFavoritesSheet() }, "Favorites", Icons.Outlined.FavoriteBorder, Color(0xFFF6EFFE), Color(0xFF8E44AD))
                    }
                }

                // CONDITIONAL: Show "Continue where you left off" & "Recent Reads" ONLY if recent != null
                if (recent != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(recent.uri) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEEFA)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF6C5CE7), Color(0xFF8A70D6))
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoStories,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Continue where you left off",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1C2230)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = recent.displayName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF7A869A)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6C5CE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Reads",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C2230)
                                )
                            )
                            TextButton(onClick = onOpenRecentSheet) {
                                Text("See all", fontSize = 13.sp, color = Color(0xFF5A77FF), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (displayRecents.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(displayRecents.size) { idx ->
                                    val item = displayRecents[idx]
                                    Column(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable { onOpenDocument(item.uri) }
                                    ) {
                                        PdfCoverThumbnail(
                                            uri = item.uri,
                                            title = item.displayName,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C2230),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Recently read",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8A94A6)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // "This Week" Reading Analytics Card (Real Time, Pages, Books Finished)
                if (hasStoragePermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "This Week",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C2230)
                                        )
                                    )

                                    Surface(
                                        color = if (weeklyStats.isPositiveTrend) Color(0xFFEAF8F0) else Color(0xFFFFF1F0),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = weeklyStats.percentageComparison,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (weeklyStats.isPositiveTrend) Color(0xFF27AE60) else Color(0xFFE74C3C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Daily breakdown row (M T W T F S S)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    weeklyStats.dailyStats.forEach { dayStat ->
                                        val isHighlight = dayStat.isToday || dayStat.isPeak
                                        val timeText = if (dayStat.minutes > 0) {
                                            if (dayStat.minutes >= 60) "${dayStat.minutes / 60}h ${dayStat.minutes % 60}m" else "${dayStat.minutes}m"
                                        } else "0m"

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = timeText,
                                                fontSize = 9.sp,
                                                color = if (isHighlight) Color(0xFF6C5CE7) else Color(0xFF8A94A6),
                                                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = dayStat.dayInitial,
                                                fontSize = 12.sp,
                                                fontWeight = if (dayStat.isToday) FontWeight.Black else FontWeight.Bold,
                                                color = if (dayStat.isToday) Color(0xFF6C5CE7) else Color(0xFF1C2230)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Metrics Row (Total Time | Pages Read | Books Finished)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(weeklyStats.totalTimeFormatted, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C2230))
                                        Text("Total Time", fontSize = 11.sp, color = Color(0xFF7A869A))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${weeklyStats.totalPagesRead}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C2230))
                                        Text("Pages Read", fontSize = 11.sp, color = Color(0xFF7A869A))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${weeklyStats.booksFinishedCount}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C2230))
                                        Text("Books Finished", fontSize = 11.sp, color = Color(0xFF7A869A))
                                    }
                                }
                            }
                        }
                    }

                    // "All Documents" Section with List / Grid Toggle (Matching Image 5)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All Documents",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C2230)
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { isGridView = false },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isGridView) Color(0xFF2C3545) else Color.White
                                ) {
                                    Text("List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isGridView) Color.White else Color(0xFF7A869A), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                                Surface(
                                    onClick = { isGridView = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isGridView) Color(0xFF2C3545) else Color.White
                                ) {
                                    Text("Grid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isGridView) Color.White else Color(0xFF7A869A), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }

                    if (filteredScannedPdfs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Text(text = if (searchQuery.isNotBlank()) "No PDFs match \"$searchQuery\"" else "Scanning device for PDFs...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (isGridView) {
                        val pairs = filteredScannedPdfs.chunked(2)
                        items(pairs.size) { idx ->
                            val pair = pairs[idx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { pdf ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onOpenDocument(pdf.uri) },
                                        elevation = CardDefaults.cardElevation(2.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            PdfCoverThumbnail(
                                                uri = pdf.uri,
                                                title = pdf.title,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(pdf.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2230), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(pdf.formattedSize, fontSize = 11.sp, color = Color(0xFF8A94A6))
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(filteredScannedPdfs, key = { it.uri.toString() }) { pdf ->
                            ScannedPdfItem(
                                pdf = pdf,
                                isFavorited = favoritedUris.contains(pdf.uri.toString()),
                                onToggleFavorite = { onToggleFavorite(pdf.uri.toString()) },
                                onClick = { onOpenDocument(pdf.uri) },
                                onShowFileInfo = onShowFileInfo,
                                onSharePdf = onSharePdf
                            )
                        }
                    }
                } else {
                    // No Permission State
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF2B3545)), shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(52.dp).background(Color(0xFF3B475A), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoStories, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text("ReadX", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                        Text("Book Reader • AI • Smart Notes", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B9C6)))
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2B3545))) {
                                    Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open File Picker", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Find documents on this device", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Grant permission to scan and display all PDF documents stored on your device.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7B8D)))
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(onClick = onRequestPermission, shape = RoundedCornerShape(14.dp)) { Text("Show Device PDFs") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedPdfItem(
    pdf: ScannedPdf,
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit,
    onShowFileInfo: (FileInfoState) -> Unit,
    onSharePdf: (Uri, String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PdfCoverThumbnail(
                uri = pdf.uri,
                title = pdf.title,
                cornerRadius = 8.dp,
                modifier = Modifier.size(38.dp, 48.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pdf.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2230)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pdf.formattedSize, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8A94A6)))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorited) Color(0xFFE74C3C) else Color(0xFF8A94A6),
                    modifier = Modifier.size(20.dp)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Options") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(text = { Text(if (isFavorited) "Remove Favorite 💔" else "Add Favorite 💖") }, onClick = { menuExpanded = false; onToggleFavorite() })
                    DropdownMenuItem(text = { Text("File Info ℹ️") }, onClick = { menuExpanded = false; onShowFileInfo(FileInfoState(pdf.title, pdf.uri.toString(), pdf.formattedSize, pdf.formattedDate)) })
                    DropdownMenuItem(text = { Text("Share 📤") }, onClick = { menuExpanded = false; onSharePdf(pdf.uri, pdf.title) })
                }
            }
        }
    }
}

private fun Context.hasDocPermissions(): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager() || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredPermissions(): Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

private fun Context.persistReadPermission(uri: Uri) {
    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("PDF text", text))
}

@Composable
private fun LoadingScreen(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Opening document...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorScreen(
    padding: PaddingValues,
    message: String,
    onOpenPicker: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Could not open document", style = MaterialTheme.typography.titleLarge)
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenPicker) {
                Text("Choose another")
            }
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
