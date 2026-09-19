cmake_minimum_required(VERSION 3.18.1)

project("hfcl")

# 设置C标准
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

# 查找ByteHook库
find_package(bytehook REQUIRED CONFIG)

# 设置全局包含目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# 检查架构
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_definitions(-DADRENO_POSSIBLE)
endif()

# 模块：hfcl
add_library(
    hfcl
    SHARED
    fcl/fcl_bridge.c
    fcl/fcl_loader.c
)

target_include_directories(
    hfcl
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/fcl/include
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
    hfcl
    PRIVATE
    bytehook::bytehook
    log
    dl
    android
)

# 模块：linkerhook - 与 Amethyst-Android 布局一致，hook.c 位于 driver_helper/
add_library(
    linkerhook
    SHARED
    driver_helper/hook.c
)

target_link_options(
    linkerhook
    PRIVATE
    -z global
)

# 模块：awt_headless - 占位库
add_library(
    awt_headless
    SHARED
    # 空库，仅作为占位
    ${CMAKE_CURRENT_SOURCE_DIR}/awt_headless/placeholder.c
)

# 为占位库添加必要的链接选项
target_link_libraries(
    awt_headless
    PRIVATE
    log
)

# 模块：awt_xawt
add_library(
    awt_xawt
    SHARED
    awt_xawt/xawt_fake.c
)

target_include_directories(
    awt_xawt
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/awt_xawt
)

target_link_libraries(
    awt_xawt
    PRIVATE
    awt_headless
)

# 模块：pojavexec_awt
add_library(
    pojavexec_awt
    SHARED
    awt_bridge.c
)

target_link_libraries(
    pojavexec_awt
    PRIVATE
    hfcl
)

# 模块：pojavexec
# ---------------------------------------------------------------------------
# 窗口/输入后端：当前本模块通过自实现 org.lwjgl.glfw.GLFW* / CallbackBridge*
# 的 JNI 入口（见 input_bridge_v3.c、egl_bridge.c），在 Android ANativeWindow
# 上用 EGL/GLES 自建 GLFW 风格窗口。
#
# 迁移到 SDL3（LWJGL 3.4.1 内置 org.lwjgl.sdl）后，窗口/输入应由 libSDL3.so
# 承担；libSDL3.so 由 LWJGL natives 以预编译 .so 形式提供（位于
# app_runtime/lwjgl/3.4.1/natives/<abi>/），【不在此处从源码编译】，
# 故本文件不新增 SDL3 源文件、不链接 SDL3。
#
# 如需自绘 SDL3 窗口桥（把下方 GLFW JNI 入口改写成 org.lwjgl.sdl.* 等价物），
# 届时再在此处追加对应 .c 源文件与 target_link_libraries(... SDL3)。
# 当前仅做构建/打包层切换，native 重写需 NDK 环境，详见迁移记录。
add_library(
    pojavexec
    SHARED
    bigcoreaffinity.c
    egl_bridge.c
    ctxbridges/loader_dlopen.c
    ctxbridges/gl_bridge.c
    ctxbridges/osm_bridge.c
    ctxbridges/egl_loader.c
    ctxbridges/osmesa_loader.c
    ctxbridges/swap_interval_no_egl.c
    environ/environ.c
    input_bridge_v3.c
    virgl/virgl.c
    jre_launcher.c
    jvm_hooks/java_exec_hooks.c
    jvm_hooks/emui_iterator_fix_hook.c
    jvm_hooks/lwjgl_dlopen_hook.c
    driver_helper/nsbypass.c
)

target_include_directories(
    pojavexec
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/ctxbridges
    ${CMAKE_CURRENT_SOURCE_DIR}/environ
    ${CMAKE_CURRENT_SOURCE_DIR}/virgl
)

target_link_libraries(
    pojavexec
    PRIVATE
    hfcl
    log
    dl
    android
)

# 仅在arm64-v8a架构下链接EGL和GLESv2
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_link_libraries(
        pojavexec
        PRIVATE
        EGL
        GLESv2
    )
endif()
