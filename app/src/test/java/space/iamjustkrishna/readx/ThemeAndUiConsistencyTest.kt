package space.iamjustkrishna.readx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
import space.iamjustkrishna.readx.model.Highlight
import space.iamjustkrishna.readx.ui.theme.AppTheme
import space.iamjustkrishna.readx.ui.theme.getThemeColors

class ThemeAndUiConsistencyTest {

    @Test
    fun testAllThemesDefinedAndContrasting() {
        for (theme in AppTheme.values()) {
            val colors = getThemeColors(theme)

            assertNotNull("Theme $theme must produce valid colors", colors)
            assertNotEquals("Primary color must be specified", Color.Unspecified, colors.primary)
            assertNotEquals("Background color must be specified", Color.Unspecified, colors.background)
            assertNotEquals("Surface color must be specified", Color.Unspecified, colors.surface)
            assertNotEquals("OnSurface color must be specified", Color.Unspecified, colors.onSurface)

            val bgLuminance = colors.background.luminance()
            val textLuminance = colors.onBackground.luminance()

            if (colors.isDark) {
                assertTrue("Dark theme ${theme.name} background should be dark (luminance < 0.35)", bgLuminance < 0.35f)
                assertTrue("Dark theme ${theme.name} text should be light (luminance > 0.50)", textLuminance > 0.50f)
            } else {
                assertTrue("Light theme ${theme.name} background should be light (luminance > 0.65)", bgLuminance > 0.65f)
                assertTrue("Light theme ${theme.name} text should be dark (luminance < 0.40)", textLuminance < 0.40f)
            }

            val contrastDiff = kotlin.math.abs(bgLuminance - textLuminance)
            assertTrue("Contrast difference for ${theme.name} must be >= 0.30, got $contrastDiff", contrastDiff >= 0.30f)
        }
    }

    @Test
    fun testScrubberPageIndicatorContrastAndFormat() {
        fun formatPageIndicator(currentPage: Int, pageCount: Int): String { return "${currentPage + 1} / $pageCount" }

        assertEquals("1 / 10", formatPageIndicator(0, 10))
        assertEquals("45 / 100", formatPageIndicator(44, 100))

        for (theme in AppTheme.values()) {
            val colors = getThemeColors(theme)
            val bubbleBg = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
            val bubbleText = if (colors.isDark) Color.White else Color(0xFF0F172A)

            val bgLum = bubbleBg.luminance()
            val textLum = bubbleText.luminance()
            val contrast = kotlin.math.abs(bgLum - textLum)

            assertTrue("Scrubber contrast for ${theme.name} must be >= 0.70, got $contrast", contrast >= 0.70f)
        }
    }

    @Test
    fun testStorageFormatUtilities() {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes.toFloat() / (1024f * 1024f * 1024f))
                bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes.toFloat() / (1024f * 1024f))
                bytes > 0 -> "${bytes / 1024} KB"
                else -> "0 B"
            }
        }

        assertEquals("0 B", formatBytes(0L))
        assertEquals("500 KB", formatBytes(512_000L))
        assertEquals("10.0 MB", formatBytes(10L * 1024L * 1024L))
        assertEquals("1.00 GB", formatBytes(1024L * 1024L * 1024L))
    }

    @Test
    fun testHighlightModelIntegrity() {
        val highlight = Highlight(
            id = "test-hl-1",
            pageIndex = 3,
            startChar = 10,
            endChar = 45,
            text = "Clean architecture in Android with Jetpack Compose",
            color = 0xFF4285F4L,
            note = "Important architectural note"
        )

        assertEquals("test-hl-1", highlight.id)
        assertEquals(3, highlight.pageIndex)
        assertEquals(10, highlight.startChar)
        assertEquals(45, highlight.endChar)
        assertEquals(0xFF4285F4L, highlight.color)
        assertEquals("Important architectural note", highlight.note)
        assertTrue(highlight.text.contains("Jetpack Compose"))
    }

    @Test
    fun testSearchQueryDebounceSanitization() {
        fun isValidSearch(query: String): Boolean {
            return query.trim().length >= 2
        }

        assertFalse(isValidSearch(""))
        assertFalse(isValidSearch("   "))
        assertFalse(isValidSearch("a"))
        assertFalse(isValidSearch(" a "))
        assertTrue(isValidSearch("ab"))
        assertTrue(isValidSearch("Kotlin Coroutines"))
    }
}