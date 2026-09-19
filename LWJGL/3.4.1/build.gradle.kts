plugins {
    java
}

val lwjglVersion = "3.4.1"
group = "org.lwjgl.glfw"

configurations {
    create("lwjglModules") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "../compileOnly", "include" to listOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "libs/$lwjglVersion", "include" to listOf("*.jar"))))
    add(
        "lwjglModules",
        fileTree(mapOf("dir" to "libs/$lwjglVersion", "include" to listOf("*.jar")))
    )
    implementation(libs.jspecify) // lwjgl3.3.3 has jsr305 included as a jar

}

tasks.jar {
    // 被排除的模块只经 doLast 复制、不参与合并，需显式声明为输入，
    // 否则更新这些 jar 后任务会误判 UP-TO-DATE，导致 copy 与 version 不更新
    inputs.files(configurations["lwjglModules"])

    // =====================================================================
    // 窗口/输入后端：GLFW -> SDL3 迁移说明（LWJGL 3.4.1）
    // ---------------------------------------------------------------------
    // * lwjgl-glfw.jar  : GLFW 窗口后端的 Java 绑定。MC 仍直接调用 org.lwjgl.glfw.*，
    //                     故本 jar 仍参与 merged jar（不在 excludedModules 中），不能删；
    //                     迁移的是【其背后的原生窗口/输入实现】，而非删除 glfw API 绑定。
    // * lwjgl-sdl.jar   : LWJGL 3.4.1 内置的 SDL3 绑定（包 org.lwjgl.sdl），
    //                     是本次迁移选定的【活动原生窗口/输入后端】。它与其它独立模块
    //                     一样走 excludedModules 原样拷贝到运行时 classes 目录，
    //                     不参与 merged jar，保证 org.lwjgl.sdl 类完整可加载。
    //   - SDL3 原生库由 LWJGL natives 提供，so 名为 libSDL3.so（运行时按
    //     "SDL3" 名 dlopen，可用 -Dorg.lwjgl.sdl.libname= 覆盖），随各 ABI 铺在
    //     assets/app_runtime/lwjgl/3.4.1/natives/<abi>/ 下（由 FCL 打包脚本与
    //     natives aar 负责，见 FCL/build.gradle.kts 的 natives 段）。
    //   - 注意：SDL3 绑定自 LWJGL 3.4.x 才有，3.3.3 分支无 lwjgl-sdl，仍走 GLFW。
    // =====================================================================

    // Modules to copy over to the components directory instead of patching and merging
    val excludedModules = arrayOf(
        "lwjgl-lwjglx.jar",
        "lwjgl.jar",
        "jsr305.jar",
        "lwjgl-freetype.jar",
        "lwjgl-jemalloc.jar",
        "lwjgl-nanovg.jar",
        "lwjgl-openal.jar",
        "lwjgl-stb.jar",
        "lwjgl-tinyfd.jar",
        "lwjgl-shaderc.jar",
        "lwjgl-spvc.jar",
        "lwjgl-vma.jar",
        "lwjgl-vulkan.jar",
        // SDL3 窗口/输入后端（LWJGL 3.4.1 内置绑定）：活动原生后端，原样拷贝到运行时；
        // glfw Java 绑定保持参与合并（MC 仍引用 org.lwjgl.glfw.*），故不在此排除
        "lwjgl-sdl.jar",
        "lwjgl-spng.jar"
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-${lwjglVersion}-merged-modules")
    destinationDirectory.set(file("$rootDir/FCL/src/main/assets/app_runtime/lwjgl/${lwjglVersion}"))

    from({
        // Ensure that the core lwjgl jar is processed first so duplicates in META-INF from other classes
        // are ignored. This avoids InvalidModuleDescriptorException due to say, using the module-info.class
        // from lwjgl-jemalloc.
        val includedModules = configurations["lwjglModules"].filter { dep ->
            !excludedModules.any { it == dep.name }
        }
        val coreJar = includedModules.find { it.name == "lwjgl.jar" }
        val jarList =
            if (coreJar != null) listOf(coreJar) + (includedModules - coreJar) else includedModules
        println("Merging LWJGL $lwjglVersion modules in the order: ")
        jarList.map {
            println(it.name)
            if (it.isDirectory) it else zipTree(it)
        }
    })

    // Makes the jar reproducible so the version file actually is a version file
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    val versionFile = File(destinationDirectory.get().asFile, "version")
    doLast {
        val excludedModulesFileList = excludedModules.flatMap { fileName ->
            configurations["lwjglModules"].filter { it.name == fileName }
        }
        copy {
            // Copy excluded modules to the lwjgl classes dir
            from(excludedModulesFileList)
            into(archiveFile.get().asFile.parentFile)
        }
        versionFile.writeText(System.currentTimeMillis().toString())
    }
    // Adds the jank to outputs
    outputs.file(versionFile)
    outputs.files(excludedModules.map { path -> File(destinationDirectory.get().asFile, path) })
    exclude("net/java/openjdk/cacio/ctc/**")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}