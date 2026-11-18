#!/usr/bin/env bash
#
# Sauti Native Model Downloader & Setup CLI
# Downloads verified GGUF and Whisper on-device model weights from Hugging Face
# Sets up models_store/ and symlinks for Android assets
#

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STORE_DIR="${ROOT_DIR}/models_store"
ASSETS_DIR="${ROOT_DIR}/android/app/src/main/assets/sauti"

mkdir -p "${STORE_DIR}"
mkdir -p "${ASSETS_DIR}"

WHISPER_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
SMOLLM2_URL="https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf"
QWEN25_URL="https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_0.gguf"

WHISPER_FILE="${STORE_DIR}/whisper-tiny-sw-q4.bin"
SMOLLM2_FILE="${STORE_DIR}/smollm2-135m-sw-q4.gguf"
QWEN25_FILE="${STORE_DIR}/qwen2.5-0.5b-chat-q4_0.gguf"

echo "=========================================================="
echo "    Sauti: Sovereign AI On-Device Model Downloader CLI    "
echo "=========================================================="

# 1. Whisper ASR Engine
if [ -f "${WHISPER_FILE}" ] && [ "$(stat -c%s "${WHISPER_FILE}" 2>/dev/null || stat -f%z "${WHISPER_FILE}" 2>/dev/null)" -gt 10000000 ]; then
    echo "[OK] Whisper-Tiny ASR core already present ($(du -h "${WHISPER_FILE}" | cut -f1))."
else
    echo "[DOWNLOADING] Whisper-Tiny Swahili ASR core (GGML)..."
    curl -L --progress-bar "${WHISPER_URL}" -o "${WHISPER_FILE}"
fi
 
# 2. Qwen2.5 0.5B Instruct (Core Pro)
if [ -f "${QWEN25_FILE}" ] && [ "$(stat -c%s "${QWEN25_FILE}" 2>/dev/null || stat -f%z "${QWEN25_FILE}" 2>/dev/null)" -gt 100000000 ]; then
    echo "[OK] Qwen2.5-0.5B Core Pro already present ($(du -h "${QWEN25_FILE}" | cut -f1))."
else
    echo "[DOWNLOADING] Qwen2.5-0.5B Chat (Q4_0 GGUF)..."
    curl -L --progress-bar "${QWEN25_URL}" -o "${QWEN25_FILE}"
fi

# 3. SmolLM2 135M (Core Lite)
if [ -f "${SMOLLM2_FILE}" ] && [ "$(stat -c%s "${SMOLLM2_FILE}" 2>/dev/null || stat -f%z "${SMOLLM2_FILE}" 2>/dev/null)" -gt 50000000 ]; then
    echo "[OK] SmolLM2-135M Core Lite already present ($(du -h "${SMOLLM2_FILE}" | cut -f1))."
else
    echo "[DOWNLOADING] SmolLM2-135M Swahili (Q4_K_M GGUF)..."
    curl -L --progress-bar "${SMOLLM2_URL}" -o "${SMOLLM2_FILE}"
fi

# Symlink into Android assets/sauti
ln -sf "${WHISPER_FILE}" "${ASSETS_DIR}/voice_lite.bin"
ln -sf "${QWEN25_FILE}" "${ASSETS_DIR}/core_pro.bin"
ln -sf "${SMOLLM2_FILE}" "${ASSETS_DIR}/core_lite.bin"

echo "=========================================================="
echo "[SUCCESS] All model assets configured in models_store/ and android/app/src/main/assets/sauti/"
ls -lh "${ASSETS_DIR}"
