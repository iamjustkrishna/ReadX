package com.krishnajeena.readx.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages dynamic AI model configurations fetched from the remote GitHub repository.
 * Falls back immediately to embedded defaults if network is unavailable or offline.
 */
class DynamicAiModelConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // In-memory cache of models
    private val memoryModels = mutableMapOf<AiProvider, List<Pair<String, String>>>()
    private val memoryDefaultModels = mutableMapOf<AiProvider, String>()

    init {
        loadCachedOrFallback()
    }

    /**
     * Asynchronously fetches the latest model configuration from the GitHub repository.
     * Caches the result on disk so future launches have the latest data even when offline.
     */
    suspend fun fetchRemoteConfig(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(REMOTE_CONFIG_URL)
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            val jsonStr = response.body?.string()

            if (response.isSuccessful && !jsonStr.isNullOrBlank()) {
                parseAndApply(jsonStr)
                prefs.edit().putString(KEY_CACHED_JSON, jsonStr).apply()
                Log.d(TAG, "Successfully fetched and updated remote AI model config.")
                true
            } else {
                Log.w(TAG, "Remote config fetch unsuccessful: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote AI model config: ${e.message}")
            false
        }
    }

    fun getModelsFor(provider: AiProvider): List<Pair<String, String>> {
        return memoryModels[provider] ?: getHardcodedModels(provider)
    }

    fun getDefaultModelFor(provider: AiProvider): String {
        return memoryDefaultModels[provider] ?: getHardcodedDefaultModel(provider)
    }

    /**
     * Checks if the currently selected model is still active in the catalog.
     * If decommissioned/retired, automatically returns the new default model.
     */
    fun sanitizeModel(provider: AiProvider, currentModel: String): String {
        val available = getModelsFor(provider).map { it.first }
        return if (available.contains(currentModel)) {
            currentModel
        } else {
            getDefaultModelFor(provider)
        }
    }

    private fun loadCachedOrFallback() {
        val cached = prefs.getString(KEY_CACHED_JSON, null)
        if (!cached.isNullOrBlank()) {
            val success = parseAndApply(cached)
            if (success) return
        }
        applyHardcodedFallbacks()
    }

    private fun parseAndApply(jsonStr: String): Boolean {
        return runCatching {
            val root = JSONObject(jsonStr)
            val providersObj = root.optJSONObject("providers") ?: return false

            for (provider in AiProvider.values()) {
                val pObj = providersObj.optJSONObject(provider.name) ?: continue
                val defModel = pObj.optString("default_model", getHardcodedDefaultModel(provider))
                val modelsArr = pObj.optJSONArray("models")

                val modelList = mutableListOf<Pair<String, String>>()
                if (modelsArr != null) {
                    for (i in 0 until modelsArr.length()) {
                        val m = modelsArr.getJSONObject(i)
                        val id = m.getString("id")
                        val name = m.getString("name")
                        modelList.add(id to name)
                    }
                }

                if (modelList.isNotEmpty()) {
                    memoryModels[provider] = modelList
                    memoryDefaultModels[provider] = defModel
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun applyHardcodedFallbacks() {
        for (provider in AiProvider.values()) {
            memoryModels[provider] = getHardcodedModels(provider)
            memoryDefaultModels[provider] = getHardcodedDefaultModel(provider)
        }
    }

    companion object {
        private const val TAG = "DynamicAiConfig"
        private const val PREFS_NAME = "readx_dynamic_ai_config"
        private const val KEY_CACHED_JSON = "cached_ai_config_json"

        const val REMOTE_CONFIG_URL =
            "https://raw.githubusercontent.com/thekrishnajeena/ReadX/master/ai_models_config.json"

        fun getHardcodedDefaultModel(provider: AiProvider): String = when (provider) {
            AiProvider.GROQ -> "llama-3.3-70b-versatile"
            AiProvider.GEMINI -> "gemini-2.0-flash"
            AiProvider.OPENAI -> "gpt-4o-mini"
            AiProvider.ANTHROPIC -> "claude-3-5-sonnet-20241022"
        }

        fun getHardcodedModels(provider: AiProvider): List<Pair<String, String>> = when (provider) {
            AiProvider.GROQ -> listOf(
                "llama-3.3-70b-versatile" to "Llama 3.3 70B (Fast & Intelligent)",
                "llama-3.1-8b-instant" to "Llama 3.1 8B Instant (Ultra Fast)",
                "openai/gpt-oss-120b" to "GPT OSS 120B (High Reasoning)",
                "mixtral-8x7b-32768" to "Mixtral 8x7B (Large Context)",
                "gemma2-9b-it" to "Gemma 2 9B"
            )
            AiProvider.GEMINI -> listOf(
                "gemini-2.0-flash" to "Gemini 2.0 Flash (Fastest & Free Tier)",
                "gemini-1.5-flash" to "Gemini 1.5 Flash (Balanced)",
                "gemini-1.5-pro" to "Gemini 1.5 Pro (Deep Reasoning)"
            )
            AiProvider.OPENAI -> listOf(
                "gpt-4o-mini" to "GPT-4o mini (Fast & Cost-Efficient)",
                "gpt-4o" to "GPT-4o (Flagship Multimodal)",
                "gpt-3.5-turbo" to "GPT-3.5 Turbo (Legacy)"
            )
            AiProvider.ANTHROPIC -> listOf(
                "claude-3-5-sonnet-20241022" to "Claude 3.5 Sonnet (State of the Art)",
                "claude-3-5-haiku-20241022" to "Claude 3.5 Haiku (Fast & Lightweight)",
                "claude-3-opus-20240229" to "Claude 3 Opus (Complex Analysis)"
            )
        }
    }
}
