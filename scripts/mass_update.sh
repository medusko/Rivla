#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

ANDROID_VERSION=$(grep 'versionName =' "$ROOT_DIR/app/build.gradle.kts" | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')

echo "=========================================================="
echo "🚀 Rivla Build, Release & Device Deploy (v$ANDROID_VERSION)"
echo "=========================================================="

if [[ -z "${JAVA_HOME:-}" || ! -d "$JAVA_HOME" ]]; then
  if [[ -d "/opt/homebrew/opt/openjdk@21" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
  elif [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
fi

./gradlew testDebugUnitTest assembleDebug assembleRelease

APK_SRC="app/build/outputs/apk/release/app-release.apk"
APK_DEST="Rivla-Android-v${ANDROID_VERSION}.apk"
cp "$APK_SRC" "$APK_DEST"
echo "✅ Built $APK_DEST ($(du -h "$APK_DEST" | cut -f1))"
