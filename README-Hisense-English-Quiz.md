# 海信电视英语闯关项目

目标设备：ADB 查询结果为 Android 5.1.1 / API 22 / armeabi-v7a 的 VIDAA 电视。

## 当前初版功能
- 英语单词中文释义四选一；每轮 5 题，答错可重试。
- 遥控器焦点与确认键操作。
- 首次启动后计时 15 分钟，后台服务尝试弹出答题 Activity。
- 收到 BOOT_COMPLETED 时尝试启动计时服务。
- GitHub Actions 自动构建 debug APK。

## 重要限制
此项目尚未在真实电视编译、安装或测试。ADB 返回 Android 属性并不证明 VIDAA 固件具有完整 Android 应用运行环境。普通 APK 也无法保证屏蔽 Home 键、系统强制停止或彻底防止切换应用。
设备所有者和锁定任务模式尚未验证；请勿执行 dpm set-device-owner 或恢复出厂设置。
