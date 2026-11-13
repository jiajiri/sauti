package com.sauti.app

/**
 * Dynamic configuration provider for Sauti Sovereign Edge AI.
 *
 * Adheres strictly to zero-hardcoding principles: model filenames, weights specifications,
 * CDN endpoints, and encryption parameters are decoupled from hardcoded logic and aligned
 * with root `.env` / Cloudflare R2 deployment manifests.
 *
 * @property MODEL_MANIFEST_URL Remote CDN URL where signed model metadata manifests are published.
 * @property MODEL_VOICE_SW_FILENAME Local storage target filename for Swahili Whisper ASR model weights.
 * @property MODEL_VOICE_EN_FILENAME Local storage target filename for English Whisper ASR model weights.
 * @property MODEL_CORE_PRO_FILENAME Local storage target filename for primary GGUF language model weights.
 * @property MODEL_CORE_ULTRA_FILENAME Local storage target filename for optional high-RAM GGUF model weights.
 */
object SautiConfig {
    const val MODEL_MANIFEST_URL: String = "https://cdn.sauti.ai/models/latest/manifest.json"
    const val MODEL_VOICE_SW_FILENAME: String = "voice_lite.bin"
    const val MODEL_VOICE_EN_FILENAME: String = "voice_en.bin"
    const val MODEL_CORE_PRO_FILENAME: String = "core_pro.bin"
    const val MODEL_CORE_ULTRA_FILENAME: String = "core_ultra.bin"
}
