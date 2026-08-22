package space.iamjustkrishna.readx.reader

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import space.iamjustkrishna.readx.ai.AiService
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

@Composable
fun AiDialog(
    selectedText: String,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
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
            color = themeColors.surface,
            border = BorderStroke(1.dp, themeColors.cardBorder),
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
                        text = "AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primary
                    )
                    IconButton(onClick = {
                        viewModel.clearAiResult()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = themeColors.onSurfaceVariant)
                    }
                }

                // Selected text preview
                Text(
                    text = "\"\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showCustomInput = false
                            viewModel.runAiAction(selectedText, AiService.PROMPT_EXPLAIN)
                        },
                        border = BorderStroke(1.dp, themeColors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Explain", color = themeColors.onSurface, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            showCustomInput = false
                            viewModel.runAiAction(selectedText, AiService.PROMPT_SIMPLIFY)
                        },
                        border = BorderStroke(1.dp, themeColors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Summarize", color = themeColors.onSurface, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            showCustomInput = false
                            viewModel.runAiAction(selectedText, AiService.translatePrompt(translateLang))
                        },
                        border = BorderStroke(1.dp, themeColors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(translateLang, color = themeColors.onSurface, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Custom prompt toggle
                TextButton(
                    onClick = { showCustomInput = !showCustomInput },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        if (showCustomInput) "Hide custom prompt" else "Custom question...",
                        color = themeColors.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (showCustomInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            placeholder = { Text("Ask anything about this text...", color = themeColors.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = themeColors.surfaceVariant,
                                unfocusedContainerColor = themeColors.surfaceVariant,
                                focusedTextColor = themeColors.onSurface,
                                unfocusedTextColor = themeColors.onSurface,
                                focusedBorderColor = themeColors.primary,
                                unfocusedBorderColor = themeColors.cardBorder
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (customPrompt.isNotBlank()) {
                                    viewModel.runCustomAiAction(selectedText, customPrompt)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = themeColors.primary)
                        }
                    }
                }

                // Result / Loading / Error display
                when {
                    aiResult.isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = themeColors.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Thinking...", style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
                        }
                    }
                    aiResult.error != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = aiResult.error ?: "Unknown error",
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (aiResult.error?.contains("API key") == true) {
                                TextButton(onClick = {
                                    onDismiss()
                                    onNavigateToSettings()
                                }) {
                                    Text("Open Settings", color = themeColors.primary)
                                }
                            }
                        }
                    }
                    aiResult.result != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = aiResult.result ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("AI Result", aiResult.result)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = themeColors.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
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