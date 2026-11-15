# 莫忘哒 · Android 工程

一句话记行程：说一句 / 打一行 → 自动拆条、按时间排好 → 到点发本地通知提醒。

- 界面：Kotlin + Jetpack Compose，零第三方依赖
- 数据：全部存本机（SharedPreferences 里的 JSON），**不**同步系统提醒事项——这是与 iOS 版唯一的功能差异
- 提醒：AlarmManager 精确闹钟 + 通知渠道，**可以选提示音 / 振动三声 / 静音**（iOS 版做不到的，这里做得到）
- 语音：系统 SpeechRecognizer 中文识别，本机完成
- 深色：跟随系统

## 与 iOS 版的功能对照

| 功能 | iOS 版 | 安卓版 |
|---|---|---|
| 一句话/语音拆条 | ✓ | ✓ 同一套规则 |
| 写入系统提醒事项 | ✓ | ✗（存本机，按需求去掉） |
| 到点提醒 | 系统提醒事项 | App 自己的通知 |
| 提醒铃声/振动 | 系统控制，App 改不了 | **可选：提示音+振动三声 / 仅振动 / 静音** |
| 左滑（完成/删除在右侧） | ✓ | ✓ |
| 优先级整卡配色 | ✓ | ✓ |
| 深色跟随系统 | ✓ | ✓ |

## 编译

推到 GitHub 后 Actions 自动出已签名的 `app-release.apk`（ubuntu + JDK 17 + Gradle 8.7 + AGP 8.5.2）。
本地 Mac/Windows 也可以：装 JDK 17 和 Android SDK 后 `gradle assembleRelease`。

## 鸿蒙兼容

- 鸿蒙 4.x 及更早：直接安装 apk
- 鸿蒙 5 / 6（NEXT）：**装不了 APK**，需要另做 ArkTS 原生版
