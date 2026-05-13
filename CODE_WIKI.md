# XD Mock GPS - Code Wiki

## 1. 项目概述

### 1.1 项目简介

XD Mock GPS是一个功能丰富的Android GPS位置模拟应用程序，基于Android Studio开发。该应用允许用户在Android设备上模拟GPS位置信息，支持单点定位、轨迹模拟和位置管理等功能。

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| 单点位置模拟 | 输入经纬度，模拟固定GPS位置 |
| 轨迹模拟 | 支持圆形、方形、直线等多种轨迹模式 |
| 位置管理 | 保存、加载、删除常用位置 |
| 多语言支持 | 中文、英文 |
| 多架构支持 | ARM、x86等主流CPU架构 |

### 1.3 项目信息

| 属性 | 值 |
|------|-----|
| 应用包名 | com.example.xd |
| 当前版本 | 2.0.0 |
| 版本代码 | 2 |
| 最低SDK版本 | 29 (Android 10) |
| 目标SDK版本 | 32 (Android 12L) |
| 构建工具版本 | Android Gradle Plugin 7.3.1 |
| Gradle版本 | 8.14.4 |
| Java版本 | 1.8 |

---

## 2. 项目架构

### 2.1 整体目录结构

```
/workspace/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/xd/
│   │   │   ├── MainActivity.java          # 主活动类
│   │   │   ├── MockLocation.java          # 位置数据模型
│   │   │   ├── LocationStorage.java       # 位置持久化存储
│   │   │   └── TrajectorySimulator.java   # 轨迹模拟器
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # 主界面布局
│   │   │   │   ├── dialog_save_location.xml  # 保存位置对话框
│   │   │   │   └── dialog_trajectory.xml  # 轨迹设置对话框
│   │   │   ├── values/
│   │   │   │   ├── strings.xml            # 中文字符串
│   │   │   │   ├── colors.xml             # 颜色资源
│   │   │   │   └── themes.xml             # 主题样式
│   │   │   └── values-en/
│   │   │       └── strings.xml            # 英文字符串
│   │   └── AndroidManifest.xml           # 应用清单
│   └── build.gradle                       # 应用级构建配置
├── build.gradle                           # 项目级构建配置
├── settings.gradle                        # Gradle设置
├── gradle.properties                      # Gradle属性
├── build.sh                              # 构建脚本
├── BUILD_INSTRUCTIONS.md                  # 构建说明
└── CODE_WIKI.md                          # 代码文档
```

### 2.2 架构模式

本项目采用分层架构设计：

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  MainActivity.java                                    │ │
│  │  - UI组件初始化与管理                                  │ │
│  │  - 用户交互处理                                        │ │
│  │  - 对话框展示                                          │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                         │
│  ┌────────────────────┐  ┌────────────────────────────┐   │
│  │ LocationStorage    │  │ TrajectorySimulator        │   │
│  │ - SharedPreferences│  │ - 轨迹点管理                │   │
│  │ - Gson序列化       │  │ - 路径生成算法              │   │
│  │ - 位置CRUD操作     │  │ - 插值计算                  │   │
│  └────────────────────┘  └────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                           │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  MockLocation.java                                    │ │
│  │  - 位置数据模型                                        │ │
│  │  - Serializable序列化支持                             │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 核心模块详解

### 3.1 MockLocation - 位置数据模型

**文件路径**: `/workspace/app/src/main/java/com/example/xd/MockLocation.java`

```java
public class MockLocation implements Serializable {
    private long id;           // 唯一标识
    private String name;      // 位置名称
    private double latitude;   // 纬度
    private double longitude;  // 经度
    private double altitude;   // 高度(米)
    private float bearing;     // 方向角度
    private float speed;       // 速度(米/秒)
    private float accuracy;    // 精度(米)
    private long createTime;   // 创建时间戳
    private String description;// 描述
}
```

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getFormattedLocation()` | 返回格式化的经纬度字符串 |
| `toString()` | 返回位置名称或坐标 |

### 3.2 LocationStorage - 位置存储管理

**文件路径**: `/workspace/app/src/main/java/com/example/xd/LocationStorage.java`

**职责**: 管理位置数据的持久化存储

**核心方法**:

| 方法 | 说明 |
|------|------|
| `saveLocation(MockLocation)` | 保存新位置 |
| `saveLocationWithName(String, ...)` | 带名称保存位置 |
| `getAllLocations()` | 获取所有保存的位置 |
| `deleteLocation(long id)` | 删除指定位置 |
| `updateLocation(MockLocation)` | 更新位置信息 |
| `saveLastUsedLocation(...)` | 保存最后使用的位置 |
| `getLastUsedLocation()` | 获取最后使用的位置 |
| `setTrajectoryEnabled(boolean)` | 设置轨迹模式开关 |
| `setTrajectoryInterval(long)` | 设置轨迹更新间隔 |
| `setTrajectorySpeed(float)` | 设置轨迹移动速度 |

**存储方式**: SharedPreferences + Gson序列化

### 3.3 TrajectorySimulator - 轨迹模拟器

**文件路径**: `/workspace/app/src/main/java/com/example/xd/TrajectorySimulator.java`

**核心内部类**: `TrajectoryPoint`

```java
public static class TrajectoryPoint {
    public double latitude;
    public double longitude;
    public double altitude;
    public float bearing;
    public float speed;
    public float accuracy;
}
```

**轨迹生成方法**:

| 方法 | 说明 |
|------|------|
| `generateCirclePath(centerLat, centerLon, radius, numPoints)` | 生成圆形轨迹 |
| `generateRectanglePath(centerLat, centerLon, width, height, pointsPerSide)` | 生成方形轨迹 |
| `generateLinePath(startLat, startLon, endLat, endLon, numPoints)` | 生成直线轨迹 |

**控制方法**:

| 方法 | 说明 |
|------|------|
| `start()` | 开始轨迹模拟 |
| `stop()` | 停止轨迹模拟 |
| `reset()` | 重置到起点 |
| `getNextPoint()` | 获取下一个轨迹点 |
| `hasMorePoints()` | 检查是否还有更多点 |
| `interpolate(p1, p2, ratio)` | 两点间插值计算 |

**距离计算**:

```java
// 计算两点间地球表面距离（米）
float distance = calculateDistance(point1, point2);

// 计算总轨迹距离
double totalDistance = calculateTotalDistance();
```

### 3.4 MainActivity - 主活动类

**文件路径**: `/workspace/app/src/main/java/com/example/xd/MainActivity.java`

#### 核心成员变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `locationManager` | `LocationManager` | 位置管理器 |
| `locationStorage` | `LocationStorage` | 位置存储实例 |
| `trajectorySimulator` | `TrajectorySimulator` | 轨迹模拟器实例 |
| `mockProviders` | `List` | 模拟位置提供者列表 |
| `bRun` | `boolean` | 模拟运行状态 |
| `trajectoryMode` | `boolean` | 轨迹模式开关 |
| `trajectoryInterval` | `long` | 轨迹更新间隔(ms) |
| `trajectorySpeed` | `float` | 轨迹移动速度(m/s) |

#### 核心方法

**生命周期方法**:

| 方法 | 说明 |
|------|------|
| `onCreate()` | 初始化组件和线程 |
| `onPostResume()` | 检查模拟位置状态 |
| `onPause()` | 移除位置监听 |
| `onDestroy()` | 清理资源和停止模拟 |

**位置模拟核心**:

| 方法 | 说明 |
|------|------|
| `initService()` | 初始化位置服务 |
| `getUseMockPosition()` | 检查并配置模拟提供者 |
| `stopMockLocation()` | 停止位置模拟 |

**对话框方法**:

| 方法 | 说明 |
|------|------|
| `showSaveLocationDialog()` | 显示保存位置对话框 |
| `showManageLocationsDialog()` | 显示管理位置对话框 |
| `showTrajectoryDialog()` | 显示轨迹设置对话框 |

**后台线程**: `RunnableMockLocation`

每`trajectoryInterval`毫秒更新一次模拟位置数据，支持轨迹模式自动遍历轨迹点。

---

## 4. UI资源文件

### 4.1 主界面布局

**`activity_main.xml`** - 采用ConstraintLayout布局

主要区域:
1. 顶部状态栏 - 显示模拟位置开关状态
2. 按钮区 - 开始/停止/保存/管理/轨迹按钮
3. 输入区 - 经纬度输入框
4. 状态显示区 - 轨迹模式和点数
5. 信息卡片 - 位置信息展示(时间、纬经度、高度等)

### 4.2 对话框布局

**`dialog_save_location.xml`**
- 位置名称输入框
- 当前坐标显示

**`dialog_trajectory.xml`**
- 轨迹类型选择器
- 中心点坐标输入
- 半径输入
- 更新间隔滑块
- 移动速度滑块

### 4.3 字符串资源

| 文件 | 语言 | 说明 |
|------|------|------|
| `values/strings.xml` | 中文 | 默认语言 |
| `values-en/strings.xml` | 英文 | 英文翻译 |

---

## 5. 权限配置

### 5.1 AndroidManifest.xml 权限

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION"
    tools:ignore="MockLocation,ProtectedPermissions"/>
```

### 5.2 权限说明

| 权限 | 用途 |
|------|------|
| `ACCESS_FINE_LOCATION` | 获取精确GPS位置 |
| `ACCESS_COARSE_LOCATION` | 获取网络辅助定位 |
| `ACCESS_BACKGROUND_LOCATION` | 后台位置访问 |
| `ACCESS_MOCK_LOCATION` | 允许模拟位置（需在开发者选项中开启） |

---

## 6. 构建配置

### 6.1 build.gradle 配置

**多版本构建**:

```groovy
splits {
    abi {
        enable true
        include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        universalApk true
    }
    language {
        enable true
        include "en", "zh-rCN"
    }
}
```

**依赖库**:

| 库 | 版本 | 用途 |
|----|------|------|
| `androidx.appcompat:appcompat` | 1.4.1 | 兼容性支持 |
| `com.google.android.material:material` | 1.5.0 | Material Design |
| `androidx.constraintlayout:constraintlayout` | 2.1.3 | 约束布局 |
| `com.google.code.gson:gson` | 2.8.9 | JSON序列化 |

### 6.2 ProGuard规则

已配置Gson序列化类的混淆规则，确保位置数据正确序列化和反序列化。

---

## 7. 功能使用说明

### 7.1 单点位置模拟

1. 输入目标纬度和经度
2. 点击"开始模拟"
3. 观察右侧位置信息更新
4. 点击"停止模拟"结束

### 7.2 保存和管理位置

1. 调整到想要保存的位置
2. 点击"保存位置"
3. 输入位置名称
4. 点击"管理位置"可查看/加载/删除已保存位置

### 7.3 轨迹模拟

1. 点击"轨迹模拟"
2. 选择轨迹类型（圆形/方形/直线）
3. 设置中心点坐标和半径
4. 调整更新间隔和移动速度
5. 点击"开始轨迹"
6. 观察位置沿轨迹移动

---

## 8. 依赖关系图

```
┌─────────────────────────────────────────────┐
│              应用模块 (app)                   │
├─────────────────────────────────────────────┤
│  com.google.code.gson:gson:2.8.9           │
│  └── (用于位置数据序列化)                     │
├─────────────────────────────────────────────┤
│  com.google.android.material:material:1.5.0│
│  ├── Material Design 组件                   │
│  └── TextInputLayout                        │
├─────────────────────────────────────────────┤
│  androidx.constraintlayout:2.1.3           │
│  └── ConstraintLayout 布局                 │
├─────────────────────────────────────────────┤
│  androidx.appcompat:appcompat:1.4.1        │
│  ├── Activity 兼容性                        │
│  └── DialogFragment 支持                    │
└─────────────────────────────────────────────┘
```

---

## 9. 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.0 | 2022-12-07 | 初始版本，基本位置模拟功能 |
| 2.0 | 2026-05-13 | 新增轨迹模拟、位置管理、多语言、多ABI支持 |

---

## 10. 开发者注意事项

### 10.1 Android 10+兼容性

- 最低SDK设置为29，确保在Android 10设备上运行
- 使用`addTestProvider` API进行位置模拟
- 需要用户在开发者选项中选择"模拟位置应用"

### 10.2 模拟位置开启步骤

1. 进入**设置 → 开发者选项**
2. 启用**USB调试**（如需要）
3. 找到**选择模拟位置信息应用**
4. 选择**xd**应用

### 10.3 性能优化建议

- 轨迹更新间隔不宜过短（建议≥1000ms）
- 轨迹点数根据需求合理设置
- 不使用时及时停止模拟节省电量

---

*文档生成时间: 2026-05-13*
*项目版本: 2.0.0*
