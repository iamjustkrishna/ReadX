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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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

    var showSplash by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var aiDialogText by remember { mutableStateOf<String?>(null) }
    var fileInfoState by remember { mutableStateOf<FileInfoState?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var hasStoragePermission by remember {
        mutableStateOf(context.hasDocPermissions())
    }

    val notesSheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasStoragePermission = granted
        if (granted) {
            viewModel.scanDevicePdfs()
        }
    }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                context.persistReadPermission(uri)
                viewModel.openDocument(uri)
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

    if (showSettings) {
        SettingsScreen(
            settingsRepo = viewModel.settingsRepo,
            onBack = { showSettings = false }
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


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val current = state) {
                            is ReaderUiState.Document -> current.document.displayName
                            else -> "ReadX"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (state is ReaderUiState.Document) {
                        val showGlyphBoxes by viewModel.showGlyphBoxes.collectAsState()
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
                        TextButton(onClick = { viewModel.toggleGlyphBoxes() }) {
                            Text(
                                text = "Aa",
                                fontWeight = FontWeight.Bold,
                                color = if (showGlyphBoxes) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            isSearchExpanded = false
                            viewModel.closeDocument()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close document",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (state is ReaderUiState.Home) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when (val current = state) {
            is ReaderUiState.Home -> HomeScreen(
                padding = padding,
                recent = current.recent,
                scannedPdfs = scannedPdfs,
                hasStoragePermission = hasStoragePermission,
                onRequestPermission = {
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
                onOpenPicker = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                onOpenDocument = { uri -> viewModel.openDocument(uri) },
                onViewNotes = { recent ->
                    viewModel.openDocument(recent.uri)
                    showNotesSheet = true
                },
                onShowFileInfo = { info -> fileInfoState = info },
                onSharePdf = { uri, title -> sharePdf(context, uri, title) }
            )


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
private fun HomeScreen(
    padding: PaddingValues,
    recent: RecentDocument?,
    scannedPdfs: List<ScannedPdf>,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenDocument: (Uri) -> Unit,
    onViewNotes: (RecentDocument) -> Unit,
    onShowFileInfo: (FileInfoState) -> Unit,
    onSharePdf: (Uri, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var recentMenuExpanded by remember { mutableStateOf(false) }

    val filteredScannedPdfs = remember(scannedPdfs, searchQuery) {
        if (searchQuery.isBlank()) scannedPdfs
        else scannedPdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ReadX",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Glyph-exact PDF Reader • AI Assistant • Smart Notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onOpenPicker,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open File Picker")
                    }
                }
            }
        }

        // Permission Banner
        if (!hasStoragePermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Find documents on this device",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant permission to scan and display all PDF documents stored on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Show Device PDFs")
                        }
                    }
                }
            }
        }

        // Recent Document Section
        if (recent != null) {
            item {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDocument(recent.uri) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = recent.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            IconButton(onClick = { recentMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = recentMenuExpanded,
                                onDismissRequest = { recentMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open") },
                                    onClick = {
                                        recentMenuExpanded = false
                                        onOpenDocument(recent.uri)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Info ℹ️") },
                                    onClick = {
                                        recentMenuExpanded = false
                                        onShowFileInfo(
                                            FileInfoState(
                                                title = recent.displayName,
                                                uriString = recent.uri.toString(),
                                                formattedSize = "Document",
                                                formattedDate = ""
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share 📤") },
                                    onClick = {
                                        recentMenuExpanded = false
                                        onSharePdf(recent.uri, recent.displayName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View Notes & Highlights 📝") },
                                    onClick = {
                                        recentMenuExpanded = false
                                        onViewNotes(recent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Device PDFs Section
        if (hasStoragePermission) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device Documents (${filteredScannedPdfs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (scannedPdfs.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search device PDFs...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            if (filteredScannedPdfs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No PDFs match \"$searchQuery\"" else "Scanning device for PDFs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredScannedPdfs, key = { it.uri.toString() }) { pdf ->
                    ScannedPdfItem(
                        pdf = pdf,
                        onClick = { onOpenDocument(pdf.uri) },
                        onShowFileInfo = onShowFileInfo,
                        onSharePdf = onSharePdf
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannedPdfItem(
    pdf: ScannedPdf,
    onClick: () -> Unit,
    onShowFileInfo: (FileInfoState) -> Unit,
    onSharePdf: (Uri, String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pdf.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("File Info ℹ️") },
                        onClick = {
                            menuExpanded = false
                            onShowFileInfo(
                                FileInfoState(
                                    title = pdf.title,
                                    uriString = pdf.uri.toString(),
                                    formattedSize = pdf.formattedSize,
                                    formattedDate = pdf.formattedDate
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share 📤") },
                        onClick = {
                            menuExpanded = false
                            onSharePdf(pdf.uri, pdf.title)
                        }
                    )
                }
            }
        }
    }
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

private fun Context.hasDocPermissions(): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager() ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredPermissions(): Array<String> {
    return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}



private fun Context.persistReadPermission(uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("PDF text", text))
}
