plugins {
    kotlin("multiplatform") version "1.6.0"
}
kotlin {
    linuxX64 {
        binaries { val main by compilations.getting {
            compilations["main"].cinterops {
                create("native") {
                    defFile = project.file("src/nativeInterop/cinterop/linux_uring.def")
                }
            }
        }
            "fixedlink".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.$it" }}
            //"iopoll".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.readwrite.iopoll" }}
            "lfsopenat".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.$it" }}
            "link".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" }}
            "register".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" }}
            "teardown".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" }}
            //"update".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" }}
            "timeout".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" } }
            "uring_cat".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "linux_uring.cat_file" } }
        }

    }
}
