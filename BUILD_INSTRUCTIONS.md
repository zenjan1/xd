# XD Mock GPS - Build Instructions

## 环境要求

- **Android Studio**: Arctic Fox (2020.3.1) 或更高版本
- **JDK**: 1.8+
- **Android SDK**: API 32 (Android 12L)
- **Gradle**: 7.6+ (或使用项目自带的 gradlew)

## 快速构建

### 使用 Android Studio

1. 用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. 点击 **Build > Generate Signed Bundle / APK**
4. 选择 **APK** > **release**
5. 配置签名密钥或创建新密钥
6. 点击 **Create** 开始构建

### 使用命令行

```bash
# 给脚本添加执行权限
chmod +x build.sh

# 运行构建脚本
./build.sh

# 或直接使用 gradlew
./gradlew assembleRelease
```

## 构建输出

Release APK 将生成在 `app/build/outputs/apk/release/` 目录。

### 生成的APK文件

根据配置，会生成以下多版本APK：

#### 按ABI架构分离

| APK后缀 | 说明 | 适用设备 |
|---------|------|----------|
| `armeabi-v7a` | ARM 32位 | 旧款Android设备 |
| `arm64-v8a` | ARM 64位 | 现代Android设备（推荐） |
| `x86` | Intel 32位 | 模拟器 |
| `x86_64` | Intel 64位 | 模拟器 |
| `universal` | 全架构 | 通用版本（体积最大） |

#### 按语言分离

| APK后缀 | 说明 |
|---------|------|
| `zh-rCN` | 简体中文 |
| `en` | 英文 |

### 完整APK命名示例

```
xd-mock-gps_2.0.0_zh-rCN_arm64-v8a.apk  (推荐)
xd-mock-gps_2.0.0_en_arm64-v8a.apk
xd-mock-gps_2.0.0_zh-rCN_armeabi-v7a.apk
xd-mock-gps_2.0.0_en_armeabi-v7a.apk
xd-mock-gps_2.0.0_zh-rCN_x86.apk
xd-mock-gps_2.0.0_en_x86_64.apk
xd-mock-gps_2.0.0_zh-rCN_universal.apk
xd-mock-gps_2.0.0_en_universal.apk
```

## 版本信息

| 属性 | 值 |
|------|-----|
| 版本名称 | 2.0.0 |
| 版本代码 | 2 |
| 最低SDK | 29 (Android 10) |
| 目标SDK | 32 (Android 12L) |

## 签名配置

Release构建需要签名密钥。在Android Studio中创建签名配置：

1. **Key store path**: 选择 `.jks` 或 `.keystore` 文件
2. **Key alias**: 密钥别名
3. **Key password**: 密钥密码
4. **Store password**: 密钥库密码

### V1/V2签名

建议同时启用 V1 和 V2 签名以获得最佳兼容性。

## 常见问题

### Q: 构建失败，提示缺少SDK

确保已安装 Android SDK Platform 32 和 Build Tools：

```bash
# 使用 sdkmanager 安装
sdkmanager "platforms;android-32" "build-tools;32.0.0"
```

### Q: Gradle同步失败

1. 检查网络连接
2. 清理并重新同步
   ```bash
   ./gradlew clean
   ./gradlew --refresh-dependencies
   ```

### Q: ProGuard/R8 导致运行时错误

Release构建启用了代码压缩。如遇问题，检查 `proguard-rules.pro` 文件。
