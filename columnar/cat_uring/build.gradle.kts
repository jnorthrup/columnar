plugins {
    kotlin("multiplatform") version "1.6.0"
}
kotlin {
    linuxX64 {
        binaries {
            "uring_cat".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "linux_uring.cat_file" } }
            "fixedlink".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.$it" }}
            "link".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.main" }}
            "lfsopenat".let { executable(it, listOf(DEBUG/*, RELEASE*/)) { baseName = it; entryPoint = "test.$it.$it" }}
        }
        val main by compilations.getting {
            compilations["main"].cinterops {
                create("native") {
                    defFile = project.file("src/nativeInterop/cinterop/linux_uring.def")
                }
            }
        }
    }
}
