# Sauti: Sovereign Offline African Language AI Platform for Android

[![Platform](https://img.shields.io/badge/Platform-Android_14_|_API_24+-brightgreen.svg)](#system-architecture)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-Zero_Internet_Permission-success.svg)](#data-sovereignty-and-security)
[![Engine](https://img.shields.io/badge/Engine-whisper.cpp_+_llama.cpp-orange.svg)](#edge-inference-engine)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_|_Material_3-blueviolet.svg)](#user-interface-and-experience)

**Sauti** is a sovereign, 100% offline edge AI agent and speech platform engineered for entry-level Android devices across Africa. It brings frontier model capabilities—including intelligent coding assistance, mathematical reasoning, Swahili & Sheng normalization, and speech recognition—directly onto mobile hardware without internet connection, API keys, or cloud servers.

Developed to guarantee data sovereignty, privacy, and digital inclusion across bandwidth-constrained regions.

---

## What Sauti Solves

Over 200 million people speak Swahili and regional African dialects (blending Swahili, Sheng, and English). Until now, technical users, developers, educators, and field professionals in Africa have faced severe barriers when accessing frontier AI tools:

1. **Total Cloud Dependency**: Cloud-gated assistants stop functioning when mobile data runs out, connectivity drops in rural zones, or latency spikes.
2. **Heavy Hardware Demands**: Modern multi-billion parameter models require high-end flagship phones (8GB–16GB RAM) or cloud subscriptions, leaving users on standard 2GB–4GB physical RAM devices behind.
3. **Dialect & Context Erasure**: Standard models struggle with everyday African code-switching (*"Niko na code hapa lakini inakataa ku-run"*).
4. **Data Privacy & Surveillance**: Critical medical records, proprietary codebases, passwords, and sensitive conversations are uploaded to third-party servers outside African jurisdiction.

**Sauti solves this by running quantized on-device SLMs (Small Language Models) and Whisper ASR models locally**, bounded strictly under a 250MB memory footprint with zero cloud egress (`android.permission.INTERNET` is not included in the manifest).

---

## Core Capabilities

- **Offline Coding Assistant**: Generates, debugs, and explains code in Kotlin, C/C++, Bash, and SQL on-device.
- **Mathematical & Algorithmic Reasoning**: Provides step-by-step mathematical problem solving, unit conversions, and logical proofs completely offline.
- **African Language & Code-Switching Speech**: Real-time acoustic recognition and normalization for Swahili, Sheng, and English code-switching.
- **Sovereign System Keyboard & Chat**: Works as an on-screen voice keyboard across all Android apps, or as an autonomous conversational studio.

---

## System Architecture

```
+-------------------------------------------------------------------+
|               Android Jetpack Compose UI / IME Layer              |
|        (Edge-to-Edge System Bar, Minimal Scaffolding: 120 FPS)    |
+---------------------------------+---------------------------------+
                                  | Lock-Free SPSC Circular Ring
                                  v
+-------------------------------------------------------------------+
|               Native Audio Producer (16kHz Mono PCM)              |
|             AAudio / AudioRecord Native Ingestion Loop            |
+---------------------------------+---------------------------------+
                                  | SPSC Queue / Zero-Copy Memory Map
                                  v
+-------------------------------------------------------------------+
|                 C++ Native Engine (whisper.cpp NDK)               |
|            ARM NEON SIMD Accelerated Acoustic Encoder             |
+---------------------------------+---------------------------------+
                                  | Raw ASR Token Stream
                                  v
+-------------------------------------------------------------------+
|              GBNF Grammar Post-Processing (llama.cpp)             |
|       Deterministic Casing, Punctuation & Disfluency Removal      |
+---------------------------------+---------------------------------+
                                  | Verified Text / Reasoning
                                  v
+-------------------------------------------------------------------+
|                    InputConnection.commitText()                   |
+-------------------------------------------------------------------+
```

---

## Engineering Features

### 1. Zero Internet Permission (`android.permission.INTERNET` Excluded)
The application manifest intentionally omits internet access:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.sauti.app">
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <!-- Zero network permission: guaranteed local privacy -->
</manifest>
```
All audio processing, feature extraction, code generation, and mathematical reasoning execute locally inside native C++ memory buffers.

### 2. Low-Level DSA: O(1) Lock-Free SPSC Audio Buffer
To eliminate audio frame dropping and prevent Android UI thread stutter, audio samples flow through an $O(1)$ Single-Producer Single-Consumer (SPSC) circular queue (`ring_buffer.h`):
- **Fixed Capacity**: 65,536 samples (power-of-two for bitwise masking without modulo division).
- **Cache-Line Alignment**: `alignas(64)` on head and tail atomic pointers to eliminate false sharing between CPU cores.
- **Acquire-Release Semantics**: Atomic memory fences ensure thread safety without mutexes, yielding zero runtime allocations.

### 3. Native JNI Bridge with ARM NEON Acceleration
The speech processing core compiles directly via CMake with ARM NEON SIMD optimizations (`-O3 -ffast-math -mfpu=neon`), delivering real-time factor (RTF) $< 0.4$ on quad-core ARM Cortex-A53 architectures.

### 4. Zero-Hallucination Grammar Verification
Post-ASR cleanup utilizes Context-Free Grammars (GBNF/EBNF) that restrict the language model to:
- Correcting sentence capitalization.
- Inserting appropriate punctuation marks (`.`, `,`, `?`, `!`).
- Removing Swahili verbal hesitations (*"yaani"*, *"kama"*, *"eti"*, *"uh"*, *"um"*).
- Forbidding token hallucinations (verified via token membership checks).

---

## User Interface & Experience

Designed with a minimal obsidian aesthetic inspired by high-performance mobile AI systems:

| Screen | Core Purpose | Interface Mechanics |
| :--- | :--- | :--- |
| **Sauti Chat** | Private on-device coding, math & reasoning | Edge-to-edge dark background, zero bottom-overlap Scaffolding, dynamic token speed (`t/s`), and interactive history navigation. |
| **Audio Chat** | Voice dictation & continuous conversation | Centered acoustic orb with 5 vertical equalizer bars, dynamic audio decibel scaling, and tap-to-interrupt speech control. |
| **Workspace Drawer** | Session history & multi-workspace switcher | Quick navigation between New Chat, Audio Chat, Library, and Notebooks, plus historical sessions and user profile. |
| **Voice Keyboard (IME)** | System-wide text input across all Android apps | System `InputMethodService` overlay featuring a live Whisper transcription strip, centered voice trigger, and fast punctuation keys. |

---

## Directory Structure

```
.
├── android/
│   ├── build.gradle                           # Top-level Gradle configuration
│   ├── settings.gradle                        # Project settings
│   └── app/
│       ├── build.gradle                       # Android dependencies & NDK toolchain
│       └── src/main/
│           ├── AndroidManifest.xml            # Zero internet permissions manifest
│           ├── cpp/
│           │   ├── CMakeLists.txt             # Native NDK ARM NEON build definition
│           │   ├── ring_buffer.h              # Lock-free SPSC C++ circular queue (DSA)
│           │   └── voice_engine_jni.cpp       # JNI bridge for audio capture & inference
│           ├── java/com/sauti/app/
│           │   ├── MainActivity.kt            # Edge-to-edge Chat, Audio, and Workspace UI
│           │   ├── SautiInputMethodService.kt # Android InputMethodService (IME)
│           │   ├── audio/
│           │   │   ├── AudioRecorderManager.kt # Dynamic hardware device input binder
│           │   │   └── AudioRoutingManager.kt  # System headset / USB-C route detector
│           │   └── ui/
│           │       ├── screens/
│           │       │   ├── ChatScreen.kt      # Sauti offline speech & coding chat
│           │       │   └── VoiceModeScreen.kt # Hands-free acoustic voice mode
│           │       └── theme/
│           │           ├── Color.kt           # Obsidian palette tokens
│           │           └── Theme.kt           # Material 3 dark color scheme
│           └── res/
│               ├── values/strings.xml         # Localization strings
│               └── xml/method.xml             # InputMethod subtype definitions
├── models/
│   └── grammars/
│       └── swahili_cleanup.gbnf               # GBNF zero-hallucination grammar
└── README.md
```

---

## Building and Verification

### Prerequisites
- Android Studio Hedgehog (2023.1.1+) or newer
- Android SDK API 34
- Android NDK 25+
- CMake 3.22.1+

### Debug Build
```bash
cd android
./gradlew assembleDebug
```
The compiled APK will be located at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Installation and Activation
```bash
# 1. Install on connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Enable in Android Settings
# Settings -> System -> Languages & Input -> On-screen keyboard -> Manage on-screen keyboards -> Sauti
```

---

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
