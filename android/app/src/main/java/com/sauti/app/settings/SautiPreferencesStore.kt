package com.sauti.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.sauti.app.tts.VoicePersona

/**
 * Clean commercial model tiers without exposing internal model architecture or filenames.
 */
enum class ModelTier(
    val displayName: String,
    val assetFileName: String,
    val userFacingSize: String,
    val description: String
) {
    CORE_LITE(
        displayName    = "Sauti Core Lite",
        assetFileName  = "core_lite.bin",
        userFacingSize = "135 MB",
        description    = "Ultra-fast & lightweight (Battery saver)"
    ),
    CORE_PRO(
        displayName    = "Sauti Core Pro",
        assetFileName  = "core_pro.bin",
        userFacingSize = "410 MB",
        description    = "Highest accuracy & multilingual reasoning"
    )
}

/**
 * Three-level user-friendly Hands-Free VAD sensitivity.
 */
enum class VadSensitivity(
    val displayName: String,
    val description: String
) {
    QUIET("Quiet Room", "High sensitivity for low-noise environments"),
    BALANCED("Balanced", "Standard sensitivity for home or office"),
    NOISY("Noisy Outdoors", "Aggressive filtering for crowded or street noise")
}

/**
 * Application Theme Modes: Auto (System-driven), Dark Mode, or Light Mode.
 */
enum class AppTheme(val displayName: String) {
    SYSTEM("Auto"),
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * Persistent user preferences backing commercial settings.
 * 100% sovereign, on-device SharedPreferences storage.
 */
class SautiPreferencesStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── User Profile & Personalization ────────────────────────────────────────

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        set(value) { prefs.edit().putString(KEY_USER_NAME, value.trim().ifEmpty { "User" }).apply() }

    var customInstructions: String
        get() = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CUSTOM_INSTRUCTIONS, value.trim()).apply() }

    // ── Appearance & Theme ─────────────────────────────────────────────────────

    var appTheme: AppTheme
        get() {
            val name = prefs.getString(KEY_APP_THEME, AppTheme.SYSTEM.name)
            return try { AppTheme.valueOf(name ?: AppTheme.SYSTEM.name) }
            catch (e: Exception) { AppTheme.SYSTEM }
        }
        set(value) { prefs.edit().putString(KEY_APP_THEME, value.name).apply() }

    // ── Voice & Audio ──────────────────────────────────────────────────────────

    var voicePersona: VoicePersona
        get() {
            val name = prefs.getString(KEY_VOICE_PERSONA, VoicePersona.MAWIMBI.name)
            return try { VoicePersona.valueOf(name ?: VoicePersona.MAWIMBI.name) }
            catch (e: IllegalArgumentException) { VoicePersona.MAWIMBI }
        }
        set(value) { prefs.edit().putString(KEY_VOICE_PERSONA, value.name).apply() }

    var autoSpeakReplies: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, false)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply() }

    var vadSensitivity: VadSensitivity
        get() {
            val name = prefs.getString(KEY_VAD_SENSITIVITY, VadSensitivity.BALANCED.name)
            return try { VadSensitivity.valueOf(name ?: VadSensitivity.BALANCED.name) }
            catch (e: Exception) { VadSensitivity.BALANCED }
        }
        set(value) { prefs.edit().putString(KEY_VAD_SENSITIVITY, value.name).apply() }

    // ── Intelligence Tier & Battery ───────────────────────────────────────────

    var modelTier: ModelTier
        get() {
            val name = prefs.getString(KEY_MODEL_TIER, ModelTier.CORE_PRO.name)
            return try { ModelTier.valueOf(name ?: ModelTier.CORE_PRO.name) }
            catch (e: IllegalArgumentException) { ModelTier.CORE_PRO }
        }
        set(value) { prefs.edit().putString(KEY_MODEL_TIER, value.name).apply() }

    var autoSwitchLowBattery: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SWITCH_BATTERY, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_SWITCH_BATTERY, value).apply() }

    // ── Privacy & Security ─────────────────────────────────────────────────────

    var biometricLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        set(value) { prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply() }

    // ── Balanced Inference Defaults (Internal) ────────────────────────────────

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
        private const val PREFS_NAME              = "sauti_user_prefs"
        private const val KEY_USER_NAME           = "user_display_name"
        private const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions"
        private const val KEY_APP_THEME           = "app_theme"
        private const val KEY_VOICE_PERSONA       = "voice_persona"
        private const val KEY_AUTO_SPEAK          = "auto_speak_replies"
        private const val KEY_VAD_SENSITIVITY     = "vad_sensitivity"
        private const val KEY_MODEL_TIER          = "model_tier"
        private const val KEY_AUTO_SWITCH_BATTERY = "auto_switch_battery"
        private const val KEY_BIOMETRIC_LOCK      = "biometric_lock"
        private const val KEY_TEMPERATURE         = "temperature"
        private const val KEY_TOP_K               = "top_k"
        private const val KEY_TOP_P               = "top_p"
        private const val KEY_REPEAT_PENALTY      = "repeat_penalty"
        private const val KEY_MAX_TOKENS          = "max_tokens"

        const val DEFAULT_TEMPERATURE    = 0.70f
        const val DEFAULT_TOP_K          = 40
        const val DEFAULT_TOP_P          = 0.90f
        const val DEFAULT_REPEAT_PENALTY = 1.25f
        const val DEFAULT_MAX_TOKENS     = 512
    }
}
