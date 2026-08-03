package com.krishnajeena.readx.reader

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.krishnajeena.readx.ai.AiService

@Composable
fun AiDialog(
    selectedText: String,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val aiResult by viewModel.aiResult.collectAsState()
    val settingsRepo = viewModel.settingsRepo
    var customPrompt by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val translateLang = settingsRepo.getTranslateLanguage()

    Dialog(onDismissRequest = {
        viewModel.clearAiResult()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Assistant ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = {
                        viewModel.clearAiResult()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Selected text preview
                Text(
                    text = "\"$selectedText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Quick Action Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.runAiAction(selectedText, AiService.PROMPT_SIMPLIFY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Simplify", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runAiAction(selectedText, AiService.translatePrompt(translateLang)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text(translateLang, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runAiAction(selectedText, AiService.PROMPT_EXPLAIN) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Text("Explain", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Custom Prompt Toggle
                if (!showCustomInput) {
                    TextButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Custom prompt...", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            placeholder = { Text("Ask AI something about this text...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (customPrompt.isNotBlank()) {
                                    viewModel.runCustomAiAction(selectedText, customPrompt)
                                }
                            },
                            enabled = customPrompt.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send prompt")
                        }
                    }
                }

                // Result Area
                when {
                    aiResult.isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    aiResult.error != null -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = aiResult.error!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (aiResult.error == "Set your API key in Settings") {
                                    TextButton(onClick = {
                                        onDismiss()
                                        onNavigateToSettings()
                                    }) {
                                        Text("Open Settings")
                                    }
                                }
                            }
                        }
                    }

                    aiResult.result != null -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = aiResult.result!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI Output", aiResult.result))
                                        Toast.makeText(context, "Copied AI response", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy AI response",
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
