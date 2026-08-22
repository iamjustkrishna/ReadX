package space.iamjustkrishna.readx.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanningScreen(
    scannedCount: Int,
    progress: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    val creamBg = Color(0xFFF6F4EE)
    val purplePrimary = Color(0xFF6C5CE7)
    val purpleAccent = Color(0xFF8A70D6)
    val darkNavy = Color(0xFF1C2230)
    val mutedGray = Color(0xFF7A869A)

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scan_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(creamBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Glowing Squircle Badge
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = purplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(purplePrimary, purpleAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner soft ambient radial glow
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Icon(
                    imageVector = Icons.Outlined.ViewInAr,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Title
            Text(
                text = "Scanning your device...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = darkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle - Dynamic Document Count
            Text(
                text = if (scannedCount > 0) "Found $scannedCount documents" else "Searching for documents...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = mutedGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Sleek Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = purplePrimary,
                trackColor = Color(0xFFE2DFD8)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningScreenPreview() {
    ScanningScreen(scannedCount = 24, progress = 0.65f)
}
