package space.iamjustkrishna.readx.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AppTheme(val displayName: String, val subtitle: String) {
    SYSTEM_DEFAULT("Minimalist Light", "Clean, modern adaptive light theme"),
    COSMIC_SPACE("Cosmic Space", "Deep dark obsidian with starlight particles"),
    NORDIC_FOREST("Nordic Forest", "Deep pine emerald with fresh mint accents"),
    WARM_SEPIA("Warm Sepia", "Warm cream parchment for eye comfort")
}

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val cardBorder: Color = Color.Transparent,
    val isDark: Boolean = false
)

val MinimalistLightColors = ThemeColors(
    background = Color(0xFFF8F9FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B),
    primary = Color(0xFF5B4DDB),
    primaryContainer = Color(0xFFEEF2FF),
    onPrimary = Color.White,
    cardBorder = Color(0xFFE2E8F0).copy(alpha = 0.5f),
    isDark = false
)

val CosmicSpaceColors = ThemeColors(
    background = Color(0xFF070B14),
    surface = Color(0xFF0F1829),
    surfaceVariant = Color(0xFF1A2540),
    onBackground = Color(0xFFF0F4FF),
    onSurface = Color(0xFFE8EEFF),
    onSurfaceVariant = Color(0xFF9BAACF),
    primary = Color(0xFF818CF8),
    primaryContainer = Color(0xFF1E2456),
    onPrimary = Color.White,
    cardBorder = Color(0xFF2D3F6E).copy(alpha = 0.7f),
    isDark = true
)

val NordicForestColors = ThemeColors(
    background = Color(0xFF060F0B),
    surface = Color(0xFF0D1F17),
    surfaceVariant = Color(0xFF183328),
    onBackground = Color(0xFFEDFDF5),
    onSurface = Color(0xFFD8FCE8),
    onSurfaceVariant = Color(0xFF7DDBA8),
    primary = Color(0xFF10B981),
    primaryContainer = Color(0xFF053822),
    onPrimary = Color.White,
    cardBorder = Color(0xFF0B5E37).copy(alpha = 0.5f),
    isDark = true
)

val WarmSepiaColors = ThemeColors(
    background = Color(0xFFFAF6EE),
    surface = Color(0xFFFFFDF9),
    surfaceVariant = Color(0xFFF2ECE0),
    onBackground = Color(0xFF2D2319),
    onSurface = Color(0xFF3D3226),
    onSurfaceVariant = Color(0xFF7C6D5D),
    primary = Color(0xFFB45309),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimary = Color.White,
    cardBorder = Color(0xFFE6DCB8).copy(alpha = 0.6f),
    isDark = false
)

fun getThemeColors(theme: AppTheme): ThemeColors = when (theme) {
    AppTheme.SYSTEM_DEFAULT -> MinimalistLightColors
    AppTheme.COSMIC_SPACE -> CosmicSpaceColors
    AppTheme.NORDIC_FOREST -> NordicForestColors
    AppTheme.WARM_SEPIA -> WarmSepiaColors
}

// ──────────────────────────────────────────────────────────────
// Star particle data
// ──────────────────────────────────────────────────────────────

private enum class StarLayer { TINY, MID, BRIGHT }

private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alphaBase: Float,
    val phaseOffset: Float,    // individual phase offset → independent twinkle
    val twinkleSpeed: Float,
    val color: Color,
    val layer: StarLayer
)

@Composable
fun CosmicSpaceParticleCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 90
) {
    val particles = remember {
        val rng = Random(42)
        val tinyCount = (particleCount * 0.55f).toInt()
        val midCount  = (particleCount * 0.35f).toInt()
        val brightCount = particleCount - tinyCount - midCount

        val starColors = listOf(
            Color(0xFFFFFFFF),   // pure white
            Color(0xFFBDD4FF),   // pale blue
            Color(0xFFD9D4FF),   // pale indigo
            Color(0xFFFFF8D6),   // warm star-white
        )

        buildList {
            repeat(tinyCount) {
                add(StarParticle(
                    xRatio = rng.nextFloat(),
                    yRatio = rng.nextFloat(),
                    radius = rng.nextFloat() * 0.7f + 0.4f,
                    alphaBase = rng.nextFloat() * 0.4f + 0.15f,
                    phaseOffset = rng.nextFloat() * 2 * PI.toFloat(),
                    twinkleSpeed = rng.nextFloat() * 0.8f + 0.4f,
                    color = starColors[rng.nextInt(starColors.size)],
                    layer = StarLayer.TINY
                ))
            }
            repeat(midCount) {
                add(StarParticle(
                    xRatio = rng.nextFloat(),
                    yRatio = rng.nextFloat(),
                    radius = rng.nextFloat() * 1.1f + 1.3f,
                    alphaBase = rng.nextFloat() * 0.45f + 0.25f,
                    phaseOffset = rng.nextFloat() * 2 * PI.toFloat(),
                    twinkleSpeed = rng.nextFloat() * 0.6f + 0.3f,
                    color = starColors[rng.nextInt(starColors.size)],
                    layer = StarLayer.MID
                ))
            }
            repeat(brightCount) {
                add(StarParticle(
                    xRatio = rng.nextFloat(),
                    yRatio = rng.nextFloat(),
                    radius = rng.nextFloat() * 1.5f + 2.8f,
                    alphaBase = rng.nextFloat() * 0.35f + 0.55f,
                    phaseOffset = rng.nextFloat() * 2 * PI.toFloat(),
                    twinkleSpeed = rng.nextFloat() * 0.4f + 0.2f,
                    color = Color.White,
                    layer = StarLayer.BRIGHT
                ))
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val globalPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val dynamicAlpha = (p.alphaBase + 0.38f * sin((globalPhase * p.twinkleSpeed + p.phaseOffset).toDouble()).toFloat())
                .coerceIn(0.05f, 1f)
            drawCircle(
                color = p.color.copy(alpha = dynamicAlpha),
                radius = p.radius,
                center = Offset(p.xRatio * w, p.yRatio * h)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Nordic Forest — glowing green firefly particles
// ──────────────────────────────────────────────────────────────

private data class FireflyParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alphaBase: Float,
    val phaseOffset: Float,
    val speed: Float
)

@Composable
fun NordicForestParticleCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 40
) {
    val particles = remember {
        val rng = Random(99)
        List(particleCount) {
            FireflyParticle(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat(),
                radius = rng.nextFloat() * 1.8f + 0.8f,
                alphaBase = rng.nextFloat() * 0.3f + 0.1f,
                phaseOffset = rng.nextFloat() * 2 * PI.toFloat(),
                speed = rng.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "fireflies")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ffphase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val alpha = (p.alphaBase + 0.45f * sin((phase * p.speed + p.phaseOffset).toDouble()).toFloat())
                .coerceIn(0.02f, 0.75f)
            // Soft glow: draw outer soft circle + inner bright dot
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = alpha * 0.35f),
                radius = p.radius * 3f,
                center = Offset(p.xRatio * w, p.yRatio * h)
            )
            drawCircle(
                color = Color(0xFF6EE7B7).copy(alpha = alpha),
                radius = p.radius,
                center = Offset(p.xRatio * w, p.yRatio * h)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Unified ThemeParticleBackground — renders on top of bg, behind content
// ──────────────────────────────────────────────────────────────

@Composable
fun ThemeParticleBackground(
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (theme) {
            AppTheme.COSMIC_SPACE -> CosmicSpaceParticleCanvas()
            AppTheme.NORDIC_FOREST -> NordicForestParticleCanvas()
            else -> Unit
        }
    }
}
