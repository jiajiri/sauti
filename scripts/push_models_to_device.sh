#!/bin/bash
set -e

# ==============================================================================
# Sauti Offline Model Provisioning Tool
# Stages local model weights to a connected Android phone or emulator.
# Obfuscates all model filenames into standard internal identifiers.
# ==============================================================================

echo "======================================================"
echo "Sauti Offline Model Provisioning Tool"
echo "======================================================"

ADB_BIN=$(which adb || echo "/home/laphilemon/Android/Sdk/platform-tools/adb")
if [ ! -x "$ADB_BIN" ]; then
    echo "Error: adb not found. Ensure Android platform-tools are installed and in your PATH."
    exit 1
fi

DEVICE=$($ADB_BIN devices | grep -w "device" | head -n 1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "Error: No connected Android device found via ADB."
    echo "Please plug in your phone via USB with USB Debugging enabled, or boot an emulator."
    exit 1
fi
echo "Target Android Device: $DEVICE"

MODELS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/models_store"

# Find models dynamically by file type without hardcoded names
VOICE_SRC=$(find "$MODELS_DIR" -type f -name "*.bin" | head -n 1)
CORE_SRC=$(find "$MODELS_DIR" -type f -name "*.gguf" | head -n 1)

STAGE_DIR="/sdcard/Download/sauti_models"
$ADB_BIN -s "$DEVICE" shell mkdir -p "$STAGE_DIR"

if [ -n "$VOICE_SRC" ] && [ -f "$VOICE_SRC" ]; then
    echo "Staging voice component ($(du -h "$VOICE_SRC" | awk '{print $1}'))..."
    $ADB_BIN -s "$DEVICE" push "$VOICE_SRC" "$STAGE_DIR/voice_lite.bin"
else
    echo "Notice: Voice component not found in models_store/."
fi

if [ -n "$CORE_SRC" ] && [ -f "$CORE_SRC" ]; then
    echo "Staging core engine component ($(du -h "$CORE_SRC" | awk '{print $1}'))..."
    $ADB_BIN -s "$DEVICE" push "$CORE_SRC" "$STAGE_DIR/core_pro.bin"
else
    echo "Notice: Core engine component not found in models_store/."
fi

echo "======================================================"
echo "Staging complete on device: $STAGE_DIR"
echo "When Sauti is launched on the phone:"
echo "1. The app imports the components into its private app sandbox (/data/data/com.sauti.app/files/sauti/)."
echo "2. The public files in $STAGE_DIR are automatically deleted to protect your models."
echo "======================================================"
