package com.krishnajeena.readx.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════
// AnimatedSplashScreen — Final Composable
// ═════════════════════════════════════════════════════════════

@Composable
fun AnimatedSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSplashState()

    LaunchedEffect(Unit) {
        state.playEntrance()
        delay(1600)
        state.playExit()
        delay(500)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Ambient animated orbs
        MeshBackground(
            isActive = state.isBackgroundActive,
            primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
            accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f)
        )

        // Main content with exit animation
        AnimatedVisibility(
            visible = state.isContentVisible,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(500)) + shrinkOut(
                animationSpec = tween(500),
                shrinkTowards = Alignment.Center
            )
        ) {
            SplashContent(
                iconScale = state.iconScale,
                iconRotation = state.iconRotation,
                titleProgress = state.titleProgress,
                taglineProgress = state.taglineProgress,
                badgeProgress = state.badgeProgress
            )
        }

        // Bottom loading dots
        AnimatedVisibility(
            visible = state.isLoadingVisible,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            ) {
                LoadingDots(
                    isActive = state.isLoadingActive,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// State
// ═════════════════════════════════════════════════════════════

@Stable
class SplashState {
    var isContentVisible by mutableStateOf(true)
    var isBackgroundActive by mutableStateOf(true)
    var isLoadingVisible by mutableStateOf(false)
    var isLoadingActive by mutableStateOf(false)

    var iconScale by mutableFloatStateOf(0f)
    var iconRotation by mutableFloatStateOf(-20f)
    var titleProgress by mutableFloatStateOf(0f)
    var taglineProgress by mutableFloatStateOf(0f)
    var badgeProgress by mutableFloatStateOf(0f)

    suspend fun playEntrance() {
        isLoadingVisible = true

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f)
        ) { value, _ -> iconScale = value }

        animate(
            initialValue = -20f,
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 220f)
        ) { value, _ -> iconRotation = value }

        delay(200)

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        ) { value, _ -> titleProgress = value }

        delay(140)

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearOutSlowInEasing)
        ) { value, _ -> taglineProgress = value }

        delay(240)

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f)
        ) { value, _ -> badgeProgress = value }

        isLoadingActive = true
    }

    suspend fun playExit() {
        isLoadingActive = false
        delay(150)
        isLoadingVisible = false
        isBackgroundActive = false
        delay(200)
        isContentVisible = false
    }
}

@Composable
fun rememberSplashState(): SplashState = remember { SplashState() }

// ═════════════════════════════════════════════════════════════
// Content
// ═════════════════════════════════════════════════════════════

@Composable
private fun SplashContent(
    iconScale: Float,
    iconRotation: Float,
    titleProgress: Float,
    taglineProgress: Float,
    badgeProgress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Logo with glow
        Box(
            modifier = Modifier
                .scale(iconScale)
                .graphicsLayer { rotationZ = iconRotation },
            contentAlignment = Alignment.Center
        ) {
            // Soft radial glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .blur(56.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = "ReadX",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Brand name — clean, no weird spacing
        Text(
            text = "ReadX",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    blurRadius = 20f * titleProgress,
                    offset = Offset(0f, 4f * titleProgress)
                ),
                textMotion = TextMotion.Animated
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(titleProgress)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tagline — broader than just PDF
        Text(
            text = "Your Intelligent Reading Space",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(taglineProgress)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // AI Integrated badge
        AIBadge(progress = badgeProgress)
    }
}

// ═════════════════════════════════════════════════════════════
// AI Integrated Badge
// ═════════════════════════════════════════════════════════════

@Composable
private fun AIBadge(progress: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(
            alpha = (progress * 0.9f).coerceIn(0f, 1f)
        ),
        modifier = modifier
            .scale(progress)
            .alpha(progress)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            // Animated dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(if (progress > 0.8f) pulse else 1f)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Integrated",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Mesh Background
// ═════════════════════════════════════════════════════════════

@Composable
private fun MeshBackground(
    isActive: Boolean,
    primary: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "p1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "p2"
    )

    if (!isActive) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(100.dp)
    ) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = primary,
            radius = w * 0.45f,
            center = Offset(
                w * 0.3f + sin(phase1) * w * 0.15f,
                h * 0.4f + cos(phase1 * 0.6f) * h * 0.2f
            )
        )
        drawCircle(
            color = accent,
            radius = w * 0.4f,
            center = Offset(
                w * 0.7f + sin(phase2 * 0.7f) * w * 0.18f,
                h * 0.6f + cos(phase2) * h * 0.15f
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════
// Loading Dots
// ═════════════════════════════════════════════════════════════

@Composable
private fun LoadingDots(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val anims = List(3) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, delayMillis = i * 130),
                repeatMode = RepeatMode.Reverse
            ),
            label = "d$i"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        anims.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(anim.value)
                    .background(
                        color = color.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
fun SplashScreenFinalPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            MeshBackground(
                isActive = true,
                primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f)
            )

            SplashContent(
                iconScale = 1f,
                iconRotation = 0f,
                titleProgress = 1f,
                taglineProgress = 1f,
                badgeProgress = 1f
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            ) {
                LoadingDots(
                    isActive = true,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}