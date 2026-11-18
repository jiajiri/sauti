# Sauti

Sauti is an offline voice assistant and speech recognition app for Android. It runs Swahili and English speech-to-text and language models directly on the device using C++ (`whisper.cpp` and `llama.cpp`).

It does not require internet access and does not send any voice or text data off the phone.

---

## Install APK

Scan this QR code with your phone camera or click the link below to download the latest working APK:

<p align="center">
  <a href="https://github.com/jiajiri/sauti/releases/latest">
    <img src="docs/assets/download_qr.png" width="180" alt="Scan to install Sauti APK" />
  </a>
  <br>
  <a href="https://github.com/jiajiri/sauti/releases/latest"><b>Download Latest APK Release</b></a>
</p>

- Mobile (Android): Scan the QR code with your camera app, download `sauti-v1.0.0-arm64-v8a.apk`, and tap to install.
- Desktop (macOS / Linux / Windows): Scan the QR code directly from your monitor, or download the APK file and install via USB using ADB:
  ```bash
  adb install -r sauti-v1.0.0-arm64-v8a.apk
  ```

---

## Why Sauti?

Most voice assistants require a fast internet connection and costly cloud APIs. In East Africa, mobile data is expensive and network coverage is often intermittent. Sauti is built to work reliably without a network connection:

- Offline by design: Runs entirely on-device. The app does not request internet permission in AndroidManifest.xml.
- Swahili and English: Built to understand conversational Swahili, East African English, and mixed sentences (code-switching).
- Lightweight: Runs within 250MB of RAM, making it suitable for budget Android devices with 2GB to 4GB of memory.
- Private: All audio processing and chat history stay on local device storage with SQLite encryption.

---

## How It Works

Sauti captures microphone audio via OpenSL ES, converts it into log-mel spectrograms using ARM NEON SIMD instructions, and feeds it into whisper.cpp for acoustic decoding.

```
[Microphone Audio] (16kHz 16-bit PCM)
         |
         v
[OpenSL ES / Oboe Native Ring Buffer]
         |
         v
[DSP Preprocessing] (Pre-emphasis, Mel Filterbank via ARM NEON)
         |
         v
[whisper.cpp Core] (4-bit quantized acoustic model)
         |
         v
[Bilingual Dispatch] (Detects Swahili vs English)
         |
         v
[llama.cpp Runtime / Jetpack Compose UI]
```

- Audio Ingestion: Low-latency capture directly in C++ via OpenSL ES.
- Speech Recognition: Quantized Whisper models running with NEON SIMD vectorization.
- Language Handling: Fast dictionary and phonetic heuristics for Swahili-English code-switching.
- UI: Built with modern Jetpack Compose (Material 3) and an obsidian AMOLED dark theme.

---

## Requirements

- Android device or emulator running Android 7.0+ (API 24 or higher)
- Android Studio Ladybug (2024.2.1+) or command-line SDK tools
- Android NDK `26.1.10909125`
- CMake `3.22.1+`
- JDK 17

---

## Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/jiajiri/sauti.git
cd sauti
```

### 2. Configure environment (optional)
Copy the example environment file for local builds:
```bash
cp .env.example .env
```

### 3. Build the APK
```bash
cd android
./gradlew assembleRelease
```

Built APKs will be located at:
- `android/app/build/outputs/apk/release/sauti-v1.0.0-x86_64.apk` (for emulator)
- `android/app/build/outputs/apk/release/sauti-v1.0.0-arm64-v8a.apk` (for physical ARM devices)
- `android/app/build/outputs/apk/release/sauti-v1.0.0-universal.apk` (universal APK)

### 4. Install and Run
```bash
adb install -r app/build/outputs/apk/release/sauti-v1.0.0-x86_64.apk
adb shell am start -n com.sauti.app/com.sauti.app.MainActivity
```

---

## Project Structure

```
.
|-- android/                             # Android application root
|   |-- app/
|   |   |-- build.gradle                 # Gradle build and NDK toolchain configuration
|   |   `-- src/main/
|   |       |-- AndroidManifest.xml      # Air-gapped manifest with zero internet permission
|   |       |-- cpp/                     # C++20 NDK core (whisper.cpp & llama.cpp)
|   |       `-- java/com/sauti/app/      # Kotlin application code & Jetpack Compose UI
|-- benchmarks/                          # Latency benchmarks & evaluation scripts
|-- docs/                                # Project documentation & community guides
|   |-- assets/                          # Static assets (download QR code)
|   |-- CODE_OF_CONDUCT.md               # Community standards and pledge
|   |-- CONTRIBUTING.md                  # Development setup and pull request guide
|   |-- SECURITY.md                      # Vulnerability reporting and security policy
|   `-- DEVELOPER_API_ACCESS.md          # Model delivery and R2 developer guide
|-- scripts/                             # Provisioning and model conversion utilities
|   `-- push_models_to_device.sh         # Helper to stage offline weights on device
|-- LICENSE                              # Apache 2.0 open-source license
`-- README.md                            # Executive overview and installation guide
```

---

## Documentation and Community

- Architecture and design: See [docs/](docs/)
- Contributing guidelines: See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)
- Code of conduct: See [docs/CODE_OF_CONDUCT.md](docs/CODE_OF_CONDUCT.md)
- Security policy: See [docs/SECURITY.md](docs/SECURITY.md)
- Model delivery setup: See [docs/DEVELOPER_API_ACCESS.md](docs/DEVELOPER_API_ACCESS.md)
- UI Components: Sauti includes an Airbnb Showkase browser for previewing Compose components. Run `ShowkaseBrowserActivity` in debug builds.
- API Reference: Dokka HTML documentation can be built via `./gradlew dokkaHtml`.

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
