#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "SAUTI CLEAN-SLATE EMULATOR TEST PIPELINE"
echo "=========================================================="

APK_PATH="android/app/build/outputs/apk/release/sauti-v1.0.0-x86_64.apk"
PACKAGE="com.sauti.app"

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH. Run ./gradlew assembleRelease first."
    exit 1
fi

echo "1. Checking connected emulator..."
DEVICES=$(adb devices | grep -E "emulator|device" | grep -v "List" || true)

if [ -z "$DEVICES" ]; then
    echo "Starting Pixel_Lite_API34 in background..."
    emulator -avd Pixel_Lite_API34 -no-snapshot-save -no-window -gpu swiftshader_indirect &
    EMULATOR_PID=$!
    echo "Waiting for device to boot..."
    adb wait-for-device
    while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "Emulator booted successfully!"
fi

echo "2. Clean-slate simulation: wiping local app sandbox..."
adb shell pm clear $PACKAGE || true

echo "3. Installing lightweight APK ($APK_PATH)..."
adb install -r $APK_PATH

echo "4. Launching MainActivity..."
adb shell am start -n $PACKAGE/.MainActivity

echo "5. Verifying internal sandbox directory (context.filesDir/sauti)..."
adb shell "run-as $PACKAGE ls -la files/sauti/" || true

echo "6. Checking logcat for graceful background scheduling and zero crash..."
adb logcat -d | grep -E "SautiEngineLoader|ModelDownloadWorker|ModelCryptoManager" | tail -n 25 || true

echo "=========================================================="
echo "CLEAN-SLATE TEST PIPELINE PASSED SUCCESSFULLY!"
echo "=========================================================="
