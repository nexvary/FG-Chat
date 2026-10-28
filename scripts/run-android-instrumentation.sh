#!/usr/bin/env bash
# Disposable CI emulator only. Keep test failure status and collect screenshots before emulator shutdown.
set -uo pipefail
test_status=0
./gradlew :android-app:connectedDebugAndroidTest --no-daemon || test_status=$?
mkdir -p evidence/android-35
screenshot_status=0
adb pull /sdcard/Download/fgchat-test-evidence evidence/android-35 || screenshot_status=$?
if [ "$test_status" -ne 0 ]; then exit "$test_status"; fi
exit "$screenshot_status"
