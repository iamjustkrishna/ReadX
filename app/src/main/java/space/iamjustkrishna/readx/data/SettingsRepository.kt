package space.iamjustkrishna.readx.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import space.iamjustkrishna.readx.BuildConfig

enum class AiProvider(val displayName: String) {
    GROQ("Groq (Default ⚡)"),
    GEMINI("Google Gemini"),
    OPENAI("OpenAI / ChatGPT"),
    ANTHROPIC("Anthropic Claude");
}

/**
 * Stores AI/app settings securely. API keys for Groq, Gemini, OpenAI, and Anthropic
 * are kept in EncryptedSharedPreferences; preferences in SharedPreferences.
 * Supports dynamic remote model lists with offline fallbacks and auto-sanitization.
 */
class SettingsRepository(context: Context) {

    val dynamicConfig = DynamicAiModelConfigRepository(context)

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

    suspend fun fetchDynamicConfig(): Boolean {
        return dynamicConfig.fetchRemoteConfig()
    }

    fun getModelsFor(provider: AiProvider): List<Pair<String, String>> {
        return dynamicConfig.getModelsFor(provider)
    }

    fun getDefaultModelFor(provider: AiProvider): String {
        return dynamicConfig.getDefaultModelFor(provider)
    }

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
        val provider = getProvider()
        val defaultModel = dynamicConfig.getDefaultModelFor(provider)
        val savedModel = prefs.getString("${KEY_AI_MODEL}_${provider.name}", null)
            ?: prefs.getString(KEY_AI_MODEL, defaultModel)
            ?: defaultModel
        val sanitized = dynamicConfig.sanitizeModel(provider, savedModel)
        if (sanitized != savedModel) {
            setAiModel(sanitized)
        }
        return sanitized
    }

    fun setAiModel(model: String) {
        val provider = getProvider()
        val sanitized = dynamicConfig.sanitizeModel(provider, model)
        prefs.edit()
            .putString("${KEY_AI_MODEL}_${provider.name}", sanitized)
            .putString(KEY_AI_MODEL, sanitized)
            .apply()
    }

    // ---- Theme ----

    fun getAppTheme(): space.iamjustkrishna.readx.ui.theme.AppTheme {
        val name = prefs.getString(KEY_APP_THEME, space.iamjustkrishna.readx.ui.theme.AppTheme.SYSTEM_DEFAULT.name)
        return runCatching { space.iamjustkrishna.readx.ui.theme.AppTheme.valueOf(name!!) }
            .getOrDefault(space.iamjustkrishna.readx.ui.theme.AppTheme.SYSTEM_DEFAULT)
    }

    fun setAppTheme(theme: space.iamjustkrishna.readx.ui.theme.AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
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
        private const val KEY_APP_THEME = "app_theme"

        const val DEFAULT_TRANSLATE_LANG = "Hindi"

        // Embedded fallback lists (used for static references if needed)
        val GROQ_MODELS get() = DynamicAiModelConfigRepository.getHardcodedModels(AiProvider.GROQ)
        val GEMINI_MODELS get() = DynamicAiModelConfigRepository.getHardcodedModels(AiProvider.GEMINI)
        val OPENAI_MODELS get() = DynamicAiModelConfigRepository.getHardcodedModels(AiProvider.OPENAI)
        val ANTHROPIC_MODELS get() = DynamicAiModelConfigRepository.getHardcodedModels(AiProvider.ANTHROPIC)

        val AVAILABLE_LANGUAGES = listOf(
            "Hindi", "Spanish", "French", "German", "Japanese",
            "Chinese", "Korean", "Portuguese", "Arabic", "Russian",
            "Italian", "Bengali", "Tamil", "Telugu", "Marathi",
            "Gujarati", "Kannada", "Malayalam", "Punjabi", "Urdu"
        )
    }
}
