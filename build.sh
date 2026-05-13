#!/bin/bash

# XD Mock GPS Build Script
# 用于构建多版本Release APK

set -e

echo "=========================================="
echo "XD Mock GPS - Release Build Script"
echo "=========================================="

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    else
        echo "Error: Android SDK not found. Please set ANDROID_HOME"
        exit 1
    fi
fi

echo "Using ANDROID_HOME: $ANDROID_HOME"

# 检查Gradle
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found"
    exit 1
fi

chmod +x ./gradlew

# 清理并构建
echo ""
echo "Building Release APK..."
echo ""

./gradlew clean assembleRelease

echo ""
echo "=========================================="
echo "Build completed!"
echo "=========================================="
echo ""
echo "APK files are located at:"
echo "app/build/outputs/apk/release/"
echo ""
echo "To list all APKs:"
ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "No APKs found"
