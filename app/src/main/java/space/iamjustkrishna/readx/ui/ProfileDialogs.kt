package space.iamjustkrishna.readx.ui

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.iamjustkrishna.readx.R
import space.iamjustkrishna.readx.data.ScannedPdf
import space.iamjustkrishna.readx.ui.theme.MinimalistLightColors
import space.iamjustkrishna.readx.ui.theme.ThemeColors

// ------------------------------------------------------------------
// 1. About App Dialog
// ------------------------------------------------------------------

@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.readx_logo),
                        contentDescription = "ReadX Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("ReadX", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = themeColors.onSurface)
                    Text("Version 1.0.0", fontSize = 12.sp, color = themeColors.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ReadX is a modern, high-performance PDF reader and intelligent document assistant designed for fluid reading, smart notes, and context-aware AI chat.",
                    fontSize = 13.sp,
                    color = themeColors.onSurface,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text("Crafted by Krish", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.primary)
                Text("Native PDF Engine with Jetpack Compose & Material 3.", fontSize = 12.sp, color = themeColors.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.onPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = themeColors.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = themeColors.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

// ------------------------------------------------------------------
// 2. Storage Info Dialog (Real Storage Stats + Device Stats)
// ------------------------------------------------------------------

@Composable
fun StorageInfoDialog(
    scannedPdfs: List<ScannedPdf>,
    onDismiss: () -> Unit,
    themeColors: ThemeColors = MinimalistLightColors
) {
    val totalSizeBytes = remember(scannedPdfs) {
        scannedPdfs.sumOf { it.sizeBytes }
    }
    val totalSizeFormatted = remember(totalSizeBytes) {
        if (totalSizeBytes >= 1024L * 1024L * 1024L) {
            String.format("%.2f GB", totalSizeBytes / (1024f * 1024f * 1024f))
        } else {
            String.format("%.1f MB", totalSizeBytes / (1024f * 1024f))
        }
    }

    val (deviceFreeFormatted, deviceTotalFormatted) = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            val freeBytes = availableBlocks * blockSize
            val totalBytes = totalBlocks * blockSize
            val freeStr = String.format("%.1f GB", freeBytes / (1024f * 1024f * 1024f))
            val totalStr = String.format("%.1f GB", totalBytes / (1024f * 1024f * 1024f))
            freeStr to totalStr
        }.getOrDefault("Available" to "Device Storage")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(themeColors.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SdStorage,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text("Storage Usage", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = themeColors.onSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // PDF Library Storage Card
                Surface(
                    color = themeColors.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PDF Library Storage", fontSize = 12.sp, color = themeColors.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(totalSizeFormatted, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = themeColors.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Indexed ${scannedPdfs.size} documents on your device", fontSize = 13.sp, color = themeColors.primary, fontWeight = FontWeight.Medium)
                    }
                }

                // Device Hardware Storage Card
                Surface(
                    color = themeColors.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Device Internal Storage", fontSize = 12.sp, color = themeColors.onSurfaceVariant)
                            Text("Free: $deviceFreeFormatted / Total: $deviceTotalFormatted", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = themeColors.onSurface)
                        }
                    }
                }

                Text("PDFs are indexed and read directly from device storage without duplicating, modifying, or deleting any files.", fontSize = 12.sp, color = themeColors.onSurfaceVariant, lineHeight = 17.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.onPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK", color = themeColors.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = themeColors.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
