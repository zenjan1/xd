#!/bin/bash
# MockGPS 构建脚本

set -e

echo "=========================================="
echo "MockGPS 构建脚本"
echo "=========================================="

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    fi
fi

if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo "错误: 未找到Android SDK"
    echo "请设置 ANDROID_HOME 环境变量"
    exit 1
fi

echo "使用 ANDROID_HOME: $ANDROID_HOME"

# 检查Java
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi

echo "使用 JAVA_HOME: $JAVA_HOME"

# 清理并构建
echo ""
echo "开始构建Release APK..."
echo ""

./gradlew clean assembleRelease

echo ""
echo "=========================================="
echo "构建完成!"
echo "=========================================="
echo ""
echo "APK文件位于: app/build/outputs/apk/release/"
echo ""
ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "APK未生成"
