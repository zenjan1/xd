# MockGPS - Android GPS 位置模拟应用

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

## 技术方案

### 位置模拟核心原理

```java
// 1. 添加测试位置提供者
locationManager.addTestProvider(
    LocationManager.GPS_PROVIDER,
    false, false, false, false, true, true, true,
    Criteria.POWER_HIGH, Criteria.ACCURACY_FINE
);

// 2. 启用提供者
locationManager.setTestProviderEnabled(
    LocationManager.GPS_PROVIDER, true);

// 3. 设置模拟位置
Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
mockLocation.setLatitude(lat);
mockLocation.setLongitude(lon);
locationManager.setTestProviderLocation(
    LocationManager.GPS_PROVIDER, mockLocation);
```

### 关键API
- `LocationManager.addTestProvider()` - 添加测试提供者
- `LocationManager.setTestProviderLocation()` - 设置模拟位置
- `LocationManager.setTestProviderEnabled()` - 启用提供者

## 项目结构

```
app/
├── src/main/
│   ├── java/com/mockgps/app/
│   │   ├── MainActivity.java          # 主界面
│   │   └── NotificationHelper.java    # 通知助手
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml     # 主界面布局
│   │   │   ├── dialog_save.xml       # 保存位置对话框
│   │   │   └── dialog_settings.xml    # 设置对话框
│   │   ├── drawable/
│   │   │   ├── joystick_base.xml      # 摇杆底座
│   │   │   └── joystick_knob.xml       # 摇杆旋钮
│   │   └── values/
│   │       ├── colors.xml             # 颜色
│   │       ├── strings.xml            # 字符串
│   │       └── themes.xml             # 主题
│   └── AndroidManifest.xml
└── build.gradle                       # 构建配置
```

## 构建说明

### 环境要求
- Android Studio Arctic Fox (2020.3.1) 或更高
- JDK 11+
- Android SDK API 34

### 快速构建

```bash
# 给脚本添加执行权限
chmod +x build.sh

# 运行构建
./build.sh
```

### 手动构建

```bash
# 设置环境变量
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk

# 构建Release APK
./gradlew assembleRelease
```

### APK输出

Release APK 将生成在 `app/build/outputs/apk/release/` 目录:

| 文件名 | 说明 |
|--------|------|
| `MockGPS_3.0.0_armeabi-v7a.apk` | ARM 32位 |
| `MockGPS_3.0.0_arm64-v8a.apk` | ARM 64位 (推荐) |
| `MockGPS_3.0.0_x86.apk` | x86 32位 |
| `MockGPS_3.0.0_x86_64.apk` | x86 64位 |
| `MockGPS_3.0.0_universal.apk` | 通用版 |

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

### 摇杆控制
1. 点击 **摇杆控制** 按钮
2. 拖动屏幕上的摇杆
3. 位置将沿摇杆方向移动

### GPX轨迹
1. 点击 **导入GPX** 按钮
2. 选择GPX文件
3. 点击 **开始模拟**
4. 应用将自动沿轨迹移动

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 3.0.0 | 2026-05-13 | 完全重构，添加摇杆控制和GPX导入 |
| 2.0.0 | 2026-05-13 | 添加轨迹模拟和多语言支持 |
| 1.0.0 | 2022-12-07 | 初始版本 |

## 许可证

MIT License
