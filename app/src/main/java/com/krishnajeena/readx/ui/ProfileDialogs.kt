package com.krishnajeena.readx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishnajeena.readx.data.ScannedPdf

// ═════════════════════════════════════════════════════════════
// 1. About App Dialog
// ═════════════════════════════════════════════════════════════

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6C5CE7), Color(0xFF8A70D6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("ReadX", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C2230))
                    Text("Version 1.0.0", fontSize = 12.sp, color = Color(0xFF7A869A))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ReadX is a modern, high-performance PDF reader & intelligent document assistant designed for seamless reading, smart notes, and contextual AI discussions.",
                    fontSize = 13.sp,
                    color = Color(0xFF4A5568),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Developed by Krish", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6C5CE7))
                Text("Powered by Android Jetpack Compose & Native PDF Engine.", fontSize = 12.sp, color = Color(0xFF7A869A))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// ═════════════════════════════════════════════════════════════
// 2. Storage Info Dialog (Real Storage Stats)
// ═════════════════════════════════════════════════════════════

@Composable
fun StorageInfoDialog(
    scannedPdfs: List<ScannedPdf>,
    onDismiss: () -> Unit
) {
    val totalSizeBytes = remember(scannedPdfs) {
        scannedPdfs.sumOf { it.sizeBytes }
    }
    val totalSizeMb = String.format("%.1f MB", totalSizeBytes / (1024f * 1024f))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEF5EA), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SdStorage, contentDescription = null, tint = Color(0xFFE67E22), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Storage Usage", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C2230))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFFF8F9FD),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Scanned Storage Index", fontSize = 12.sp, color = Color(0xFF7A869A))
                        Text(totalSizeMb, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C2230))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Indexed ${scannedPdfs.size} documents on device", fontSize = 13.sp, color = Color(0xFF6C5CE7), fontWeight = FontWeight.Medium)
                    }
                }
                Text("App data & cached PDF pages are stored securely in local app storage.", fontSize = 12.sp, color = Color(0xFF7A869A))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// ═════════════════════════════════════════════════════════════
// 3. Preferences Dialog
// ═════════════════════════════════════════════════════════════

@Composable
fun PreferencesDialog(onDismiss: () -> Unit) {
    var selectedTheme by remember { mutableStateOf("System") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEBF3FE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF2B7FFF), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C2230))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Theme", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A869A))
                listOf("System Default", "Light Mode", "Dark Mode").forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = selectedTheme == theme, onClick = { selectedTheme = theme })
                        Text(theme, fontSize = 14.sp, color = Color(0xFF1C2230))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B7FFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// ═════════════════════════════════════════════════════════════
// 4. AI Settings Dialog
// ═════════════════════════════════════════════════════════════

@Composable
fun AiSettingsDialog(onDismiss: () -> Unit) {
    var selectedModel by remember { mutableStateOf("Gemini Pro Reader") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEAF8F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF27AE60), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C2230))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI Reading Engine", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A869A))
                listOf("Gemini Pro Reader", "Local Lightweight Model", "Deep Document Summarizer").forEach { model ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = selectedModel == model, onClick = { selectedModel = model })
                        Text(model, fontSize = 14.sp, color = Color(0xFF1C2230))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
