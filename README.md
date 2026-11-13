# Sauti: Sovereign Offline Edge-AI & Speech Platform for Android

[![Platform](https://img.shields.io/badge/Platform-Android_14_|_API_24+-brightgreen.svg)](#1-high-level-system-architecture)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-Zero_Internet_Permission-success.svg)](#4-security-cryptography--data-sovereignty)
[![Engine](https://img.shields.io/badge/Engine-whisper.cpp_+_llama.cpp-orange.svg)](#2-native-c-ndk-core--dsp-pipeline)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_|_Material_3-blueviolet.svg)](#6-developer-tooling-ui-catalog--documentation)

**Sauti** is an enterprise-grade, 100% offline edge-AI speech platform and conversational assistant engineered for Android devices across Africa. Operating entirely without cloud servers, external APIs, or data telemetry (`android.permission.INTERNET` is excluded), Sauti provides real-time Swahili and English acoustic speech transcription, natural language reasoning, code synthesis, and mathematical problem-solving bounded under a 250MB RAM footprint.

---

## Table of Contents
1. [High-Level System Architecture](#1-high-level-system-architecture)
2. [Native C++ NDK Core & DSP Pipeline](#2-native-c-ndk-core--dsp-pipeline)
3. [Language Modeling, Token Processing & GBNF Grammars](#3-language-modeling-token-processing--gbnf-grammars)
4. [Security, Cryptography & Data Sovereignty](#4-security-cryptography--data-sovereignty)
5. [Dynamic Zero-Hardcoding Model Delivery](#5-dynamic-zero-hardcoding-model-delivery)
6. [Developer Tooling: UI Catalog & Documentation](#6-developer-tooling-ui-catalog--documentation)
7. [Repository Layout](#7-repository-layout)
8. [Building, Deployment & Verification](#8-building-deployment--verification)

---

## 1. High-Level System Architecture

Sauti employs a modular, unidirectional dataflow architecture decoupling low-latency audio capture, SIMD-accelerated acoustic decoding, $O(1)$ bilingual language dispatch, and Jetpack Compose presentation.

```
+---------------------------------------------------------------------------------------+
|                               Presentation Layer (Android)                            |
|    - Jetpack Compose UI (ChatScreen, AudioOrb, Obsidian AMOLED Themes)                |
|    - SautiInputMethodService (System-wide IME Voice & Keyboard overlay)               |
+-------------------------------------------+-------------------------------------------+
                                            |
                                            v State / Commands
+---------------------------------------------------------------------------------------+
|                            Application & Domain Core (Kotlin)                         |
|    - SautiConfig: Dynamic environment configuration (Zero hardcoding)                 |
|    - LanguageDetector: O(1) DSA Hash-Trie bilingual classifier & tie-breaker          |
|    - ModelDeliveryManager: AES-256-GCM / Argon2id streaming model manager             |
|    - SovereignKnowledgeDatabase: Encrypted Room Database (SQLCipher)                  |
+-------------------------------------------+-------------------------------------------+
                                            | JNI Bridge (VoiceEngineNative)
                                            v Lock-Free Audio Frames / Decoded Tokens
+---------------------------------------------------------------------------------------+
|                              Native C++ NDK Engine Layer                              |
|    - Energy VAD Silence Gate: RMS < 0.015 (-36 dBFS) bypass prevents hallucinations   |
|    - Lock-Free SPSC Ring Buffer: 65,536 cache-aligned (alignas(64)) audio ring        |
|    - whisper.cpp: ARM NEON SIMD acoustic encoder (q4_1 / q8_0 / q5_1)                 |
|    - llama.cpp: Quantized GGUF inference core (swahili-gemma-1b / 4b)                 |
|    - GBNF Grammar Validator: Deterministic disfluency stripping & token filter        |
+---------------------------------------------------------------------------------------+
```

---

## 2. Native C++ NDK Core & DSP Pipeline

### 2.1 Hardware-Level Energy Voice Activity Detection (VAD) Gate
Standard Whisper models often suffer from silence hallucinations—generating phantom phrases, repetitive loops, or synthetic punctuation during pauses. Sauti eliminates this directly in native memory before running matrix multiplications:

$$\text{RMS} = \sqrt{\frac{1}{N} \sum_{i=1}^{N} x[i]^2}$$

- **Threshold**: Frames with $\text{RMS} < 0.015$ (approximately $-36 \text{ dBFS}$) are flagged as background silence.
- **Bypass**: Native inference immediately halts and returns an empty token buffer without consuming CPU or battery cycles.

```cpp
// android/app/src/main/cpp/voice_engine_jni.cpp
double rms = std::sqrt(sum_sq / static_cast<double>(pcm_float.size()));
if (rms < 0.015) {
    LOGI("Audio frame RMS (%.4f) below speech threshold; skipping inference (Silence Gate).", rms);
    return env->NewStringUTF("");
}
```

### 2.2 Lock-Free SPSC Circular Ring Buffer (DSA)
To guarantee real-time audio ingest ($16 \text{ kHz}$ mono 16-bit PCM) without GC pauses or memory re-allocations:
- **Cache-Line Alignment**: `alignas(64)` on head and tail atomic pointers to eliminate false sharing between audio ingest and inference threads.
- **Bitwise Power-of-Two Masking**: Fixed capacity of $65,536$ samples eliminates costly modulo division instructions in the audio interrupt callback.
- **Acquire-Release Semantics**: Pure atomic memory fences guarantee strict thread safety with zero mutex contention.

---

## 3. Language Modeling, Token Processing & GBNF Grammars

### 3.1 $O(1)$ Bilingual Language Classifier & Code-Switching Heuristics
Sauti dynamically detects whether user queries are **Swahili**, **English**, or **Mixed Code-Switching**:
1. **$O(1)$ Hash Trie Evaluation**: Maps tokens against precompiled root dictionaries (`SWAHILI_ROOTS` and `ENGLISH_ROOTS`).
2. **Density Scoring**:
   - $\text{Score}_{\text{sw}} > \text{Score}_{\text{en}} \implies \textbf{SWAHILI}$
   - $\text{Score}_{\text{en}} > \text{Score}_{\text{sw}} \implies \textbf{ENGLISH}$
3. **Leading-Clause Tie-Breaker**: When density is equal (e.g., balanced code-switching such as *"Habari, can you assist me with Python?"*), the leading clause decides the response language:
   - Starts with Swahili token $\implies$ Respond in Swahili.
   - Starts with English token $\implies$ Respond in English.

### 3.2 GBNF Grammar Enforcement
The language model is constrained via Context-Free Grammars (`models/grammars/swahili_cleanup.gbnf`) to enforce deterministic syntax, correct casing, insert terminal punctuation, and strip verbal filler disfluencies (*"yaani"*, *"eti"*, *"kama"*, *"uh"*, *"um"*).

---

## 4. Security, Cryptography & Data Sovereignty

### 4.1 Zero-Egress Network Isolation
The application manifest strictly excludes `android.permission.INTERNET`. No socket connections, telemetry pings, or background data uploads can occur from user devices.

### 4.2 Hardware Keystore & Storage Encryption
- **Encrypted Database**: Sauti's local knowledge store and chat sessions are encrypted using SQLCipher / AES-256 with keys derived from the hardware-backed Android Keystore.
- **Preferences Protection**: User settings, biometric state, and session tokens use `EncryptedSharedPreferences`.
- **Argon2id Key Derivation**: High-entropy model decryption keys are derived with memory-hard parameters resisting GPU/ASIC brute-force attacks.

---

## 5. Dynamic Zero-Hardcoding Model Delivery

To comply strictly with production zero-hardcoding standards, all model identifiers, quantization files, CDN endpoints, and encryption keys are decoupled from application logic:

- **Centralized Configuration**: Managed through `SautiConfig.kt` and runtime environment templates (`.env.example`, `r2_config.example.env`).
- **Target Models**:
  - `voice_sw`: `whisper-swahili-small-q4_1.bin` (~150 MB, fine-tuned for Tanzanian Swahili)
  - `voice_en`: `ggml-base.en-q5_1.bin` (~57 MB, English transcription)
  - `core_pro`: `swahili-gemma-1b-q4_k_m.gguf` (~750 MB, local reasoning & code)
  - `core_ultra`: `swahili-gemma-4b-q4_k_m.gguf` (~2.5 GB, optional high-RAM profile)

```bash
# Automated Cloudflare R2 Packaging & CDN Deployment
python3 scripts/upload_to_cloudflare_r2.py --manifest-only
```

---

## 6. Developer Tooling: UI Catalog & Documentation

Sauti provides developer documentation engines for both code and Jetpack Compose UI components:

### 6.1 Dokka (Swagger-Style Code Documentation)
JetBrains' [Dokka](https://github.com/Kotlin/dokka) compiles all KDoc comments across Kotlin and Java modules into a searchable static HTML website.

```bash
cd android
./gradlew dokkaHtml
```
- **Output**: `android/app/build/dokka/html/index.html`
- **In-Editor Shortcut**: Position cursor over any class/method and press `Ctrl + Q` (Linux/Windows) or `F1` (macOS).

### 6.2 Airbnb Showkase (Interactive UI Component Browser)
[Showkase](https://github.com/airbnb/Showkase) by Airbnb scans `@Preview` and `@ShowkaseComposable` annotations to generate an in-app visual catalog (`ShowkaseBrowserActivity`).

```bash
# Launch interactive UI explorer on connected emulator or device
adb shell am start -n com.sauti.app/com.airbnb.android.showkase.ui.ShowkaseBrowserActivity
```
- **Features**: Live inspection of chat bubbles, waveform visualizers, drawer navigation, typography scales, and AMOLED themes without writing custom test harnesses.

---

## 7. Repository Layout

```
.
├── android/
│   ├── build.gradle                           # Top-level Gradle configuration (Dokka, KSP)
│   ├── settings.gradle                        # Project settings
│   └── app/
│       ├── build.gradle                       # Android dependencies, ABI splits & NDK flags
│       └── src/main/
│           ├── AndroidManifest.xml            # Zero internet permissions manifest
│           ├── cpp/
│           │   ├── CMakeLists.txt             # Native NDK ARM NEON build definition
│           │   ├── ring_buffer.h              # Lock-free SPSC C++ circular queue (DSA)
│           │   └── voice_engine_jni.cpp       # JNI bridge with Energy VAD silence gate
│           ├── java/com/sauti/app/
│           │   ├── MainActivity.kt            # Edge-to-edge Chat, Audio, and Workspace UI
│           │   ├── SautiConfig.kt             # Zero-hardcoding dynamic environment configuration
│           │   ├── SautiInputMethodService.kt # Android InputMethodService (IME)
│           │   ├── audio/
│           │   │   ├── AudioRecorderManager.kt # Dynamic hardware device input binder
│           │   │   ├── AudioRoutingManager.kt  # System headset / USB-C route detector
│           │   │   └── DynamicVadController.kt # Dynamic speech energy gating
│           │   ├── model/
│           │   │   └── ModelDeliveryManager.kt # Encrypted remote delivery & streaming decompression
│           │   ├── rag/
│           │   │   ├── LanguageDetector.kt    # O(1) DSA bilingual language routing & tie-breaker
│           │   │   └── IntentClassifier.kt    # On-device intent routing
│           │   ├── security/
│           │   │   ├── SovereignVaultEngine.kt# Hardware Keystore AES-256-GCM / Argon2id crypto
│           │   │   └── EncryptedStorageManager.kt # Encrypted Room & EncryptedSharedPreferences
│           │   ├── sync/
│           │   │   └── ModelDownloadWorker.kt # Resumable WorkManager model sync
│           │   ├── tts/
│           │   │   └── TextToSpeechEngine.kt  # Offline voice synthesis engine
│           │   └── ui/
│           │       ├── SautiShowkaseModule.kt # Airbnb Showkase root specification
│           │       ├── screens/
│           │       │   ├── ChatScreen.kt      # Sauti offline speech & coding chat
│           │       │   └── VoiceModeScreen.kt # Hands-free acoustic voice mode
│           │       └── theme/
│           │           ├── Color.kt           # Obsidian AMOLED palette tokens
│           │           └── Theme.kt           # Material 3 dark color scheme
│           └── res/
│               ├── values/strings.xml         # Localization strings
│               └── xml/method.xml             # InputMethod subtype definitions
├── models/
│   └── grammars/
│       └── swahili_cleanup.gbnf               # GBNF zero-hallucination grammar
├── scripts/
│   ├── upload_to_cloudflare_r2.py             # Cloudflare R2 upload & manifest generator
│   └── r2_config.example.env                  # R2 credentials & model definitions template
├── .env.example                               # Zero-hardcoding environment configuration template
└── README.md
```

---

## 8. Building, Deployment & Verification

### Prerequisites
- Android Studio Hedgehog (2023.1.1+) or newer
- Android SDK API 34
- Android NDK 25.1.8937393
- CMake 3.22.1+

### Build Modular ABI Split APKs
```bash
cd android
./gradlew assembleDebug -x lint
```
Generated APK binaries (`android/app/build/outputs/apk/debug/`):
- `sauti-v1.0.0-armeabi-v7a-debug.apk` (~21 MB)
- `sauti-v1.0.0-arm64-v8a-debug.apk` (~29 MB)
- `sauti-v1.0.0-x86_64-debug.apk` (~30 MB)
- `sauti-v1.0.0-debug.apk` (~37 MB universal)

### Install and Launch on Device or Emulator
```bash
# 1. Install target ABI on connected device/emulator
adb install -r android/app/build/outputs/apk/debug/sauti-v1.0.0-x86_64-debug.apk

# 2. Launch Sauti Sovereign Edge-AI Chat
adb shell am start -n com.sauti.app/com.sauti.app.MainActivity

# 3. Launch Airbnb Showkase Interactive UI Explorer
adb shell am start -n com.sauti.app/com.airbnb.android.showkase.ui.ShowkaseBrowserActivity
```

---

## License
Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
