#!/bin/bash
set -e

# ---- Paths ----
SDK_ROOT="/usr/lib/android-sdk"
BUILD_TOOLS="$SDK_ROOT/build-tools/34.0.0"
PLATFORM_JAR="$SDK_ROOT/platforms/android-34/android.jar"

SRC_DIR="./app/src/main/java"
MANIFEST="./app/src/main/AndroidManifest.xml"

BUILD_DIR="./app/build"
OUT_CLASSES="$BUILD_DIR/classes"
OUT_DEX="$BUILD_DIR/dex"
OUT_APK="$BUILD_DIR/com.joshuagranick.btautopin.apk"
APK_UNSIGNED="$BUILD_DIR/output-unsigned.apk"

PACKAGE_NAME="com.joshuagranick.btautopin"

KEYSTORE="./debug.keystore"
KEY_ALIAS="androiddebugkey"
KEY_PASS="android"

mkdir -p "$OUT_CLASSES" "$OUT_DEX" "$BUILD_DIR"

echo "=== 1. Compiling Java sources ==="
javac \
  -source 17 \
  -target 17 \
  -cp "$PLATFORM_JAR" \
  -d "$OUT_CLASSES" \
  $(find "$SRC_DIR" -name "*.java")

echo "=== 2. Packaging classes into JAR ==="
CLASSES_JAR="$BUILD_DIR/classes.jar"
jar cf "$CLASSES_JAR" -C "$OUT_CLASSES" .

echo "=== 3. Converting JAR to DEX ==="
"$BUILD_TOOLS/d8" \
  --min-api 21 \
  --output "$OUT_DEX" \
  --classpath "$PLATFORM_JAR" \
  "$CLASSES_JAR"

echo "=== 4. Packaging APK (unsigned, with aapt) ==="
aapt package \
  -f \
  -M "$MANIFEST" \
  -I "$PLATFORM_JAR" \
  -F "$APK_UNSIGNED"

echo "=== 5. Adding classes.dex ==="
zip -q -j "$APK_UNSIGNED" "$OUT_DEX/classes.dex"

echo "=== 6. Aligning APK ==="
"$BUILD_TOOLS/zipalign" -f -p 4 \
  "$APK_UNSIGNED" \
  "$OUT_APK"

echo "=== 7. Signing APK ==="
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  "$OUT_APK"

echo
echo "✅ APK build complete:"
echo "   $OUT_APK"
