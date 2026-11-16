# Contributing to Sauti

Thank you for your interest in contributing to Sauti. Sauti is an open-source, offline voice assistant and edge-AI platform for Android.

We welcome contributions from developers, researchers, linguists, and community members.

---

## Getting Started

1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/sauti.git
   cd sauti
   ```
3. Create a descriptive feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```

---

## Development Environment

### Requirements
- Android Studio Ladybug (2024.2.1+) or newer
- Android NDK `26.1.10909125`
- CMake `3.22.1+`
- JDK 17 (OpenJDK or Azul Zulu)
- Android SDK with platform API 34 installed

### Building the Project
Navigate to the `android/` directory and run:
```bash
cd android
./gradlew assembleDebug
```

### Running Unit Tests
Before submitting changes, ensure all unit tests pass:
```bash
./gradlew testDebugUnitTest
```

---

## Contribution Guidelines

### 1. Code Standards
- **Kotlin**: Follow official Kotlin coding conventions. Use Jetpack Compose for UI development. Keep composables stateless where possible and hoist state to ViewModels.
- **C++ (NDK Core)**: Follow the C++20 standard. Audio capture and DSP code must remain thread-safe, lock-free on the real-time audio thread, and avoid unnecessary heap allocations.
- **Security & Privacy**: Sauti is an air-gapped application. Never add `android.permission.INTERNET` to `AndroidManifest.xml` or introduce network telemetry. Never commit API keys, tokens, or personal credentials to git.

### 2. Commit Messages
Write clear, conventional commit messages:
- `feat(audio): add ring buffer overflow protection`
- `fix(nlp): resolve Swahili code-switching tie-breaker edge case`
- `docs(readme): update build instructions for NDK r26`
- `perf(simd): optimize mel spectrogram vectorization`

### 3. Pull Request Process
1. Push your branch to your GitHub fork:
   ```bash
   git push origin feat/your-feature-name
   ```
2. Open a Pull Request against the `core` branch of `jiajiri/sauti`.
3. Provide a clear description of what the PR changes, why it is needed, and how it was tested.
4. Maintainers will review your PR and provide constructive feedback.

---

## Reporting Issues

If you find a bug or have a feature request:
- Search existing GitHub Issues to see if it has already been reported.
- If not, open a new Issue providing reproduction steps, device specifications (Android version, device model, RAM), and logcat output if applicable.

---

## License

By contributing to Sauti, you agree that your contributions will be licensed under the project's [Apache License, Version 2.0](LICENSE).
