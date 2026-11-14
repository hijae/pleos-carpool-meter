#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug "$@"
sdk_path="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -n "$sdk_path" && -x "$sdk_path/build-tools/35.0.0/apksigner" ]]; then
  "$sdk_path/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
fi
