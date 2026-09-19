# HFCL 合并与改造说明（CHANGES.md）

本文件记录将 **HelloFoldCraftLauncher (HFCL)** 与上游 **FoldCraftLauncher (FCL)** 合并、迁移 SDL3、修复已知 bug、并全量品牌重命名为 HFCL 的全过程。

---

## 1. 仓库基线

| 项 | Commit / 说明 |
|---|---|
| HFCL fork 合并前 HEAD | `20a19874`（HaQiMi-Din/HelloFoldCraftLauncher，fork 自 FCL-Team/FoldCraftLauncher） |
| 合并的上游基线 | `1db59b34`（FCL-Team/FoldCraftLauncher `main` 最新） |
| 合并提交 | `d5eae8f6` |
| 本次改造集成提交 | `808c21a5` |

HFCL fork 相对上游分叉点领先 134 个提交（主要是平板 UI 迭代：主题色 `#5C6BC0`、`values-w600dp`/`values-w720dp` dimens、应用图标、`activity_main.xml` 布局调整），落后上游 321 个提交。

## 2. 合并策略与冲突解决

- **方向**：以 HFCL fork 为工作树，`git merge upstream/main` 把上游最新代码并入。
- **冲突**：本次自动合并**无文本冲突**（HFCL 的定制改动集中在图标/dimens/布局资源，与上游改动区域不重叠）。合并后已校验全树无 `<<<<<<<`/`>>>>>>>` 残留标记。
- **保留的 HFCL 定制**（已确认在合并中存活）：主题色 `default_theme_color=#5C6BC0`、`values-w600dp/dimens.xml`、`values-w720dp/dimens.xml`、应用图标、`strings.xml` 品牌名。

## 3. SDL3 后端迁移

### 背景
本项目基于 PojavLauncher：游戏窗口/输入由 LWJGL 驱动。仓库自带 LWJGL `3.3.3` 与 `3.4.1` 两套运行时，其中 **3.4.1 已内置 `lwjgl-sdl.jar`（SDL3 绑定）**。改造前：窗口后端为 Pojav 自实现的 GLFW 桥（native 见 `input_bridge_v3.c`、`egl_bridge.c`，Java 桥 `org.lwjgl.glfw.CallbackBridge`），`lwjgl-sdl.jar` 虽打包但未启用。

### 已落地改动（构建/打包层）
- `LWJGL/3.4.1/build.gradle.kts`：注释明确 `lwjgl-sdl` 为活动原生窗口/输入后端（保持在 `excludedModules` 单独拷贝，不参与 merged jar）；`lwjgl-glfw.jar` 仍参与合并（Minecraft 仍引用 `org.lwjgl.glfw.*`，不能删）。
- `FCL/build.gradle.kts`（natives 打包段）：新增对 `natives/<abi>/libSDL3.so` 的存在性自检日志（存在则 log，缺失则告警提示补齐 aar）。
- `FCLauncher/src/main/jni/jvm_hooks/lwjgl_dlopen_hook.c`：在 LWJGL `dlopen` 钩子里增加对 `SDL3` 的识别日志（additive，不破坏原 GLFW 加载行为）。
- `FCLauncher/src/main/jni/CMakeLists.txt`：补充 SDL3 迁移说明注释（`libSDL3.so` 来自 LWJGL 预编译 natives，不在此处从源码编译）。

### SDL3 vs 原 GLFW 关键差异
- 原生库名：GLFW 后端在本树无独立 `libglfw.so`（窗口由 `pojavexec.so` 自实现 `org.lwjgl.glfw.*` JNI）；SDL3 后端为 LWJGL natives 提供的 **`libSDL3.so`**，运行时按名 `"SDL3"` dlopen，可用 `-Dorg.lwjgl.sdl.libname=` 覆盖。
- 窗口模型：GLFW 用 `GLFWwindow*` 句柄 + `glfwSet*Callback` 回调表；SDL3 用 `SDL_Window*` + `SDL_InitSubSystem(SDL_INIT_VIDEO)` + `SDL_PollEvent` 事件队列。
- 输入：GLFW 回调直投 `(window,key,scancode,action,mods)`；SDL3 为 `SDL_KEYDOWN/UP`、`SDL_MOUSEBUTTON*`、`SDL_MOUSEMOTION`、`SDL_MOUSEWHEEL` 等事件结构。
- 上下文：GLFW `glfwMakeContextCurrent/glfwSwapBuffers` → SDL3 `SDL_GL_MakeCurrent/SDL_GL_SwapWindow`。

### ⚠️ 未完成、需 NDK/真机环境
当前 `FCL/libs/lwjgl-3.4.1-natives-release.aar` **不含 `libSDL3.so`**（4 个 ABI 已逐一核对）。要真正运行 SDL3 后端还需：
1. 提供/替换含 `libSDL3.so`（arm64-v8a / armeabi-v7a / x86 / x86_64）的 natives aar。
2. 重写 native 桥：`input_bridge_v3.c` 全部 `Java_org_lwjgl_glfw_GLFW_nglfwSet*` 入口、`environ/environ.h` 的 `GLFW_invoke_*` 回调表与 `GLFWInputEvent`、`egl_bridge.c` 的 `GLFW_CLIENT_API/OPENGL_API/NO_API` 窗口提示常量 → 映射到 SDL3 `SDL_Window*`/`SDL_GL_*`/事件泵。
3. Java 侧 `CallbackBridge.java`、`LwjglGlfwKeycode.java`、`FCLBridge.java` native 签名与按键码表配 `org.lwjgl.sdl`。
4. 启动参数补 `-Dorg.lwjgl.sdl.libname=<natives>/<abi>/libSDL3.so`。

> 本次环境无 Android SDK/NDK，以上 native 重写未能编译验证，已逐项记录。

## 4. 强制横屏（用户硬性要求）

- `AndroidManifest.xml` 全部 9 个 activity 的 `screenOrientation` 统一为 **`sensorLandscape`**（允许反向横屏、禁止竖屏）。修复了 3 处违规：`ShellActivity`（原 `sensorPortrait`）、`JVMCrashActivity`（原 `sensor`）、`CrashReportActivity`（原 `sensor`）。
- 全树 grep 无任何运行时 `setRequestedOrientation(...)` 调用，无按传感器切换方向的逻辑。
- 全部 activity 的 `configChanges` 已含 `orientation|screenSize|screenLayout|keyboardHidden`（额外含 `smallestScreenSize|keyboard|navigation`），旋转/尺寸变化不重建 Activity。
- `res/` 下无 `layout-port`/`values-port` 等竖屏专用资源目录。

## 5. DPI / 全屏显示问题（用户重点 bug）

### 根因
全屏沉浸原用**已废弃**的裸 `decorView.setSystemUiVisibility(SYSTEM_UI_FLAG_*)` + `FLAG_LAYOUT_IN_SCREEN`。在 Android 11/12+ 强制 edge-to-edge 行为下，这套旧 flag 不能稳定隐藏手势导航/状态栏，且 `LAYOUT_*` flag 与新 insets 模型不一致，导致不同 DPI / 有无三键导航 / 有无挖孔时出现黑边、内容被系统栏占位、UI 错位。

### 修复（集中在 `ThemeEngine.applyFullscreen`，由 `FCLActivity` 基类在 onCreate/onWindowFocusChanged/onPostResume 统一调用）
1. `WindowCompat.setDecorFitsSystemWindows(window, false)` 取代旧 LAYOUT flag，内容稳定边到边。
2. `WindowInsetsControllerCompat`：`setSystemBarsBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)` + `hide(systemBars())` 取代旧 `IMMERSIVE_STICKY|HIDE_NAVIGATION|FULLSCREEN`，适配 R/S+。
3. 刘海/挖孔：API≥28 保持 `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`（横屏内容延伸到挖孔区）；仅当用户设置勾选"避让刘海"时回退 `NEVER`（保留用户偏好）。

### 审查通过、无需改
- 游戏画面 `TextureView` 为 `match_parent`，边到边后自动铺满；`onSurfaceTextureAvailable` 用实测宽高 × scaleFactor 送 native，未误用竖屏宽高。
- `DisplayUtil.getDisplayMetrics` 用 `getRealMetrics`（物理全像素），横屏全屏下与 SHORT_EDGES 一致；分屏时已 `isInMultiWindowMode` 分支退回。
- `fitsSystemWindows="true"` 仅局部用于 `activity_shell.xml` 根布局；layout 中硬编码 `px` 仅 9 处且全为 `1px` 分隔线。

## 6. 其他安全静态 bug 修复

| 文件 | 修复 |
|---|---|
| `util/AndroidUtils.java` | cursor / stream / media retriever 资源泄漏修复 |
| `fcllibrary/util/ConvertUtils.java` | Bitmap decode 流泄漏修复 |

> 上游 FCL 仓库有 64 个 open 的 `bug/错误` issue，绝大多数是特定 Mod / 特定 GPU / 特定机型的运行时崩溃（如某 Mod 闪退、天玑 8000 进不去游戏等），需日志与真机复现，不在静态可修范围；本次仅修了上述通用、低风险项。

## 7. 品牌重命名 FCL → HFCL 范围

| 维度 | 改动 |
|---|---|
| 主包名 | `com.tungsten.fcl` → `com.tungsten.hfcl`（目录已 `git mv`） |
| 子库包名 | `com.tungsten.fclcore`→`hfclcore`、`com.tungsten.fclauncher`→`hfclauncher`、`com.tungsten.fcllibrary`→`hfcllibrary` |
| applicationId / namespace | `com.tungsten.fcl` → `com.tungsten.hfcl`（debug 后缀 `.debug` 不变） |
| Application 类 | `FCLApplication` → `HFCLApplication`（含 Manifest 与全部引用） |
| native JNI 函数 | `Java_com_tungsten_fclauncher_bridge_*` → `Java_com_tungsten_hfclauncher_bridge_*`，与 Java native 方法一一对应 |
| native 库 | `libfcl.so` → `libhfcl.so`（CMake target 与 `System.loadLibrary` 同步） |
| 应用显示名 | `app_name` = `HelloFoldCraftLauncher`；"Fold Craft Launcher" → "Hello Fold Craft Launcher"（splash/about/crash 提示） |
| 主题 | `Theme.FoldCraftLauncher` → `Theme.HelloFoldCraftLauncher` |
| rootProject.name | → `Hello Fold Craft Launcher` |
| 许可证 | GPL-3.0 原文及原作者保留条款**未改动**；about 页保留对 FCL-Team / HMCLCore / PojavLauncher / Boat 的署名 |

### 有意未改（避免破坏构建）
- Gradle 模块目录名 `FCL/`、`FCLCore/`、`FCLauncher/` 与 `include(":FCL")` 等保持原样（目录改名会牵动大量相对路径，且无编译环境无法验证；纯内部命名，不影响 APK 功能）。
- 内部类符号如 `FCLBridge`、`FCLGameRepository`、`FCLActivity` 等保留原名（重命名需编译验证，留作后续）。

## 8. 已知未解决 / 构建注意事项

- **无 Android SDK/NDK，未做 gradle/ndk 实际编译**；所有改动为源码/构建脚本层面的静态修改，已用 grep 自检：无旧包名残留、无 JNI 签名不匹配、CMake target 与 loadLibrary 一致。
- SDL3 native 桥重写（见 §3 ⚠️）需在 NDK 环境完成。
- 336 处 `textSize="..sp"` 未抽 dimens（多机型一致性可后续分批处理）。
- `JVMActivity` 的 `OnGlobalLayoutListener` 随 decorView 回收，建议后续持有引用在 `onDestroy` 移除。
- FCLDialog 同样走沉浸式 hide system bars，需真机确认弹窗内输入/返回不受影响。
- 分屏/小窗下 notch 扣除逻辑需真机验证。

## 9. HMCL 风格化改造（第二轮迭代）

目标：在不回退横屏锁定/沉浸式全屏、不改包名 `com.tungsten.hfcl*`、不动游戏逻辑与 native 的前提下，把启动器 UI 视觉/交互尽量贴近桌面版 HMCL（Hello Minecraft Launcher，huangyuhang，JavaFX + Material You 主题）。

### 参考的 HMCL 真实 UI 元素
- 直接读 `HMCL-dev/HMCL` main 分支 `HMCL/src/main/resources/assets/css/blue.css` 取得 Material You 色板；对照 HMCL 经典主界面截图确认布局结构。
- 经典 HMCL 主界面特征：MC 世界壁纸背景 + 左侧竖向导航栏（导航项 = 方块图标 + 加粗标题 + 灰色副标题 + 右侧可选齿轮/列表小图标），导航栏压暗保证白字可读；右下角大圆角蓝色「启动游戏」主按钮；正文区浅底卡片。

### 配色方案对照（落 values/colors.xml，新增 `hmcl_*` 系列）
| HMCL 角色 | 色值 | 落位 |
|---|---|---|
| primary | `#4352A5` | themes.xml `colorPrimary`、启动胶囊背景 |
| primary-container | `#5C6BC0` | `colorPrimaryDark`/PrimaryVariant、选中导航项半透高亮（`#405C6BC0`）；与既有 `default_theme_color` 同源 |
| on-primary | `#FFFFFF` | 胶囊/按钮白字 |
| surface | `#FBF8FF` | `android:colorBackground` |
| surface-container | `#EFEDF5` | 内容卡片底（`bg_container_white*`） |
| on-surface / variant | `#1B1B21` / `#454651` | 文字（卡片正文仍走运行时 ThemeEngine.autoTint） |
| outline / error | `#767683` / `#BA1A1A` | 描边/错误预留 |

### 改动的文件
- `values/colors.xml`：新增 14 个 `hmcl_*` 色值，原色全保留。
- `values/themes.xml`：主色/背景/错误色/状态栏色按上表更新；NoActionBar、横屏、沉浸式原样保留。
- `drawable/bg_left_menu_scrim.xml`（新增）：左侧栏横向渐变压暗 `#CC000000 → #59000000`。
- `drawable/bg_left_menu_item.xml`：圆角 8→14dp，选中态改半透 primary-container 高亮，涟漪改白。
- `drawable/bg_start_capsule.xml`（新增）：`#4352A5` 实心 + 28dp 大圆角胶囊。
- `drawable/bg_container_white.xml` / `bg_container_white_clickable.xml`：卡片底白→`#EFEDF5`，圆角 5→16dp（覆盖设置/下载/关于等所有页卡片）。
- `layout/activity_main.xml`：`left_menu` 加 scrim 背景、内容区左右 8dp 留白；`start` 启动按钮背景换 `bg_start_capsule`。

### 因 Android 平台限制只能近似（未 1:1 复刻）
- HMCL 导航项的灰色副标题行：HFCL 左侧 item 结构只有「图标+标题」，为不动 `findViewById` 结构未强行新增副标题控件。
- HMCL 圆角方块图标底是 JavaFX CSS 实现；Android 侧 `FCLMenuView` 矢量图标直接着色，未额外套方块背景。
- 设置页是原生 `FCLSwitch/FCLSpinner/FCLTextView`，无法搬 JavaFX/Metro 控件，仅统一浅底卡片、圆角与蓝色选中态。
- HMCL 桌面内容区不显示壁纸；Android 端内容仍为浅卡片压在壁纸之上，仅左侧栏做了压暗。

### 需真机验证的视觉项
- 左侧栏渐变 scrim 与不同 MC 壁纸叠加后白字/灰副标题可读性。
- 14dp 圆角导航项 + 8dp 留白在 140dp 栏宽、横屏 w600dp/w720dp 下是否过挤（必要时微调 `left_menu_width`）。
- 启动胶囊 28dp 圆角在 `start_button_width=200dp` 下的比例与右下角位置。
- 卡片改 `#EFEDF5` 后浅色壁纸缝隙处的对比。
- 状态栏 `#CC000000` 在沉浸式全屏下是否被窗口隐藏（预期不影响）。

静态校验：改动的 20 个 XML 全部 well-formed；9 个 `hmcl_*` 色全部有定义、无悬空引用；两个新 drawable 各被引用 1 次；`left_menu/start/text_start/go_setting/version_name` 等 `findViewById` id 全部保留，未增删改名。

## 10. GitHub Actions 云编译

按用户要求改用 GitHub Actions 云端编译（未在本地装 SDK/NDK）。

### workflow
- 复用仓库既有的 `.github/workflows/build.yml`（FCL 官方同款）：`ubuntu-latest` + `actions/setup-java@v5` JDK 17 Temurin + Gradle 缓存；owner 为 HaQiMi-Din 时自动执行 `./gradlew assemblefordebug -Darch=<arch>`，matrix = all/arm/arm64/x86/x86_64，产物经 `actions/upload-artifact` 上传。compileSdk 35 / NDK 27.0.12077973 / CMake 由 AGP 与仓库配置自动拉取，无需手写 SDK 安装步骤。
- 触发：push 到 main 即自动跑（workflow 文件本身在 paths-ignore，只改 .md 不会再触发）。

### 结果
- Run：https://github.com/HaQiMi-Din/HelloFoldCraftLauncher/actions/runs/35430704314
- 状态：**success（5/5 ABI 全部通过）**，含 native CMake 编译。
- 产物（Actions Artifacts，约保留期内可下载）：
  - `app-arm` 165.0 MB、`app-arm64` 173.6 MB、`app-x86` 159.1 MB、`app-x86_64` 178.0 MB、`app-all` 328.3 MB（全 ABI）。

### 实际修复的编译错误（静态修改的遗漏在云端暴露）
1. **`FCL/.../hfcl/util/AndroidUtils.java:120` — unreported exception IOException**：此前"资源泄漏修复"在 finally 中直接调用 `MediaMetadataRetriever.release()`，而该方法抛受检 IOException。修复：finally 内再包一层 try-catch（IOException 已 import）。修复后一次通过。
- 其余（包名重命名、JNI `Java_com_tungsten_hfclauncher_*`、`loadLibrary("hfcl")`、Manifest `.HFCLApplication`、主题/资源引用）经云端实际编译验证均无遗漏、无悬空 R 引用、native 链接一致。

### 备注
- token 含 `repo` + `workflow` 权限，可正常推送 workflow 与触发 Actions，无权限受阻。
- release 签名所需 secrets（FCL_KEYSTORE_PASSWORD 等）未配置，故只出 debug APK；release 流程在非 FCL-Team 仓库本就被 workflow 跳过。

## 11. 首启字符串残留 / 内容裁切 / 微软登录 client_id 修复（第三轮迭代）

### 11.1 首启流程残留 FCL 字符串
- 权限警告弹窗正文（`splash_agreement`）：各语言中独立的 "FCL"（"FCL需要储存权限…FCL在GitHub…"、"FCL requires storage…FCL is fully open-source…"、俄/越/繁中变体）全部改为 "HFCL"。
- 无存储权限提示（`splash_permission_msg`）："请授予 Fold Craft Launcher 权限" → "请授予 Hello Fold Craft Launcher 权限"（含英/德/葡/俄/乌/越/简中/繁中）。
- 欢迎/EULA 标题（`splash_title`）："欢迎使用 Fold Craft Launcher" → "欢迎使用 HelloFoldCraftLauncher"。
- 通知权限说明（`notification_permission`）："FCL …" → "HFCL …"。
- EULA 正文 `FCL/src/main/assets/eula.txt`：标题改 HelloFoldCraftLauncher；"The software is developed by FCL-Team." → "The software is forked from FoldCraftLauncher, originally developed by FCL-Team, and rebranded as HFCL by HaQiMi-Din."；"FCL requires Android 8.0" → "HFCL requires Android 8.0"。
- 保留：`about_developer=FCL-Team` 署名、about_desc 中对 FCL-Team/HMCLCore/PojavLauncher/Boat 的 GPL 署名。

### 11.2 右侧内容区底部被裁切
- 根因：右下角"启动游戏"胶囊按钮（`@id/start`）悬浮在内容容器 `@id/ui_layout` 之上，而沉浸式 `setDecorFitsSystemWindows(false)` 使内容延伸到屏幕底，版本列表/下载列表最后一项被 FAB 与手势导航条遮挡、滚不到底。
- 修复：`activity_main.xml` 的 `ui_layout` 增加 `android:paddingBottom="108dp"` + `android:clipToPadding="false"`，为 FAB（约 75dp 高 + 16dp 边距）留出滚动余量。

### 11.3 微软正版登录 AADSTS900144: client_id missing
- 根因：`FCL/build.gradle.kts` 中 `oauth_api_key` 来自环境变量 `OAUTH_API_KEY` / `local.properties`；fork 的 GitHub Actions 未配置该 secret，构建出的 APK 里 `R.string.oauth_api_key` 为空（或字面 "null"），OAuth token 请求 body 不带 client_id，被 Azure 拒绝。
- 修复：在 `FCL/build.gradle.kts` 给 `oauthApiKey` 加兜底——未注入 secret 时回退到众所周知、已被 Xbox/Minecraft 服务放行的官方 Minecraft 启动器公开 client_id `00000000402b5328`；官方 secret 仍优先。

### 构建结果
- Run：https://github.com/HaQiMi-Din/HelloFoldCraftLauncher/actions/runs/35435907910 — success（5/5 ABI）。
- arm64 debug APK 已下载到工作区 `work/apk/`。
