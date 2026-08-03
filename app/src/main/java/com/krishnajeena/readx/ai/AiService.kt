package com.krishnajeena.readx.ai

import com.krishnajeena.readx.data.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight multi-provider API client for Google Gemini and Anthropic Claude.
 * Uses raw OkHttp to avoid external SDK bloat.
 */
class AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun prompt(
        provider: AiProvider,
        systemPrompt: String,
        userText: String,
        apiKey: String,
        model: String
    ): Result<String> = when (provider) {
        AiProvider.GROQ -> promptOpenAiCompatible(GROQ_API_URL, systemPrompt, userText, apiKey, model, "Groq")
        AiProvider.GEMINI -> promptGemini(systemPrompt, userText, apiKey, model)
        AiProvider.OPENAI -> promptOpenAiCompatible(OPENAI_API_URL, systemPrompt, userText, apiKey, model, "OpenAI")
        AiProvider.ANTHROPIC -> promptAnthropic(systemPrompt, userText, apiKey, model)
    }

    private suspend fun promptOpenAiCompatible(
        apiUrl: String,
        systemPrompt: String,
        userText: String,
        apiKey: String,
        model: String,
        providerName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    if (systemPrompt.isNotBlank()) {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                    }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userText)
                    })
                })
                put("temperature", 0.5)
            }

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from $providerName")

            if (!response.isSuccessful) {
                val errorJson = runCatching { JSONObject(responseBody) }.getOrNull()
                val errorMsg = errorJson?.optJSONObject("error")?.optString("message")
                    ?: "$providerName API error ${response.code}"
                throw Exception(errorMsg)
            }

            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
                ?: throw Exception("No choices returned from $providerName")
            if (choices.length() == 0) throw Exception("Empty choices in response")

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
                ?: throw Exception("No message in response choice")

            val textResult = message.optString("content", "").trim()
            if (textResult.isBlank()) throw Exception("Empty text returned")

            textResult
        }
    }

    private suspend fun promptGemini(
        systemPrompt: String,
        userText: String,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$GEMINI_BASE_URL/$model:generateContent?key=$apiKey"

            val body = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userText))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from Gemini")

            if (!response.isSuccessful) {
                val errorJson = runCatching { JSONObject(responseBody) }.getOrNull()
                val errorMsg = errorJson?.optJSONObject("error")?.optString("message")
                    ?: "Gemini API error ${response.code}"
                throw Exception(errorMsg)
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
                ?: throw Exception("No candidates returned from Gemini")

            if (candidates.length() == 0) throw Exception("Empty candidates list")

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
                ?: throw Exception("No content in response")
            val parts = content.optJSONArray("parts")
                ?: throw Exception("No parts in response content")

            val textResult = (0 until parts.length())
                .map { parts.getJSONObject(it).optString("text", "") }
                .joinToString("\n")

            if (textResult.isBlank()) throw Exception("Empty text returned")
            textResult
        }
    }

    private suspend fun promptAnthropic(
        systemPrompt: String,
        userText: String,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put("system", systemPrompt)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userText)
                    })
                })
            }

            val request = Request.Builder()
                .url(ANTHROPIC_API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from Anthropic")

            if (!response.isSuccessful) {
                val errorJson = runCatching { JSONObject(responseBody) }.getOrNull()
                val errorMsg = errorJson?.optJSONObject("error")?.optString("message")
                    ?: "Anthropic API error ${response.code}"
                throw Exception(errorMsg)
            }

            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            val textBlock = (0 until content.length())
                .map { content.getJSONObject(it) }
                .firstOrNull { it.getString("type") == "text" }
                ?: throw Exception("No text in response")

            textBlock.getString("text")
        }
    }

    companion object {
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        const val PROMPT_SIMPLIFY = "You are a serene reading assistant. Simplify the following text for easy understanding. Keep the core meaning but use simpler words and shorter sentences. Respond with only the simplified text, no preamble."

        const val PROMPT_EXPLAIN = "You are a serene reading assistant. Explain the following text concisely. Break down complex concepts and provide context. Keep your explanation brief and clear. Respond with only the explanation, no preamble."

        fun translatePrompt(language: String): String =
            "You are a translator. Translate the following text to $language. Provide only the translation, no preamble or explanation."

        const val PROMPT_CUSTOM_PREFIX = "You are a reading assistant helping with a PDF document. The user has selected text and wants help with it. Here is their request:\n\n"
    }
}
