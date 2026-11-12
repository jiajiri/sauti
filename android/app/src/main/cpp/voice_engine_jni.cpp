#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <algorithm>
#include <sstream>
#include <iomanip>
#include <mutex>
#include <atomic>
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
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
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

    // Evaluate prompt tokens using llama_batch_get_one
    struct llama_batch prompt_batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
    if (llama_decode(g_llama_context, prompt_batch) != 0) {
        LOGE("Failed to decode prompt.");
        return;
    }

    // Init greedy sampler
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    // Generate up to 256 tokens
    int n_cur = prompt_tokens.size();
    for (int iter = 0; iter < 256; ++iter) {
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

        struct llama_batch single_batch = llama_batch_get_one(&new_token_id, 1);
        if (llama_decode(g_llama_context, single_batch) != 0) {
            LOGE("Failed to decode token during generation.");
            break;
        }
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
