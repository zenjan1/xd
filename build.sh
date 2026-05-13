#!/bin/bash
# MockGPS 一键构建脚本

set -e

echo "=========================================="
echo "MockGPS 构建脚本 v1.0"
echo "=========================================="
echo ""

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    else
        echo "错误: 未找到Android SDK"
        echo "请设置 ANDROID_HOME 环境变量"
        exit 1
    fi
fi

echo "[INFO] Android SDK: $ANDROID_HOME"

# 检查Java
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    export JAVA_HOME
fi

echo "[INFO] JAVA_HOME: $JAVA_HOME"

# 检查gradle
if [ ! -f "./gradlew" ]; then
    echo "错误: gradlew 不存在"
    exit 1
fi

echo "[INFO] 开始构建..."
echo ""

# 清理并构建
./gradlew clean assembleRelease

echo ""
echo "=========================================="
echo "构建完成!"
echo "=========================================="
echo ""
echo "APK文件位于:"
echo "  app/build/outputs/apk/release/"
echo ""
ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "APK未生成"
