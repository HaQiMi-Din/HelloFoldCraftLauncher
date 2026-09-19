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
