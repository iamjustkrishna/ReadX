package com.krishnajeena.readx.ui

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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.krishnajeena.readx.data.AiProvider
import com.krishnajeena.readx.data.SettingsRepository


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    onBack: () -> Unit
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

    val availableModels = when (selectedProvider) {
        AiProvider.GROQ -> SettingsRepository.GROQ_MODELS
        AiProvider.GEMINI -> SettingsRepository.GEMINI_MODELS
        AiProvider.OPENAI -> SettingsRepository.OPENAI_MODELS
        AiProvider.ANTHROPIC -> SettingsRepository.ANTHROPIC_MODELS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // AI Provider Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "AI Reading Assistant ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Choose your preferred AI provider. Keys are encrypted and stored on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Provider Filter Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        // Adds space between items
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        // Adds space at the start and end of the scrollable area
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        items(
                            items = AiProvider.entries,
                            key = { it.name } // Improving performance by providing a stable key
                        ) { provider ->
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    selectedModel = provider.defaultModel
                                },
                                label = {
                                    Text(text = provider.displayName)
                                }
                            )
                        }
                    }

                    // API Key Field based on Provider
                    when (selectedProvider) {
                        AiProvider.GROQ -> {
                            OutlinedTextField(
                                value = groqApiKey,
                                onValueChange = { groqApiKey = it.trim() },
                                label = { Text("Groq API Key (gsk_...) - Optional", style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("Using built-in default app key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                }
                            )
                            Text(
                                text = if (groqApiKey.isBlank()) "✓ Using built-in default app key. Enter custom key above to override."
                                else "Using custom Groq API key.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
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
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get a free Gemini API key at aistudio.google.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
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
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get an API key at platform.openai.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
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
                                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Get an API key at console.anthropic.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
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
                            placeholder = { Text("e.g. llama-3.3-70b-versatile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
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
                                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(modelKey, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = langDropdownExpanded,
                            onDismissRequest = { langDropdownExpanded = false }
                        ) {
                            SettingsRepository.AVAILABLE_LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (savedMessage) "Saved! ✓" else "Save Settings")
            }

            // App info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ReadX v1.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Serene Book Reader", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
