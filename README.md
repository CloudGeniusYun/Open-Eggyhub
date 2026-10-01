
<div align=center>
<img width="250" height="250" src=./app/src/main/ic_launcher-playstore.png alt="Open-Eggyhub"/>
</div>

<h1 align=center >欢迎来到 Open-Eggyhub</h1>

<h3 align=center >本Git库将不再维护</h3>

<div align=center>
<a href="./LICENSE">
    <img src="https://img.shields.io/github/license/cloudgeniusyun/open-eggyhub?logo=google%20docs&logoColor=ffffff" alt="license">
</a>

<a href="https://qm.qq.com/cgi-bin/qm/qr?k=8yBu5_KNSisz5zpwVJaGgjqwqPMqigEb&jump_from=webapi&authKey=Z4F65fkl1kY10IE5foN9BCGDclO9TR8zaDqoBBjWD5iHQAKYtDks55p4IPJl6uh/">
    <img src="https://img.shields.io/badge/%E7%BE%A4-%E8%9B%8B%E7%A0%81%E7%A0%94%E7%A9%B6%E9%99%A2-E83E8C.svg?logo=QQ&logoColor=ffffff" alt="蛋码研究院">
</a>

<a href="https://eggyhub.top">
    <img src="https://custom-icon-badges.demolab.com/badge/%E5%AE%98%E7%BD%91-Eggyhub-0096FF?logo=eggy&logoSource=feather" alt="官网">
</a>

<a href="https://eggyhub.top/app">
    <img src="https://custom-icon-badges.demolab.com/badge/download-Eggyhub--%E7%A7%BB%E5%8A%A8%E7%AB%AF%E5%BA%94%E7%94%A8-A9A9A9?logo=download&logoSource=feather&logoColor=white" alt="download">
</a>

<a href="https://developer.android.google.cn/about?hl=zh-cn">
    <img src="https://img.shields.io/badge/SDK--version--support-23%20--%2036-9370DB.svg?logo=android&logoColor=FFFFFF" alt="Android">
</a>

<a href="https://services.gradle.org/distributions/gradle-8.13-bin.zip">
    <img src="https://img.shields.io/badge/gradle--version-3.18-FF4500.svg?logo=gradle&logoColor=FFFFFF" alt="Gradle">
</a>

<a href="https://dev.java/">
    <img src="https://custom-icon-badges.demolab.com/badge/Recommended--Java--version-17.0.16-00B5D8?logo=java-color-white&logoSource=feather" alt="Java">
</a>

</div>

<div align=center>

<img width="100%" src="https://starify.komoridevs.icu/api/starify?owner=cloudgeniusyun&repo=open-eggyhub" alt="starify" />

<img src="https://api.star-history.com/svg?repos=cloudgeniusyun/open-eggyhub&type=Timeline" alt="Star Trend" width="800" />

</div>

# 简介

## 什么是Open-Eggyhub

Open-Eggyhub 是 Eggyhub 移动端应用（Android 端）的官方开源版本，基于官方开发版本整理后公开。

本仓库旨在为开发者提供学习、参考和二次开发的基础，代码以现状提供，不保证可用性和安全性。

当前应用版本：`v1.5.2.1`

## 许可证

本项目采用 BSD-3-Clause 许可证，详见 [LICENSE](./LICENSE)。

## 技术栈

| 项目 | 详情 |
| --- | --- |
| 逻辑语言 | Java |
| 页面语言 | xml |
| 构建工具 | [Gradle-8.13](https://services.gradle.org/distributions/gradle-8.13-bin.zip) |
| DSL类型 | Kotlin DSL |
| 最低 SDK | 23 |
| 目标 SDK | 36 |
| 开发环境 | Android Studio |
| UI | AndroidX、Material Components、ConstraintLayout、RecyclerView、CardView、SwipeRefreshLayout |
| 网络 | OkHttp 4.12.0、Retrofit 2.9.0、Gson 2.10.1 |
| 图片 | Glide 4.16.0、UCrop 2.2.8 |
| 动画 | Lottie 6.4.0、Tencent PAG 4.5.27 |
| 数据库 | Room 2.6.1 |
| 鉴权 | JWTDecode 2.0.2 |
| 测试 | JUnit、AndroidX Test、Espresso |
| 混淆 | release 开启 R8 / ProGuard |


## 视频播放器 dex

项目包含一个独立的 `video-dex` 模块，用于生成视频播放器所需的 dex 文件。

构建并复制到 assets：

```bash
./gradlew app:prepareVideoDex
```

> **注意：主 APK 构建时不会自动包含该文件，需要手动执行上述任务。**

## 构建要求

- Android Studio
- JDK 11
- Gradle 8.13
- Android SDK 36
- NDK / CMake（用于编译 `app/src/main/cpp/` 原生代码）

## 构建步骤

1. 克隆仓库：

```bash
git clone https://github.com/cloudgeniusyun/open-eggyhub.git
```

2. 在项目根目录运行
```bash
./gradle :app:assembleRelease
```
或打开`Android Studio`手动构建

## 免责声明

  
使用本代码所产生的任何风险由使用者自行承担。  
官方不再对本项目的安全性、可用性或合法性负责。

Copyright (c) 2026, 云云鬼才,蛋码研究院