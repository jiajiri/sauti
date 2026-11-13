#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <algorithm>
#include <sstream>
#include <iomanip>
#include <mutex>
#include <atomic>
#include <thread>
#include <android/log.h>
#include "ring_buffer.h"
#include "memory_manager.h"

// llama.cpp & whisper.cpp headers
#include "llama.h"
#include "whisper.h"

#define TAG "SautiNativeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::mutex g_engine_mutex;
static std::string g_sauti_voice_path;
static std::string g_sauti_core_path;
static bool g_sauti_voice_initialized = false;
static bool g_sauti_core_initialized = false;

// Runtime-configurable inference parameters (safe defaults match original hardcoded values)
static struct {
    float temperature    = 0.7f;
    int   top_k          = 40;
    float top_p          = 0.90f;
    float repeat_penalty = 1.25f;
    int   max_tokens     = 512;
} g_inf_params;

// Global lock-free ring buffer for 16kHz audio samples
static sauti::dsa::SpscRingBuffer<int16_t, 65536> g_audio_ring_buffer;

// Llama model & context state
static llama_model* g_llama_model = nullptr;
static llama_context* g_llama_context = nullptr;

// Whisper context state
static struct whisper_context* g_whisper_context = nullptr;

static void ensureLlamaModelLoaded() {
    if (g_llama_model) return;
    if (g_sauti_core_path.empty()) {
        LOGW("Sauti Core path is empty. Cannot load llama model.");
        return;
    }

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.load_mode = LLAMA_LOAD_MODE_MMAP;

    LOGI("Loading Llama model from path: %s", g_sauti_core_path.c_str());
    g_llama_model = llama_model_load_from_file(g_sauti_core_path.c_str(), model_params);
    if (!g_llama_model) {
        LOGE("Failed to load Llama model from %s", g_sauti_core_path.c_str());
        return;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024;
    unsigned int hw_threads = std::thread::hardware_concurrency();
    ctx_params.n_threads = (hw_threads > 0) ? std::min(hw_threads, 4u) : 2;
    ctx_params.n_batch = 512;

    g_llama_context = llama_init_from_model(g_llama_model, ctx_params);
    if (!g_llama_context) {
        LOGE("Failed to create Llama context.");
        llama_model_free(g_llama_model);
        g_llama_model = nullptr;
        return;
    }

    LOGI("Llama model and context successfully initialized.");
}

static void ensureWhisperModelLoaded() {
    if (g_whisper_context) return;
    if (g_sauti_voice_path.empty()) {
        LOGW("Sauti Voice path is empty. Cannot load whisper model.");
        return;
    }

    LOGI("Loading Whisper model from path: %s", g_sauti_voice_path.c_str());
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    g_whisper_context = whisper_init_from_file_with_params(g_sauti_voice_path.c_str(), cparams);
    if (!g_whisper_context) {
        LOGE("Failed to load Whisper context from %s", g_sauti_voice_path.c_str());
        return;
    }
    LOGI("Whisper model context successfully initialized.");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sauti_app_SautiInputMethodService_nativeInitSautiVoice(
    JNIEnv* env,
    jobject /* this */,
    jstring jSautiVoicePath) {
    if (!jSautiVoicePath) return JNI_FALSE;
    const char* pathStr = env->GetStringUTFChars(jSautiVoicePath, nullptr);
    if (!pathStr) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_engine_mutex);
    g_sauti_voice_path = pathStr;
    env->ReleaseStringUTFChars(jSautiVoicePath, pathStr);

    ensureWhisperModelLoaded();
    g_sauti_voice_initialized = (g_whisper_context != nullptr);
    return g_sauti_voice_initialized ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sauti_app_SautiInputMethodService_nativeInitSautiCore(
    JNIEnv* env,
    jobject /* this */,
    jstring jSautiCorePath) {
    if (!jSautiCorePath) return JNI_FALSE;
    const char* pathStr = env->GetStringUTFChars(jSautiCorePath, nullptr);
    if (!pathStr) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_engine_mutex);
    g_sauti_core_path = pathStr;
    env->ReleaseStringUTFChars(jSautiCorePath, pathStr);

    ensureLlamaModelLoaded();
    g_sauti_core_initialized = (g_llama_model != nullptr && g_llama_context != nullptr);
    return g_sauti_core_initialized ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sauti_app_sauti_SautiEngineLoader_nativeInitCoreEngine(
    JNIEnv* env,
    jobject /* this */,
    jstring jSautiCorePath) {
    if (!jSautiCorePath) return JNI_FALSE;
    const char* pathStr = env->GetStringUTFChars(jSautiCorePath, nullptr);
    if (!pathStr) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_engine_mutex);
    g_sauti_core_path = pathStr;
    env->ReleaseStringUTFChars(jSautiCorePath, pathStr);

    ensureLlamaModelLoaded();
    g_sauti_core_initialized = (g_llama_model != nullptr && g_llama_context != nullptr);
    return g_sauti_core_initialized ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sauti_app_sauti_SautiEngineLoader_nativeInitVoiceEngine(
    JNIEnv* env,
    jobject /* this */,
    jstring jSautiVoicePath) {
    if (!jSautiVoicePath) return JNI_FALSE;
    const char* pathStr = env->GetStringUTFChars(jSautiVoicePath, nullptr);
    if (!pathStr) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_engine_mutex);
    g_sauti_voice_path = pathStr;
    env->ReleaseStringUTFChars(jSautiVoicePath, pathStr);

    ensureWhisperModelLoaded();
    g_sauti_voice_initialized = (g_whisper_context != nullptr);
    return g_sauti_voice_initialized ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sauti_app_SautiInputMethodService_nativePushAudioSamples(
    JNIEnv* env,
    jobject /* this */,
    jshortArray samples,
    jint length) {
    if (!samples || length <= 0) return 0;

    jshort* buffer = env->GetShortArrayElements(samples, nullptr);
    if (!buffer) return 0;

    size_t written = g_audio_ring_buffer.write(reinterpret_cast<const int16_t*>(buffer), static_cast<size_t>(length));
    env->ReleaseShortArrayElements(samples, buffer, JNI_ABORT);
    return static_cast<jint>(written);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sauti_app_SautiInputMethodService_nativeTranscribeStreaming(
    JNIEnv* env,
    jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (!g_whisper_context) {
        ensureWhisperModelLoaded();
        if (!g_whisper_context) return env->NewStringUTF("");
    }

    constexpr size_t CHUNK_SIZE = 4096;
    int16_t audio_chunk[CHUNK_SIZE];
    std::vector<float> pcm_float;

    while (!g_audio_ring_buffer.empty()) {
        size_t read_samples = g_audio_ring_buffer.read(audio_chunk, CHUNK_SIZE);
        if (read_samples == 0) break;
        for (size_t i = 0; i < read_samples; ++i) {
            pcm_float.push_back(static_cast<float>(audio_chunk[i]) / 32768.0f);
        }
    }

    if (pcm_float.empty()) return env->NewStringUTF("");

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_special = false;
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.language = "sw";
    wparams.n_threads = 4;

    if (whisper_full(g_whisper_context, wparams, pcm_float.data(), pcm_float.size()) != 0) {
        LOGE("Whisper full transcription failed.");
        return env->NewStringUTF("");
    }

    std::string result_text;
    const int n_segments = whisper_full_n_segments(g_whisper_context);
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(g_whisper_context, i);
        if (text) result_text += text;
    }

    LOGI("Whisper transcribed text: %s", result_text.c_str());
    return env->NewStringUTF(result_text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_sauti_app_sauti_SautiEngineLoader_nativeGenerateChatCompletion(
    JNIEnv* env,
    jobject /* this */,
    jstring jPrompt,
    jobject tokenCallback) {
    if (!jPrompt || !tokenCallback) return;

    const char* promptCStr = env->GetStringUTFChars(jPrompt, nullptr);
    if (!promptCStr) return;

    jclass callbackClass = env->GetObjectClass(tokenCallback);
    if (!callbackClass) {
        env->ReleaseStringUTFChars(jPrompt, promptCStr);
        return;
    }

    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    if (!onTokenMethod) {
        env->ReleaseStringUTFChars(jPrompt, promptCStr);
        return;
    }

    std::lock_guard<std::mutex> lock(g_engine_mutex);
    ensureLlamaModelLoaded();

    if (!g_llama_context || !g_llama_model) {
        LOGW("Llama engine not ready for prompt: %s", promptCStr);
        env->ReleaseStringUTFChars(jPrompt, promptCStr);
        return;
    }

    const struct llama_vocab* vocab = llama_model_get_vocab(g_llama_model);
    std::string prompt(promptCStr);
    env->ReleaseStringUTFChars(jPrompt, promptCStr);

    // Tokenize
    const int n_prompt = -llama_tokenize(vocab, prompt.c_str(), prompt.length(), NULL, 0, true, true);
    std::vector<llama_token> prompt_tokens(n_prompt);
    if (llama_tokenize(vocab, prompt.c_str(), prompt.length(), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        LOGE("Failed to tokenize prompt.");
        return;
    }

    // Clear memory / KV cache before decoding new prompt
    llama_memory_clear(llama_get_memory(g_llama_context), true);

    // Evaluate prompt tokens in safe chunks of n_batch
    int32_t n_batch = llama_n_batch(g_llama_context);
    llama_pos current_pos = 0;
    for (size_t i = 0; i < prompt_tokens.size(); i += n_batch) {
        int32_t n_eval = std::min((int32_t)(prompt_tokens.size() - i), n_batch);
        llama_batch batch = llama_batch_init(n_eval, 0, 1);
        for (int32_t j = 0; j < n_eval; ++j) {
            batch.token[j] = prompt_tokens[i + j];
            batch.pos[j] = current_pos + j;
            batch.n_seq_id[j] = 1;
            batch.seq_id[j][0] = 0;
            batch.logits[j] = (i + j == prompt_tokens.size() - 1);
        }
        batch.n_tokens = n_eval;

        int ret = llama_decode(g_llama_context, batch);
        llama_batch_free(batch);
        if (ret != 0) {
            LOGE("Failed to decode prompt chunk at offset %zu (ret=%d)", i, ret);
            return;
        }
        current_pos += n_eval;
    }

    // Production LLMOps Sampling Pipeline (uses runtime-configurable g_inf_params):
    // 1. Penalties sampler: penalizes repeated n-grams using g_inf_params.repeat_penalty
    // 2. Top-K filtering using g_inf_params.top_k
    // 3. Top-P nucleus filtering using g_inf_params.top_p
    // 4. Min-P filtering (p=0.05) to eliminate long-tail hallucination tokens
    // 5. Temperature scaling using g_inf_params.temperature
    // 6. Distribution sampler with stable seed for token selection
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);

    const int32_t n_vocab = llama_vocab_n_tokens(vocab);
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(n_vocab, 64, g_inf_params.repeat_penalty, 0.50f, 0.30f));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(g_inf_params.top_k));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(g_inf_params.top_p, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_min_p(0.05f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(g_inf_params.temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

    // CRITICAL: Feed all prompt tokens into the sampler chain so penalty/repetition tracking works on full context!
    for (size_t i = 0; i < prompt_tokens.size(); ++i) {
        llama_sampler_accept(smpl, prompt_tokens[i]);
    }

    // Generate up to g_inf_params.max_tokens (runtime-configurable via Settings)
    for (int iter = 0; iter < g_inf_params.max_tokens; ++iter) {
        llama_token new_token_id = llama_sampler_sample(smpl, g_llama_context, -1);
        llama_sampler_accept(smpl, new_token_id);

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, false);
        if (n > 0) {
            std::string piece(buf, n);
            jstring jPiece = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(tokenCallback, onTokenMethod, jPiece);
            env->DeleteLocalRef(jPiece);
        }

        llama_batch single_batch = llama_batch_init(1, 0, 1);
        single_batch.token[0] = new_token_id;
        single_batch.pos[0] = current_pos;
        single_batch.n_seq_id[0] = 1;
        single_batch.seq_id[0][0] = 0;
        single_batch.logits[0] = true;
        single_batch.n_tokens = 1;

        int ret = llama_decode(g_llama_context, single_batch);
        llama_batch_free(single_batch);
        if (ret != 0) {
            LOGE("Failed to decode token during generation (ret=%d)", ret);
            break;
        }
        current_pos++;
    }

    llama_sampler_free(smpl);
    LOGI("Native LLM token streaming completed successfully.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_sauti_app_SautiInputMethodService_nativeNotifyMemoryPressure(
    JNIEnv* /* env */,
    jobject /* this */,
    jboolean isCritical) {
    sauti::memory::MemoryArenaManager::instance().onMemoryTrim(static_cast<bool>(isCritical));
}

// ─── Settings: Runtime Inference Parameter Override ───────────────────────────

extern "C" JNIEXPORT void JNICALL
Java_com_sauti_app_sauti_SautiEngineLoader_nativeSetInferenceParams(
    JNIEnv* /* env */, jobject /* this */,
    jfloat temp, jint topK, jfloat topP, jfloat repPen, jint maxTok
) {
    g_inf_params.temperature    = temp;
    g_inf_params.top_k          = topK;
    g_inf_params.top_p          = topP;
    g_inf_params.repeat_penalty = repPen;
    g_inf_params.max_tokens     = maxTok;
    LOGI("Inference params updated: temp=%.2f topK=%d topP=%.2f repPen=%.2f maxTok=%d",
         temp, topK, topP, repPen, maxTok);
}

// ─── Settings: Hot-Swap Core GGUF Model ──────────────────────────────────────

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sauti_app_sauti_SautiEngineLoader_nativeSwitchCoreModel(
    JNIEnv* env, jobject /* this */, jstring jPath
) {
    if (!jPath) return JNI_FALSE;
    const char* cpath = env->GetStringUTFChars(jPath, nullptr);
    if (!cpath) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_engine_mutex);

    // Release existing context & model
    if (g_llama_context) {
        llama_free(g_llama_context);
        g_llama_context = nullptr;
    }
    if (g_llama_model) {
        llama_model_free(g_llama_model);
        g_llama_model = nullptr;
    }

    g_sauti_core_path = std::string(cpath);
    env->ReleaseStringUTFChars(jPath, cpath);
    g_sauti_core_initialized = false;

    LOGI("Switching core model to: %s", g_sauti_core_path.c_str());
    ensureLlamaModelLoaded();
    g_sauti_core_initialized = (g_llama_model != nullptr && g_llama_context != nullptr);
    return g_sauti_core_initialized ? JNI_TRUE : JNI_FALSE;
}
