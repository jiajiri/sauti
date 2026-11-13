package com.sauti.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.sauti.app.tts.VoicePersona

/**
 * Sauti model tier selection — maps to on-device GGUF asset files.
 */
enum class ModelTier(
    val displayName: String,
    val assetFileName: String,
    val paramCount: String,
    val description: String
) {
    CORE_LITE(
        displayName  = "Core Lite",
        assetFileName = "core_lite.bin",
        paramCount   = "135M",
        description  = "Fast, lightweight — SmolLM2 135M"
    ),
    CORE_PRO(
        displayName  = "Core Pro",
        assetFileName = "core_pro.bin",
        paramCount   = "630M",
        description  = "Balanced, multilingual — Qwen2.5 0.5B"
    )
}

/**
 * Persistent user preferences for voice persona, model tier, and LLM inference parameters.
 * Backed by SharedPreferences (no network, no cloud, 100% sovereign).
 */
class SautiPreferencesStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Voice ──────────────────────────────────────────────────────────────────

    var voicePersona: VoicePersona
        get() {
            val name = prefs.getString(KEY_VOICE_PERSONA, VoicePersona.MAWIMBI.name)
            return try { VoicePersona.valueOf(name ?: VoicePersona.MAWIMBI.name) }
            catch (e: IllegalArgumentException) { VoicePersona.MAWIMBI }
        }
        set(value) { prefs.edit().putString(KEY_VOICE_PERSONA, value.name).apply() }

    // ── Model ──────────────────────────────────────────────────────────────────

    var modelTier: ModelTier
        get() {
            val name = prefs.getString(KEY_MODEL_TIER, ModelTier.CORE_PRO.name)
            return try { ModelTier.valueOf(name ?: ModelTier.CORE_PRO.name) }
            catch (e: IllegalArgumentException) { ModelTier.CORE_PRO }
        }
        set(value) { prefs.edit().putString(KEY_MODEL_TIER, value.name).apply() }

    // ── Inference parameters ───────────────────────────────────────────────────

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(value) { prefs.edit().putFloat(KEY_TEMPERATURE, value).apply() }

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, DEFAULT_TOP_K)
        set(value) { prefs.edit().putInt(KEY_TOP_K, value).apply() }

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, DEFAULT_TOP_P)
        set(value) { prefs.edit().putFloat(KEY_TOP_P, value).apply() }

    var repeatPenalty: Float
        get() = prefs.getFloat(KEY_REPEAT_PENALTY, DEFAULT_REPEAT_PENALTY)
        set(value) { prefs.edit().putFloat(KEY_REPEAT_PENALTY, value).apply() }

    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        set(value) { prefs.edit().putInt(KEY_MAX_TOKENS, value).apply() }

    // ── Reset ─────────────────────────────────────────────────────────────────

    fun resetInferenceDefaults() {
        prefs.edit()
            .putFloat(KEY_TEMPERATURE,    DEFAULT_TEMPERATURE)
            .putInt(KEY_TOP_K,            DEFAULT_TOP_K)
            .putFloat(KEY_TOP_P,          DEFAULT_TOP_P)
            .putFloat(KEY_REPEAT_PENALTY, DEFAULT_REPEAT_PENALTY)
            .putInt(KEY_MAX_TOKENS,       DEFAULT_MAX_TOKENS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME          = "sauti_user_prefs"
        private const val KEY_VOICE_PERSONA   = "voice_persona"
        private const val KEY_MODEL_TIER      = "model_tier"
        private const val KEY_TEMPERATURE     = "temperature"
        private const val KEY_TOP_K           = "top_k"
        private const val KEY_TOP_P           = "top_p"
        private const val KEY_REPEAT_PENALTY  = "repeat_penalty"
        private const val KEY_MAX_TOKENS      = "max_tokens"

        const val DEFAULT_TEMPERATURE    = 0.7f
        const val DEFAULT_TOP_K          = 40
        const val DEFAULT_TOP_P          = 0.95f
        const val DEFAULT_REPEAT_PENALTY = 1.1f
        const val DEFAULT_MAX_TOKENS     = 512
    }
}
