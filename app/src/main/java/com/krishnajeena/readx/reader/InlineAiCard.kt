package com.krishnajeena.readx.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krishnajeena.readx.ai.AiService
import kotlinx.coroutines.launch

/**
 * Compact inline AI response card anchored directly next to the floating selection toolbar.
 * Eliminates separate fullscreen modal dialogs and incorporates surrounding context for short phrase selections.
 */
@Composable
fun InlineAiCard(
    selectionState: SelectionUiState,
    viewModel: ReaderViewModel,
    onClose: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiResult by viewModel.aiResult.collectAsState()
    val translateLang = remember { viewModel.settingsRepo.getTranslateLanguage() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedAction by remember { mutableStateOf("Explain") }
    var selectedText by remember { mutableStateOf("") }
    var contextText by remember { mutableStateOf("") }

    // Fetch text and surrounding context on launch
    LaunchedEffect(selectionState.selection) {
        val (selText, ctxText) = viewModel.textWithContextFor(selectionState.selection)
        selectedText = selText
        contextText = ctxText

        // Trigger default Explain action with surrounding context
        viewModel.runAiAction(selText, AiService.PROMPT_EXPLAIN, ctxText)
    }

    Surface(
        modifier = modifier
            .widthIn(max = 310.dp)
            .padding(top = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Action Chips + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedAction == "Explain",
                        onClick = {
                            selectedAction = "Explain"
                            viewModel.runAiAction(selectedText, AiService.PROMPT_EXPLAIN, contextText)
                        },
                        label = { Text("Explain", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = selectedAction == "Simplify",
                        onClick = {
                            selectedAction = "Simplify"
                            viewModel.runAiAction(selectedText, AiService.PROMPT_SIMPLIFY, contextText)
                        },
                        label = { Text("Simplify", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = selectedAction == translateLang,
                        onClick = {
                            selectedAction = translateLang
                            viewModel.runAiAction(selectedText, AiService.translatePrompt(translateLang), contextText)
                        },
                        label = { Text(translateLang, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close AI",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Body Response Content
            when {
                aiResult.isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                aiResult.error != null -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = aiResult.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (aiResult.error?.contains("API key") == true) {
                                TextButton(
                                    onClick = onNavigateToSettings,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                aiResult.result != null -> {
                    val resultText = aiResult.result!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = resultText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Explanation", resultText))
                                        Toast.makeText(context, "Copied AI explanation", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {

                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy result",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
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
