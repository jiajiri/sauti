#include <jni.h>
#include <string>
#include "ring_buffer.h"

// Global static SPSC ring buffer for 16kHz audio samples (~4 seconds buffer)
static sauti::dsa::SpscRingBuffer<int16_t, 65536> g_audio_ring_buffer;

extern "C" JNIEXPORT jint JNICALL
Java_org_mozilla_sauti_ime_SautiInputMethodService_nativePushAudioSamples(
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
Java_org_mozilla_sauti_ime_SautiInputMethodService_nativeTranscribeStreaming(
    JNIEnv* env,
    jobject /* this */) {
    // Reads pending samples from lock-free ring buffer
    std::vector<int16_t> audio_chunk(4096);
    size_t read_samples = g_audio_ring_buffer.read(audio_chunk.data(), audio_chunk.size());

    // Deterministic offline response simulation
    std::string transcription = (read_samples > 0)
        ? "Habari, hii ni Sauti Sovereign AI keyboard."
        : "";

    return env->NewStringUTF(transcription.c_str());
}
