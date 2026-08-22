package space.iamjustkrishna.readx

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MenuDefaults

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
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.outlined.Home
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
import space.iamjustkrishna.readx.ui.OnboardingScreen
import space.iamjustkrishna.readx.ui.ScanningScreen
import space.iamjustkrishna.readx.ui.RecentDocumentsSheet
import space.iamjustkrishna.readx.ui.AiChatPdfPickerSheet
import space.iamjustkrishna.readx.ui.SmartNotesSheet
import androidx.compose.material.icons.filled.Favorite
import space.iamjustkrishna.readx.ui.FavoritesSheet
import space.iamjustkrishna.readx.ui.LibraryScreen
import space.iamjustkrishna.readx.ui.ProfileScreen
import space.iamjustkrishna.readx.ui.PdfCoverThumbnail
import space.iamjustkrishna.readx.ui.AiChatScreen
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
import space.iamjustkrishna.readx.data.ScannedPdf
import space.iamjustkrishna.readx.reader.AiDialog
import space.iamjustkrishna.readx.reader.NotesSheet
import space.iamjustkrishna.readx.reader.ReaderScreen
import space.iamjustkrishna.readx.reader.ReaderUiState
import space.iamjustkrishna.readx.reader.ReaderViewModel
import space.iamjustkrishna.readx.reader.RecentDocument
import space.iamjustkrishna.readx.ui.AnimatedSplashScreen
import space.iamjustkrishna.readx.ui.SettingsScreen
import space.iamjustkrishna.readx.ui.theme.ThemeColors
import space.iamjustkrishna.readx.ui.theme.getThemeColors
import space.iamjustkrishna.readx.ui.theme.ReadXTheme

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
            val viewModel: ReaderViewModel = viewModel()
            var currentTheme by remember { mutableStateOf(viewModel.settingsRepo.getAppTheme()) }
            val colors = getThemeColors(currentTheme)

            // Update system bars when theme changes
            LaunchedEffect(colors.isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (colors.isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (colors.isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            ReadXTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    if (currentTheme == space.iamjustkrishna.readx.ui.theme.AppTheme.COSMIC_SPACE) {
                        space.iamjustkrishna.readx.ui.theme.CosmicSpaceParticleCanvas()
                    }
                    ReadXApp(
                        viewModel = viewModel,
                        currentTheme = currentTheme,
                        onThemeSelected = { newTheme ->
                            currentTheme = newTheme
                            viewModel.settingsRepo.setAppTheme(newTheme)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadXApp(
    viewModel: ReaderViewModel = viewModel(),
    currentTheme: space.iamjustkrishna.readx.ui.theme.AppTheme = space.iamjustkrishna.readx.ui.theme.AppTheme.SYSTEM_DEFAULT,
    onThemeSelected: (space.iamjustkrishna.readx.ui.theme.AppTheme) -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scannedPdfs by viewModel.scannedPdfs.collectAsState()
    val aiChatSession by viewModel.aiChatSession.collectAsState()
    val themeColors = getThemeColors(currentTheme)

    LaunchedEffect(scannedPdfs) {
        if (scannedPdfs.isNotEmpty()) {
            space.iamjustkrishna.readx.data.PdfCoverCache.preloadCovers(context, scannedPdfs.map { it.uri }, maxCount = 40, scope = this)
        }
    }

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
    val favoritedUris by viewModel.favoritedUris.collectAsState()
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            viewModel.scanDevicePdfs()
        }
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
            onBack = { showSettings = false },
            themeColors = themeColors
        )
        return
    }

    val currentAiChat = aiChatSession
    val savedChatSessions by viewModel.savedChatSessions.collectAsState()

    if (currentAiChat != null) {
        AiChatScreen(
            session = currentAiChat,
            savedSessions = savedChatSessions,
            onResumeSession = { saved -> viewModel.resumeAiChat(saved) },
            onDeleteSession = { id -> viewModel.deleteAiChatSession(id) },
            onSendMessage = { query -> viewModel.sendAiChatMessage(query) },
            onOpenReader = { uri ->
                viewModel.closeAiChat()
                viewModel.openDocument(uri)
            },
            onOpenSettings = { showSettings = true },
            onBack = { viewModel.closeAiChat() },
            themeColors = themeColors,
            currentTheme = currentTheme
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
            },
            themeColors = themeColors
        )
    }

    if (fileInfoState != null) {
        FileInfoDialog(
            info = fileInfoState!!,
            onDismiss = { fileInfoState = null },
            themeColors = themeColors
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
            },
            themeColors = themeColors
        )
    }


    BackHandler(enabled = state is ReaderUiState.Document) {
        isSearchExpanded = false
        viewModel.closeDocument()
    }

    Scaffold(
        topBar = {
            if (state is ReaderUiState.Document) {
                val doc = (state as ReaderUiState.Document).document
                val docUriStr = viewModel.currentDocumentUri ?: ""
                val isDocFav = favoritedUris.contains(docUriStr)
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchExpanded = false
                            viewModel.closeDocument()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close document",
                                tint = themeColors.onSurface
                            )
                        }
                    },
                    title = {
                        Text(
                            text = doc.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                    },
                    actions = {
                        if (docUriStr.isNotEmpty()) {
                            IconButton(onClick = {
                                val docUri = Uri.parse(docUriStr)
                                viewModel.startAiChat(docUri, doc.displayName)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = "Ask AI about this document",
                                    tint = themeColors.primary
                                )
                            }
                        }
                        if (docUriStr.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleFavorite(docUriStr) }) {
                                Icon(
                                    imageVector = if (isDocFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isDocFav) "Remove from Favorites" else "Add to Favorites",
                                    tint = if (isDocFav) Color(0xFFEF4444) else themeColors.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search document",
                                tint = if (isSearchExpanded) themeColors.primary else themeColors.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showNotesSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = "Notes & Highlights",
                                tint = themeColors.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = themeColors.surface,
                        titleContentColor = themeColors.onSurface,
                        navigationIconContentColor = themeColors.onSurface,
                        actionIconContentColor = themeColors.onSurfaceVariant
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
                        favoritedUris = favoritedUris,
                        onToggleFavorite = { uriStr -> viewModel.toggleFavorite(uriStr) },
                        onSelectDocument = { uri -> viewModel.openDocument(uri) },
                        onDismiss = { showRecentSheet = false },
                        themeColors = themeColors
                    )
                }

                if (showAiPickerSheet) {
                    AiChatPdfPickerSheet(
                        sheetState = aiPickerSheetState,
                        scannedPdfs = scannedPdfs,
                        recentList = (state as? ReaderUiState.Home)?.recentList ?: emptyList(),
                        onSelectPdfForAi = { uri, title ->
                            viewModel.startAiChat(uri, title)
                        },
                        onOpenFilePicker = { openDocumentForAiLauncher.launch(arrayOf("application/pdf")) },
                        onDismiss = { showAiPickerSheet = false },
                        themeColors = themeColors
                    )
                }

                val allSavedHighlights by viewModel.allSavedHighlights.collectAsState()
                if (showSmartNotesSheet) {
                    SmartNotesSheet(
                        sheetState = smartNotesSheetState,
                        highlights = allSavedHighlights,
                        onDismiss = { showSmartNotesSheet = false },
                        themeColors = themeColors
                    )
                }

                if (showFavoritesSheet) {
                    FavoritesSheet(
                        sheetState = favoritesSheetState,
                        scannedPdfs = scannedPdfs,
                        favoritedUris = favoritedUris,
                        onOpenDocument = { uri -> viewModel.openDocument(uri) },
                        onToggleFavorite = { uriStr -> viewModel.toggleFavorite(uriStr) },
                        onDismiss = { showFavoritesSheet = false },
                        themeColors = themeColors
                    )
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        HomeBottomNavBar(
                            selectedTab = selectedBottomTab,
                            onTabSelected = { selectedBottomTab = it },
                            themeColors = themeColors
                        )
                    }
                ) { innerPadding ->
                    when (selectedBottomTab) {
                        0 -> HomeScreen(
                            padding = innerPadding,
                            recent = current.recent,
                            recentList = current.recentList,
                            scannedPdfs = scannedPdfs,
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
                            onToggleFavorite = { uriStr -> viewModel.toggleFavorite(uriStr) },
                            themeColors = themeColors
                        )
                        1 -> LibraryScreen(
                            padding = innerPadding,
                            scannedPdfs = scannedPdfs,
                            favoritedUris = favoritedUris,
                            onToggleFavorite = { uriStr -> viewModel.toggleFavorite(uriStr) },
                            onOpenDocument = { uri -> viewModel.openDocument(uri) },
                            onShowFileInfo = { info -> fileInfoState = info },
                            onSharePdf = { uri, title -> sharePdf(context, uri, title) },
                            themeColors = themeColors
                        )
                        2 -> ProfileScreen(
                            padding = innerPadding,
                            scannedPdfs = scannedPdfs,
                            currentTheme = currentTheme,
                            onThemeSelected = onThemeSelected,
                            onOpenSettings = { showSettings = true },
                            onClearCache = {
                                val vFreed = viewModel.clearAppCache()
                                val cFreed = space.iamjustkrishna.readx.data.PdfCoverCache.clearCache(context)
                                vFreed + cFreed
                            }
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
                onCloseSearch = { isSearchExpanded = false },
                themeColors = themeColors
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
    onDismiss: () -> Unit,
    themeColors: space.iamjustkrishna.readx.ui.theme.ThemeColors = space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Info", fontWeight = FontWeight.Bold, color = themeColors.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("File Name", style = MaterialTheme.typography.labelSmall, color = themeColors.primary, fontWeight = FontWeight.Bold)
                    Text(info.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = themeColors.onSurface)
                }
                Column {
                    Text("File Size", style = MaterialTheme.typography.labelSmall, color = themeColors.primary, fontWeight = FontWeight.Bold)
                    Text(info.formattedSize, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurface)
                }
                if (info.formattedDate.isNotBlank()) {
                    Column {
                        Text("Last Modified", style = MaterialTheme.typography.labelSmall, color = themeColors.primary, fontWeight = FontWeight.Bold)
                        Text(info.formattedDate, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurface)
                    }
                }
                Column {
                    Text("Location / URI", style = MaterialTheme.typography.labelSmall, color = themeColors.primary, fontWeight = FontWeight.Bold)
                    Text(info.uriString, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.onPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK", fontWeight = FontWeight.Bold, color = themeColors.onPrimary)
            }
        },
        containerColor = themeColors.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
private fun sharePdf(context: Context, uri: Uri, title: String) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        runCatching {
            val shareDir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").let {
                if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
            }
            val tempFile = java.io.File(shareDir, cleanTitle)

            if (uri.scheme == "file" && uri.path != null) {
                val srcFile = java.io.File(uri.path!!)
                if (srcFile.exists()) {
                    srcFile.copyTo(tempFile, overwrite = true)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val shareableUri = if (tempFile.exists() && tempFile.length() > 0L) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } else {
                uri
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareableUri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    clipData = android.content.ClipData.newRawUri(title, shareableUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share $title").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }
        }.onFailure { e ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Could not share document: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
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
    accentColor: Color,
    onClick: () -> Unit = {},
    themeColors: ThemeColors = space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
) {
    val iconBgColor = if (themeColors.isDark) {
        accentColor.copy(alpha = 0.18f)
    } else {
        accentColor.copy(alpha = 0.12f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(22.dp),
        color = themeColors.surface,
        border = BorderStroke(
            1.dp,
            if (themeColors.isDark) themeColors.cardBorder else Color(0xFFE2E8F0).copy(alpha = 0.7f)
        ),
        shadowElevation = if (themeColors.isDark) 4.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.onSurface
            )
        }
    }
}

@Composable
private fun HomeBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    themeColors: ThemeColors = space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = themeColors.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("Home", Icons.Filled.Home, Icons.Outlined.Home),
                Triple("Library", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
                Triple("Profile", Icons.Filled.Person, Icons.Outlined.Person)
            )

            tabs.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isSelected) themeColors.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) filledIcon else outlinedIcon,
                            contentDescription = label,
                            tint = if (isSelected) themeColors.primary else themeColors.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) themeColors.primary else themeColors.onSurfaceVariant
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
    onToggleFavorite: (String) -> Unit = {},
    themeColors: ThemeColors = space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
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

    Box(modifier = Modifier.fillMaxSize().background(themeColors.background)) {
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
                                Text(text = "ReadX", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = themeColors.onBackground))
                                Text(text = getGreetingMessage(), style = MaterialTheme.typography.bodyMedium.copy(color = themeColors.onSurfaceVariant))
                            }

                            Surface(
                                onClick = { isSearchVisible = !isSearchVisible },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSearchVisible) themeColors.primary else themeColors.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Lens",
                                        tint = if (isSearchVisible) Color.White else themeColors.onBackground,
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
                                placeholder = { Text("Search documents on device...", color = themeColors.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = themeColors.onSurfaceVariant) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = themeColors.onSurfaceVariant)
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
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.cardBorder),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "Recent Scanned Documents" else "Search Results (${filteredScannedPdfs.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (filteredScannedPdfs.isEmpty()) {
                                        Text("No matching documents found", fontSize = 13.sp, color = themeColors.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
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
                                                    Text(pdf.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(pdf.formattedSize, fontSize = 12.sp, color = themeColors.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Category Pills (AI Chat, Notes, Favorites)
                item {
                    val aiColor = if (themeColors.isDark) Color(0xFF34D399) else Color(0xFF059669)
                    val notesColor = if (themeColors.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                    val favColor = if (themeColors.isDark) Color(0xFFF472B6) else Color(0xFFDB2777)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickCategoryCard(
                            modifier = Modifier.weight(1f),
                            title = "AI Chat",
                            icon = Icons.Outlined.ChatBubbleOutline,
                            accentColor = aiColor,
                            onClick = onOpenAiPickerSheet,
                            themeColors = themeColors
                        )
                        QuickCategoryCard(
                            modifier = Modifier.weight(1f),
                            title = "Notes",
                            icon = Icons.Outlined.StickyNote2,
                            accentColor = notesColor,
                            onClick = onOpenSmartNotesSheet,
                            themeColors = themeColors
                        )
                        QuickCategoryCard(
                            modifier = Modifier.weight(1f),
                            title = "Favorites",
                            icon = Icons.Outlined.FavoriteBorder,
                            accentColor = favColor,
                            onClick = onOpenFavoritesSheet,
                            themeColors = themeColors
                        )
                    }
                }

                // Recent Reads Row (Horizontal Carousel)
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
                                color = themeColors.onBackground
                            )
                        )
                        TextButton(onClick = onOpenRecentSheet) {
                            Text("See all", fontSize = 13.sp, color = themeColors.primary, fontWeight = FontWeight.SemiBold)
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
                                val itemUriStr = item.uri.toString()
                                val isFav = favoritedUris.contains(itemUriStr)
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { onOpenDocument(item.uri) }
                                ) {
                                    Box {
                                        PdfCoverThumbnail(
                                            uri = item.uri,
                                            title = item.displayName,
                                            cornerRadius = 10.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                        )
                                        Surface(
                                            onClick = { onToggleFavorite(itemUriStr) },
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.9f),
                                            shadowElevation = 2.dp,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFav) Color(0xFFEF4444) else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Recently read",
                                        fontSize = 11.sp,
                                        color = themeColors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (hasStoragePermission) {
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
                                    color = themeColors.onBackground
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { isGridView = false },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isGridView) themeColors.onBackground else themeColors.surface
                                ) {
                                    Text("List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isGridView) themeColors.surface else themeColors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                                Surface(
                                    onClick = { isGridView = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isGridView) themeColors.onBackground else themeColors.surface
                                ) {
                                    Text("Grid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isGridView) themeColors.surface else themeColors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
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
                                    val pdfUriStr = pdf.uri.toString()
                                    val isFav = favoritedUris.contains(pdfUriStr)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onOpenDocument(pdf.uri) },
                                        elevation = CardDefaults.cardElevation(if (themeColors.isDark) 4.dp else 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                        border = BorderStroke(
                                            1.dp,
                                            if (themeColors.isDark) themeColors.cardBorder else Color(0xFFE2E8F0).copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Box {
                                                PdfCoverThumbnail(
                                                    uri = pdf.uri,
                                                    title = pdf.title,
                                                    cornerRadius = 10.dp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                )
                                                Surface(
                                                    onClick = { onToggleFavorite(pdfUriStr) },
                                                    shape = CircleShape,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    shadowElevation = 2.dp,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                            contentDescription = "Favorite",
                                                            tint = if (isFav) Color(0xFFEF4444) else Color(0xFF64748B),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(pdf.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(pdf.formattedSize, fontSize = 11.sp, color = themeColors.onSurfaceVariant)
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
                                onSharePdf = onSharePdf,
                                themeColors = themeColors
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
                                        Text("Book Reader · AI · Smart Notes", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B9C6)))
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
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface), shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Find documents on this device", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Grant permission to scan and display all PDF documents stored on your device.", style = MaterialTheme.typography.bodySmall.copy(color = themeColors.onSurfaceVariant))
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
    onSharePdf: (Uri, String) -> Unit,
    themeColors: ThemeColors = space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
) {
    var menuExpanded by remember { mutableStateOf(false) }
        Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(if (themeColors.isDark) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(
            1.dp,
            if (themeColors.isDark) themeColors.cardBorder else Color(0xFFE2E8F0).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PdfCoverThumbnail(
                    uri = pdf.uri,
                    title = pdf.title,
                    cornerRadius = 8.dp,
                    modifier = Modifier.size(38.dp, 48.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pdf.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = themeColors.onBackground), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pdf.formattedSize, style = MaterialTheme.typography.bodySmall.copy(color = themeColors.onSurfaceVariant))
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorited) Color(0xFFE74C3C) else themeColors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Options", tint = themeColors.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(themeColors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Document", color = themeColors.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Outlined.FileOpen, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(18.dp))
                        },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurface),
                        onClick = { menuExpanded = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isFavorited) "Remove Favorite" else "Add to Favorites", color = themeColors.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorited) Color(0xFFEF4444) else themeColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurface),
                        onClick = { menuExpanded = false; onToggleFavorite() }
                    )
                    DropdownMenuItem(
                        text = { Text("Document Info", color = themeColors.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(18.dp))
                        },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurface),
                        onClick = {
                            menuExpanded = false
                            val locationText = if (!pdf.filePath.isNullOrBlank()) {
    val emulatedPrefix = "/storage/emulated/0/"
    if (pdf.filePath.startsWith(emulatedPrefix)) {
        "Internal Storage > " + pdf.filePath.removePrefix(emulatedPrefix).replace("/", " > ")
    } else {
        pdf.filePath
    }
} else {
    "Internal Storage (Documents)"
}
onShowFileInfo(FileInfoState(pdf.title, locationText, pdf.formattedSize, pdf.formattedDate))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Document", color = themeColors.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(18.dp))
                        },
                        colors = MenuDefaults.itemColors(textColor = themeColors.onSurface),
                        onClick = { menuExpanded = false; onSharePdf(pdf.uri, pdf.title) }
                    )
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


