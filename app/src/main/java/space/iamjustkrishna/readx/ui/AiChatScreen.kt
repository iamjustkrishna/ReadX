package space.iamjustkrishna.readx.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.ai.AiChatMessage
import space.iamjustkrishna.readx.reader.AiChatSessionState
import space.iamjustkrishna.readx.ui.theme.AppTheme
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors
import space.iamjustkrishna.readx.ui.theme.ThemeParticleBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    session: AiChatSessionState,
    savedSessions: List<space.iamjustkrishna.readx.data.SavedAiChatSession> = emptyList(),
    onResumeSession: (space.iamjustkrishna.readx.data.SavedAiChatSession) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    onSendMessage: (String) -> Unit,
    onOpenReader: (Uri) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors,
    currentTheme: AppTheme = AppTheme.SYSTEM_DEFAULT
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showHistoryDialog by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    if (showHistoryDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showHistoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chat History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                        IconButton(onClick = { showHistoryDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = themeColors.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (savedSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved chat history yet.",
                                fontSize = 13.sp,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedSessions) { saved ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onResumeSession(saved)
                                            showHistoryDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = saved.documentTitle,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${saved.messages.size} messages · ${android.text.format.DateUtils.getRelativeTimeSpanString(saved.lastUpdated)}",
                                                fontSize = 11.sp,
                                                color = themeColors.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteSession(saved.sessionId) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(session.messages.size, session.isLoading) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size - 1)
        }
    }

    val starterSuggestions = listOf(
        "Summarize this document",
        "Key takeaways & conclusions",
        "Explain the core concept",
        "Quiz me on this document"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = themeColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // ── Top App Bar ──────────────────────────────────────────────
            Surface(
                color = themeColors.surface.copy(alpha = if (themeColors.isDark) 0.85f else 1f),
                shadowElevation = if (themeColors.isDark) 0.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(themeColors.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.documentTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (session.pageCount > 0) "${session.pageCount} Pages · Context-Aware AI" else "PDF AI Assistant",
                            fontSize = 11.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Saved Chats",
                            tint = themeColors.primary
                        )
                    }

                    IconButton(onClick = { onOpenReader(session.documentUri) }) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = "Open in Reader",
                            tint = themeColors.primary
                        )
                    }
                }
            }
        },
        bottomBar = {
            // ── Bottom Input Bar ──────────────────────────────────────────
            // Uses navigationBars.union(ime) so that when keyboard is closed,
            // it sits above navigation bars; when keyboard opens, it sits
            // flush on top of the keyboard without any double-padding or gap.
            Surface(
                color = themeColors.surface.copy(alpha = if (themeColors.isDark) 0.9f else 1f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.ime)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask anything about this PDF...",
                                fontSize = 14.sp,
                                color = themeColors.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = themeColors.surfaceVariant,
                            unfocusedContainerColor = themeColors.surfaceVariant,
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = themeColors.cardBorder,
                            focusedTextColor = themeColors.onSurface,
                            unfocusedTextColor = themeColors.onSurface,
                            cursorColor = themeColors.primary
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !session.isLoading) {
                                    onSendMessage(inputText.trim())
                                    inputText = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !session.isLoading)
                                    themeColors.primary
                                else
                                    themeColors.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable(enabled = inputText.isNotBlank() && !session.isLoading) {
                                onSendMessage(inputText.trim())
                                inputText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated particle background for cosmic/nordic themes
            ThemeParticleBackground(theme = currentTheme)

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Error banner ──────────────────────────────────────────────
                AnimatedVisibility(visible = session.error != null) {
                    session.error?.let { err ->
                        Surface(
                            color = Color(0xFFFFF1F0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE74C3C),
                                    modifier = Modifier.weight(1f)
                                )
                                if (err.contains("API key", ignoreCase = true) || err.contains("AI Settings", ignoreCase = true)) {
                                    TextButton(onClick = onOpenSettings) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(14.dp), tint = themeColors.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Settings", fontSize = 12.sp, color = themeColors.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Messages List ─────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Prompt starter suggestions
                    item {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Quick Questions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(starterSuggestions) { suggestion ->
                                    Surface(
                                        onClick = {
                                            if (!session.isLoading) {
                                                onSendMessage(suggestion)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = themeColors.primaryContainer,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, themeColors.primary.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = themeColors.primary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(session.messages) { message ->
                        if (message.role == "user") {
                            UserMessageBubble(text = message.content, themeColors = themeColors)
                        } else {
                            AssistantMessageBubble(
                                message = message,
                                themeColors = themeColors,
                                onCopy = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("AI Response", message.content))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    if (session.isLoading) {
                        item {
                            AssistantGeneratingBubble(themeColors = themeColors)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(
    text: String,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 4.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(themeColors.primary)
                .padding(14.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = themeColors.onPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: AiChatMessage,
    themeColors: ThemeColors,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(themeColors.surface)
                .border(
                    1.dp,
                    themeColors.cardBorder,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(themeColors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = themeColors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ReadX AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = themeColors.onSurface,
                    lineHeight = 21.sp
                )

                if (message.referencedPages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sources:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurfaceVariant
                        )
                        message.referencedPages.forEach { pageNum ->
                            Surface(
                                color = themeColors.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Page $pageNum",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
private fun AssistantGeneratingBubble(themeColors: ThemeColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(themeColors.surface)
                .border(1.dp, themeColors.cardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = themeColors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Searching PDF pages & thinking...",
                    fontSize = 12.sp,
                    color = themeColors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
