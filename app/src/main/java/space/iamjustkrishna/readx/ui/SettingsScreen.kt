package space.iamjustkrishna.readx.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.data.AiProvider
import space.iamjustkrishna.readx.data.SettingsRepository
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    onBack: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    var selectedProvider by remember { mutableStateOf(settingsRepo.getProvider()) }
    var groqApiKey by remember { mutableStateOf(settingsRepo.getGroqApiKey() ?: "") }
    var geminiApiKey by remember { mutableStateOf(settingsRepo.getGeminiApiKey() ?: "") }
    var openAiApiKey by remember { mutableStateOf(settingsRepo.getOpenAiApiKey() ?: "") }
    var anthropicApiKey by remember { mutableStateOf(settingsRepo.getAnthropicApiKey() ?: "") }

    var selectedLang by remember { mutableStateOf(settingsRepo.getTranslateLanguage()) }
    var selectedModel by remember { mutableStateOf(settingsRepo.getAiModel()) }

    var apiKeyVisible by remember { mutableStateOf(false) }

    var langDropdownExpanded by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var savedMessage by remember { mutableStateOf(false) }

    val availableModels = settingsRepo.getModelsFor(selectedProvider)

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = themeColors.surfaceVariant,
        unfocusedContainerColor = themeColors.surfaceVariant,
        focusedTextColor = themeColors.onSurface,
        unfocusedTextColor = themeColors.onSurface,
        focusedBorderColor = themeColors.primary,
        unfocusedBorderColor = themeColors.cardBorder,
        cursorColor = themeColors.primary,
        focusedLabelColor = themeColors.primary,
        unfocusedLabelColor = themeColors.onSurfaceVariant,
        focusedTrailingIconColor = themeColors.primary,
        unfocusedTrailingIconColor = themeColors.onSurfaceVariant
    )

    Scaffold(
        containerColor = themeColors.background,
        topBar = {
            TopAppBar(
                title = { Text("AI Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = themeColors.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = themeColors.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.cardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Text(
                        text = "Choose which intelligence engine powers summaries, explanations, translations, and chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(AiProvider.entries) { provider ->
                            val isSelected = selectedProvider == provider
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedProvider = provider
                                    selectedModel = settingsRepo.getDefaultModelFor(provider)
                                },
                                label = {
                                    Text(
                                        text = provider.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) themeColors.primary else themeColors.onSurfaceVariant
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = themeColors.primary,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeColors.primaryContainer,
                                    containerColor = themeColors.surfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) themeColors.primary else themeColors.cardBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Provider Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.cardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "${selectedProvider.displayName} Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )

                    when (selectedProvider) {
                        AiProvider.GROQ -> {
                            OutlinedTextField(
                                value = groqApiKey,
                                onValueChange = { groqApiKey = it.trim() },
                                label = { Text("Groq API Key (Optional)") },
                                placeholder = { Text("Using built-in preconfigured key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = tfColors,
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility",
                                            tint = themeColors.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            Text(
                                text = if (groqApiKey.isBlank()) "\u2713 Using built-in default app key. Enter custom key above to override."
                                else "Using custom Groq API key.",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.primary
                            )
                        }
                        AiProvider.GEMINI -> {
                            OutlinedTextField(
                                value = geminiApiKey,
                                onValueChange = { geminiApiKey = it.trim() },
                                label = { Text("Google Gemini API Key (AIzaSy...)") },
                                placeholder = { Text("Paste Google AI Studio API key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = tfColors,
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility",
                                            tint = themeColors.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get a free Gemini API key at aistudio.google.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                        AiProvider.OPENAI -> {
                            OutlinedTextField(
                                value = openAiApiKey,
                                onValueChange = { openAiApiKey = it.trim() },
                                label = { Text("OpenAI API Key (sk-proj-...)") },
                                placeholder = { Text("Paste OpenAI API key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = tfColors,
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility",
                                            tint = themeColors.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get an API key at platform.openai.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                        AiProvider.ANTHROPIC -> {
                            OutlinedTextField(
                                value = anthropicApiKey,
                                onValueChange = { anthropicApiKey = it.trim() },
                                label = { Text("Anthropic API Key (sk-ant-...)") },
                                placeholder = { Text("Paste Anthropic API key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = tfColors,
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility",
                                            tint = themeColors.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get an API key at console.anthropic.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                    }

                    // Model Selector Dropdown & Manual Input
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it.trim() },
                            readOnly = false,
                            label = { Text("Model Version Code") },
                            placeholder = { Text("e.g. openai/gpt-oss-120b") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = tfColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            availableModels.forEach { (modelKey, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = themeColors.onSurface)
                                            Text(modelKey, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        selectedModel = modelKey
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = "Type any custom model code manually or select a preset from the menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurfaceVariant
                    )

                    // Translate Language Selector
                    ExposedDropdownMenuBox(
                        expanded = langDropdownExpanded,
                        onExpandedChange = { langDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLang,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Default Translate Language") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = tfColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = langDropdownExpanded,
                            onDismissRequest = { langDropdownExpanded = false }
                        ) {
                            SettingsRepository.AVAILABLE_LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = themeColors.onSurface) },
                                    onClick = {
                                        selectedLang = lang
                                        langDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    settingsRepo.setProvider(selectedProvider)
                    if (groqApiKey.isBlank()) settingsRepo.clearGroqApiKey() else settingsRepo.setGroqApiKey(groqApiKey)
                    if (geminiApiKey.isBlank()) settingsRepo.clearGeminiApiKey() else settingsRepo.setGeminiApiKey(geminiApiKey)
                    if (openAiApiKey.isBlank()) settingsRepo.clearOpenAiApiKey() else settingsRepo.setOpenAiApiKey(openAiApiKey)
                    if (anthropicApiKey.isBlank()) settingsRepo.clearAnthropicApiKey() else settingsRepo.setAnthropicApiKey(anthropicApiKey)
                    settingsRepo.setTranslateLanguage(selectedLang)
                    settingsRepo.setAiModel(selectedModel)
                    savedMessage = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.primary,
                    contentColor = themeColors.onPrimary
                )
            ) {
                Text(
                    text = if (savedMessage) "Saved Successfully! âœ“" else "Save Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = themeColors.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
