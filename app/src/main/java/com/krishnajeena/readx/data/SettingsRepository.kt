package com.krishnajeena.readx.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class AiProvider(val displayName: String) {
    GEMINI("Google Gemini (Recommended)"),
    ANTHROPIC("Anthropic Claude")
}

/**
 * Stores AI/app settings securely. API keys for Gemini and Anthropic
 * are kept in EncryptedSharedPreferences; preferences in SharedPreferences.
 */
class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Provider ----

    fun getProvider(): AiProvider {
        val name = prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.name)
        return runCatching { AiProvider.valueOf(name!!) }.getOrDefault(AiProvider.GEMINI)
    }

    fun setProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    // ---- Gemini API Key ----

    fun getGeminiApiKey(): String? = securePrefs.getString(KEY_GEMINI_KEY, null)

    fun setGeminiApiKey(key: String) {
        securePrefs.edit().putString(KEY_GEMINI_KEY, key).apply()
    }

    fun clearGeminiApiKey() {
        securePrefs.edit().remove(KEY_GEMINI_KEY).apply()
    }

    // ---- Anthropic API Key ----

    fun getAnthropicApiKey(): String? = securePrefs.getString(KEY_ANTHROPIC_KEY, null)

    fun setAnthropicApiKey(key: String) {
        securePrefs.edit().putString(KEY_ANTHROPIC_KEY, key).apply()
    }

    fun clearAnthropicApiKey() {
        securePrefs.edit().remove(KEY_ANTHROPIC_KEY).apply()
    }

    /** Returns active API key for current provider. */
    fun getActiveApiKey(): String? = when (getProvider()) {
        AiProvider.GEMINI -> getGeminiApiKey()
        AiProvider.ANTHROPIC -> getAnthropicApiKey()
    }

    fun hasActiveApiKey(): Boolean = !getActiveApiKey().isNullOrBlank()

    // ---- Translate Language ----

    fun getTranslateLanguage(): String =
        prefs.getString(KEY_TRANSLATE_LANG, DEFAULT_TRANSLATE_LANG) ?: DEFAULT_TRANSLATE_LANG

    fun setTranslateLanguage(lang: String) {
        prefs.edit().putString(KEY_TRANSLATE_LANG, lang).apply()
    }

    // ---- AI Model ----

    fun getAiModel(): String {
        val defaultModel = when (getProvider()) {
            AiProvider.GEMINI -> DEFAULT_GEMINI_MODEL
            AiProvider.ANTHROPIC -> DEFAULT_ANTHROPIC_MODEL
        }
        return prefs.getString(KEY_AI_MODEL, defaultModel) ?: defaultModel
    }

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
    }

    companion object {
        private const val SECURE_PREFS_NAME = "readx_secure_settings"
        private const val PREFS_NAME = "readx_settings"

        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_ANTHROPIC_KEY = "anthropic_api_key"
        private const val KEY_TRANSLATE_LANG = "translate_language"
        private const val KEY_AI_MODEL = "ai_model"

        const val DEFAULT_TRANSLATE_LANG = "Hindi"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash"
        const val DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514"

        val GEMINI_MODELS = listOf(
            "gemini-2.0-flash" to "Gemini 2.0 Flash (Fastest & Free Tier)",
            "gemini-1.5-flash" to "Gemini 1.5 Flash (Balanced)",
            "gemini-1.5-pro" to "Gemini 1.5 Pro (Deep Reasoning)"
        )

        val ANTHROPIC_MODELS = listOf(
            "claude-sonnet-4-20250514" to "Claude Sonnet 4 (Fast)",
            "claude-opus-4-20250514" to "Claude Opus 4 (Best)",
            "claude-haiku-35-20241022" to "Claude 3.5 Haiku (Cheapest)"
        )

        val AVAILABLE_LANGUAGES = listOf(
            "Hindi", "Spanish", "French", "German", "Japanese",
            "Chinese", "Korean", "Portuguese", "Arabic", "Russian",
            "Italian", "Bengali", "Tamil", "Telugu", "Marathi",
            "Gujarati", "Kannada", "Malayalam", "Punjabi", "Urdu"
        )
    }
}
