# MockGPS - Android GPS 位置模拟应用

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11+ (推荐JDK 17)
- Android SDK API 34

### 构建步骤

1. **打开项目**
   ```bash
   # 使用Android Studio打开项目根目录
   open -a "Android Studio" /path/to/MockGPS
   ```

2. **等待Gradle同步**
   - Android Studio会自动下载必要的依赖
   - 如果遇到下载问题，检查网络代理设置

3. **生成签名密钥**（可选）
   ```bash
   keytool -genkey -v -keystore mockgps.keystore \
     -alias mockgps -keyalg RSA -keysize 2048 -validity 10000
   ```

4. **构建Release APK**
   - 点击菜单: `Build > Generate Signed Bundle / APK`
   - 选择 `APK`
   - 选择签名密钥或创建新密钥
   - 选择 `release` 构建类型
   - 点击 `Finish`

### 命令行构建

```bash
# 设置环境变量
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk

# 构建Release版本
./gradlew assembleRelease

# 构建Debug版本
./gradlew assembleDebug
```

### APK输出位置

```
app/build/outputs/apk/release/
├── MockGPS_3.0.0_armeabi-v7a.apk   # ARM 32位
├── MockGPS_3.0.0_arm64-v8a.apk     # ARM 64位 (推荐)
├── MockGPS_3.0.0_x86.apk           # x86 32位
├── MockGPS_3.0.0_x86_64.apk        # x86 64位
└── MockGPS_3.0.0_universal.apk     # 通用版
```

## 功能特性

### 核心功能
- **单点位置模拟** - 输入经纬度，模拟固定GPS位置
- **摇杆控制** - 拖动摇杆实时移动位置
- **GPX轨迹导入** - 导入GPX文件自动播放轨迹
- **位置管理** - 保存、加载、删除常用位置
- **随机位置** - 一键生成随机GPS位置

### 技术特性
- **支持Android 10-14** (API 29-34)
- **Material Design 3** 界面设计
- **多ABI支持** - ARM/x86 架构
- **后台通知** - 显示运行状态

## 使用说明

### 首次使用
1. 安装APK并打开应用
2. 授予位置权限
3. 进入 **设置 → 开发者选项**
4. 启用 **选择模拟位置应用** 并选择 **MockGPS**

### 基本操作
1. 输入目标纬度和经度
2. 点击 **开始模拟**
3. 观察位置信息更新
4. 点击 **停止模拟** 结束

## 项目结构

```
MockGPS/
├── app/
│   ├── src/main/
│   │   ├── java/com/mockgps/app/
│   │   │   ├── MainActivity.java
│   │   │   └── NotificationHelper.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle/
    └── wrapper/
```

## 许可证

MIT License
