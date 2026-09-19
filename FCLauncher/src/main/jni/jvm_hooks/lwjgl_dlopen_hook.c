//
// Created by maks on 06.01.2025.
//

#include "jvm_hooks.h"

#include "environ/environ.h"
#include "fcl/include/fcl_internal.h"

#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>

extern void* maybe_load_vulkan();

/**
 * Basically a verbatim implementation of ndlopen(), found at
 * https://github.com/PojavLauncherTeam/lwjgl3/blob/3.3.1/modules/lwjgl/core/src/generated/c/linux/org_lwjgl_system_linux_DynamicLinkLoader.c#L11
 * but with our own additions for stuff like vulkanmod.
 */
static jlong ndlopen_bugfix(__attribute__((unused)) JNIEnv *env,
                     __attribute__((unused)) jclass class,
                     jlong filename_ptr,
                     jint jmode) {
    const char* filename = (const char*) filename_ptr;

    // 窗口后端 GLFW -> SDL3：LWJGL 3.4.1 的 SDL3 绑定按 "SDL3" / "libSDL3.so" 名加载原生库。
    // 该库随 natives 铺在 app_runtime/lwjgl/3.4.1/natives/<abi>/，已通过 FCLauncher.appendCommonPaths
    // 加入 java.library.path，故此处直接走默认 dlopen 即可解析；仅加日志便于确认后端已切换。
    if (strstr(filename, "SDL3") != NULL) {
        FCL_LOG("LWJGL linkerhook: loading SDL3 window backend native: %s", filename);
    }

    // Oveeride vulkan loading to let us load vulkan ourselves
    if(strstr(filename, "libvulkan.so") == filename) {
        FCL_LOG("LWJGL linkerhook: replacing load for libvulkan.so with custom driver");
        return (jlong) maybe_load_vulkan();
    }

    // FCL 特有：插件渲染器（如 VirGL）通过 RENDERER_HANDLE 环境变量直接返回已加载的库句柄
    if (getenv("RENDERER_HANDLE") != NULL && strstr(filename,"plugin")) {
        return (jlong) strtol(getenv("RENDERER_HANDLE"), NULL, 10);
    }

    // This hook also serves the task of mitigating a bug: the idea is that since, on Android 10 and
    // earlier, the linker doesn't really do namespace nesting.
    // It is not a problem as most of the libraries are in the launcher path, but when you try to run
    // VulkanMod which loads shaderc outside of the default jni libs directory through this method,
    // it can't load it because the path is not in the allowed paths for the anonymous namesapce.
    // This method fixes the issue by being in libpojavexec, and thus being in the classloader namespace

    int mode = (int)jmode;
    return (jlong) dlopen(filename, mode);
}

/**
 * Install the LWJGL dlopen hook. This allows us to mitigate linker bugs and add custom library overrides.
 */
void installLwjglDlopenHook(JNIEnv *env) {
    FCL_LOG("Installing LWJGL dlopen() hook");
    jclass dynamicLinkLoader = (*env)->FindClass(env, "org/lwjgl/system/linux/DynamicLinkLoader");
    if(dynamicLinkLoader == NULL) {
        FCL_LOG("Failed to find the target class");
        (*env)->ExceptionClear(env);
        return;
    }
    JNINativeMethod ndlopenMethod[] = {
            {"ndlopen", "(JI)J", &ndlopen_bugfix}
    };
    if((*env)->RegisterNatives(env, dynamicLinkLoader, ndlopenMethod, 1) != 0) {
        FCL_LOG("Failed to register the hooked method");
        (*env)->ExceptionClear(env);
    }
}
