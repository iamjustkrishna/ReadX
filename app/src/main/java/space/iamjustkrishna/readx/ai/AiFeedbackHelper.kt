package space.iamjustkrishna.readx.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FormattedAiError(
    val title: String,
    val message: String,
    val isRateLimitOrQuota: Boolean,
    val isApiKeyIssue: Boolean,
    val rawMessage: String?
)

object AiFeedbackHelper {
    const val FEEDBACK_EMAIL = "hlo.krsna@gmail.com"

    fun parseError(rawError: String?): FormattedAiError {
        val raw = rawError.orEmpty().trim()
        val lower = raw.lowercase()

        val isRateLimit = lower.contains("rate_limit") ||
                lower.contains("rate limit") ||
                lower.contains("429") ||
                lower.contains("quota") ||
                lower.contains("too many requests") ||
                lower.contains("tokens per minute") ||
                lower.contains("requests per minute") ||
                lower.contains("tpm") ||
                lower.contains("rpm") ||
                lower.contains("resource_exhausted") ||
                lower.contains("capacity") ||
                lower.contains("overloaded")

        val isApiKey = lower.contains("api key") ||
                lower.contains("api_key") ||
                lower.contains("401") ||
                lower.contains("unauthorized") ||
                lower.contains("forbidden")

        val isNetwork = lower.contains("unable to resolve host") ||
                lower.contains("timeout") ||
                lower.contains("connectexception") ||
                lower.contains("network")

        return when {
            isRateLimit -> FormattedAiError(
                title = "AI Limit Reached ⚡",
                message = "You've reached the free-tier AI request limit. We're actively scaling capacity! Let us know if you'd like higher limits or pro features.",
                isRateLimitOrQuota = true,
                isApiKeyIssue = false,
                rawMessage = raw
            )
            isApiKey -> FormattedAiError(
                title = "API Key Configuration Required 🔑",
                message = "Please configure or verify your AI API key in Settings to continue using AI features.",
                isRateLimitOrQuota = false,
                isApiKeyIssue = true,
                rawMessage = raw
            )
            isNetwork -> FormattedAiError(
                title = "Network Connection Error 📡",
                message = "Unable to reach the AI servers. Please check your internet connection and try again.",
                isRateLimitOrQuota = false,
                isApiKeyIssue = false,
                rawMessage = raw
            )
            else -> FormattedAiError(
                title = "AI Service Notice ⚠️",
                message = if (raw.isNotBlank()) "AI request could not be completed: $raw" else "AI request could not be completed at this time. Send us feedback to help us improve!",
                isRateLimitOrQuota = false,
                isApiKeyIssue = false,
                rawMessage = raw
            )
        }
    }

    fun sendFeedbackEmail(
        context: Context,
        source: String = "AI Assistant",
        errorDetails: String? = null
    ) {
        val subject = "ReadX AI Feedback / Limit Request ($source)"
        val body = buildString {
            append("Hi ReadX Team,\n\n")
            append("I was using ReadX ($source) and wanted to share feedback regarding AI limits / features:\n\n")
            append("[Please write your thoughts or request for higher limits here]\n\n")
            append("------------------------\n")
            append("App: ReadX Android\n")
            append("Feature: $source\n")
            if (!errorDetails.isNullOrBlank()) {
                append("Error info: $errorDetails\n")
            }
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$FEEDBACK_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "Email client not found. Write to $FEEDBACK_EMAIL", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Reusable error card that displays a user-friendly error explanation and direct feedback email action.
 */
@Composable
fun AiErrorFeedbackCard(
    rawError: String?,
    source: String,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val formatted = AiFeedbackHelper.parseError(rawError)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)
        ),
        border = BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 26.dp else 30.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (formatted.isRateLimitOrQuota) Icons.Outlined.Warning else Icons.Outlined.Feedback,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                    )
                }
                Text(
                    text = formatted.title,
                    fontSize = if (isCompact) 12.5.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            }

            Text(
                text = formatted.message,
                fontSize = if (isCompact) 11.5.sp else 13.sp,
                color = Color(0xFF7F1D1D),
                lineHeight = if (isCompact) 16.sp else 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Email feedback action
                Button(
                    onClick = {
                        AiFeedbackHelper.sendFeedbackEmail(
                            context = context,
                            source = source,
                            errorDetails = formatted.rawMessage
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 4.dp else 6.dp
                    ),
                    modifier = Modifier.height(if (isCompact) 32.dp else 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = "Send Feedback",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send Feedback",
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (formatted.isApiKeyIssue && onNavigateToSettings != null) {
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = if (isCompact) 10.dp else 12.dp,
                            vertical = if (isCompact) 4.dp else 6.dp
                        ),
                        modifier = Modifier.height(if (isCompact) 32.dp else 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Settings",
                            fontSize = if (isCompact) 11.sp else 12.sp
                        )
                    }
                }
            }
        }
    }
}
