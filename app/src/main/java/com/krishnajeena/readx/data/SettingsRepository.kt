package com.krishnajeena.readx.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.krishnajeena.readx.BuildConfig

enum class AiProvider(val displayName: String) {
    GROQ("Groq (Default ⚡)"),
    GEMINI("Google Gemini"),
    OPENAI("OpenAI / ChatGPT"),
    ANTHROPIC("Anthropic Claude");

    val defaultModel: String
        get() = when (this) {
            GROQ -> SettingsRepository.DEFAULT_GROQ_MODEL
            GEMINI -> SettingsRepository.DEFAULT_GEMINI_MODEL
            OPENAI -> SettingsRepository.DEFAULT_OPENAI_MODEL
            ANTHROPIC -> SettingsRepository.DEFAULT_ANTHROPIC_MODEL
        }
}

/**
 * Stores AI/app settings securely. API keys for Groq, Gemini, OpenAI, and Anthropic
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
        val name = prefs.getString(KEY_PROVIDER, AiProvider.GROQ.name)
        return runCatching { AiProvider.valueOf(name!!) }.getOrDefault(AiProvider.GROQ)
    }

    fun setProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    // ---- Groq API Key ----

    fun getGroqApiKey(): String? = securePrefs.getString(KEY_GROQ_KEY, null)

    fun setGroqApiKey(key: String) {
        securePrefs.edit().putString(KEY_GROQ_KEY, key).apply()
    }

    fun clearGroqApiKey() {
        securePrefs.edit().remove(KEY_GROQ_KEY).apply()
    }

    // ---- Gemini API Key ----

    fun getGeminiApiKey(): String? = securePrefs.getString(KEY_GEMINI_KEY, null)

    fun setGeminiApiKey(key: String) {
        securePrefs.edit().putString(KEY_GEMINI_KEY, key).apply()
    }

    fun clearGeminiApiKey() {
        securePrefs.edit().remove(KEY_GEMINI_KEY).apply()
    }

    // ---- OpenAI API Key ----

    fun getOpenAiApiKey(): String? = securePrefs.getString(KEY_OPENAI_KEY, null)

    fun setOpenAiApiKey(key: String) {
        securePrefs.edit().putString(KEY_OPENAI_KEY, key).apply()
    }

    fun clearOpenAiApiKey() {
        securePrefs.edit().remove(KEY_OPENAI_KEY).apply()
    }

    // ---- Anthropic API Key ----

    fun getAnthropicApiKey(): String? = securePrefs.getString(KEY_ANTHROPIC_KEY, null)

    fun setAnthropicApiKey(key: String) {
        securePrefs.edit().putString(KEY_ANTHROPIC_KEY, key).apply()
    }

    fun clearAnthropicApiKey() {
        securePrefs.edit().remove(KEY_ANTHROPIC_KEY).apply()
    }

    /** Returns active API key for current provider (falling back to built-in app key for Groq if custom key is not set). */
    fun getActiveApiKey(): String? = when (getProvider()) {
        AiProvider.GROQ -> getGroqApiKey()?.takeIf { it.isNotBlank() } ?: BuildConfig.GROQ_API_KEY
        AiProvider.GEMINI -> getGeminiApiKey()
        AiProvider.OPENAI -> getOpenAiApiKey()
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
            AiProvider.GROQ -> DEFAULT_GROQ_MODEL
            AiProvider.GEMINI -> DEFAULT_GEMINI_MODEL
            AiProvider.OPENAI -> DEFAULT_OPENAI_MODEL
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
        private const val KEY_GROQ_KEY = "groq_api_key"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_OPENAI_KEY = "openai_api_key"
        private const val KEY_ANTHROPIC_KEY = "anthropic_api_key"
        private const val KEY_TRANSLATE_LANG = "translate_language"
        private const val KEY_AI_MODEL = "ai_model"

        const val DEFAULT_TRANSLATE_LANG = "Hindi"
        const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
        const val DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514"

        val GROQ_MODELS = listOf(
            "llama-3.3-70b-versatile" to "Llama 3.3 70B (Fast & Intelligent)",
            "llama3-8b-8192" to "Llama 3 8B (Ultra Fast)",
            "mixtral-8x7b-32768" to "Mixtral 8x7B (Context)",
            "gemma2-9b-it" to "Gemma 2 9B"
        )

        val GEMINI_MODELS = listOf(
            "gemini-2.0-flash" to "Gemini 2.0 Flash (Fastest & Free Tier)",
            "gemini-1.5-flash" to "Gemini 1.5 Flash (Balanced)",
            "gemini-1.5-pro" to "Gemini 1.5 Pro (Deep Reasoning)"
        )

        val OPENAI_MODELS = listOf(
            "gpt-4o-mini" to "GPT-4o mini (Fast & Cheap)",
            "gpt-4o" to "GPT-4o (Flagship)",
            "gpt-3.5-turbo" to "GPT-3.5 Turbo"
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
