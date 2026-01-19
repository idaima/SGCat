# 三国猫 (SGCat)

一款基于 Android 的网页游戏客户端应用，采用现代 Android 开发技术栈构建。

## 📱 应用概述

三国猫是一个 Android 游戏应用，通过内嵌 WebView 加载网页游戏内容，并提供便捷的悬浮窗口功能用于游戏管理（GM）操作。

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **最低 SDK**: Android 12 (API 31)
- **目标 SDK**: Android 15 (API 36)
- **构建工具**: Gradle Kotlin DSL

## 📦 核心依赖

- AndroidX Core KTX
- Jetpack Compose (Material 3)
- AndroidX Lifecycle
- AndroidX Activity Compose
- AndroidX DataStore Preferences

## 🏗️ 项目结构

```
SGCat/
├── app/
│   └── src/main/
│       ├── java/game/idaima/sgcat/
│       │   ├── MainActivity.kt      # 主 Activity
│       │   ├── ui/
│       │   │   ├── FloatingWindow.kt # 悬浮窗组件
│       │   │   ├── WebViewScreen.kt  # WebView 封装
│       │   │   └── theme/            # Material 主题
│       │   ├── data/                 # 数据层
│       │   └── util/                 # 工具类
│       ├── res/                      # 资源文件
│       └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## ✨ 主要功能

- **全屏游戏体验**: 隐藏系统导航栏，提供沉浸式游戏界面
- **悬浮窗口**: 可拖动、可调整大小的悬浮窗，用于快速访问 GM 工具
- **状态持久化**: 使用 DataStore 保存悬浮窗位置和大小
- **Edge-to-Edge**: 支持全面屏显示

## 🚀 构建与运行

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 21
- Android SDK 36

### 构建步骤

```bash
# 克隆项目
git clone <repository-url>
cd SGCat

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

### 安装到设备

```bash
./gradlew installDebug
```

## 📝 开发说明

### Release 构建优化

项目已配置以下优化选项：
- 代码混淆 (ProGuard/R8)
- 资源压缩
- 移除未使用的资源

### 权限

应用需要以下权限：
- `INTERNET` - 加载网页游戏内容

## 📄 License

MIT License

## ⚠️ 免责声明

1. 本项目仅供**学习交流**使用，不得用于任何商业用途。
2. 本应用所加载的游戏内容版权归原作者所有，与本应用无关。
3. 使用本应用产生的任何问题和风险由用户自行承担，开发者不承担任何责任。
4. 请遵守当地法律法规，合理使用本应用。
5. 如有侵权，请联系开发者删除。

---

*三国猫 - 让游戏更有趣！* 🐱
