package space.iamjustkrishna.readx.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import space.iamjustkrishna.readx.ai.AiService
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

@Composable
fun InlineAiCard(
    selectionState: SelectionUiState,
    viewModel: ReaderViewModel,
    onClose: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    themeColors: ThemeColors = MinimalistLightColors
) {
    val aiResult by viewModel.aiResult.collectAsState()
    val translateLang = remember { viewModel.settingsRepo.getTranslateLanguage() }
    val context = LocalContext.current

    var selectedAction by remember { mutableStateOf("Explain") }
    var selectedText by remember { mutableStateOf("") }
    var contextText by remember { mutableStateOf("") }

    LaunchedEffect(selectionState.selection) {
        val (selText, ctxText) = viewModel.textWithContextFor(selectionState.selection)
        selectedText = selText
        contextText = ctxText
        viewModel.runAiAction(selText, AiService.PROMPT_EXPLAIN, ctxText)
    }

    Surface(
        modifier = modifier.widthIn(max = 440.dp),
        shape = RoundedCornerShape(18.dp),
        color = themeColors.surface,
        border = BorderStroke(1.dp, themeColors.cardBorder),
        shadowElevation = 10.dp,
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.primaryContainer,
                            selectedLabelColor = themeColors.primary,
                            containerColor = themeColors.surfaceVariant,
                            labelColor = themeColors.onSurface
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = selectedAction == "Simplify",
                        onClick = {
                            selectedAction = "Simplify"
                            viewModel.runAiAction(selectedText, AiService.PROMPT_SIMPLIFY, contextText)
                        },
                        label = { Text("Simplify", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.primaryContainer,
                            selectedLabelColor = themeColors.primary,
                            containerColor = themeColors.surfaceVariant,
                            labelColor = themeColors.onSurface
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = selectedAction == translateLang,
                        onClick = {
                            selectedAction = translateLang
                            viewModel.runAiAction(selectedText, AiService.translatePrompt(translateLang), contextText)
                        },
                        label = { Text(translateLang, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.primaryContainer,
                            selectedLabelColor = themeColors.primary,
                            containerColor = themeColors.surfaceVariant,
                            labelColor = themeColors.onSurface
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = themeColors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // AI Response Body
            when {
                aiResult.isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = themeColors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyzing selection...",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }

                aiResult.error != null -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = aiResult.error ?: "Error processing AI request",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444)
                        )
                        if (aiResult.error?.contains("API key") == true) {
                            TextButton(
                                onClick = onNavigateToSettings,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Open Settings", color = themeColors.primary, style = MaterialTheme.typography.labelMedium)
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
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = aiResult.result ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Result", aiResult.result))
                                        Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy result",
                                        tint = themeColors.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
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