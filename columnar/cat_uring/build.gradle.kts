plugins { 
    kotlin("multiplatform") version "1.6.0-RC"
}
kotlin {
    linuxX64  {
        binaries {
            executable("uring_read"//, listOf(DEBUG, RELEASE)
            ) {
                // Base name for the output file.
                baseName = "uring_cat"
                // Accessing the output file.
                println("Executable path: ${outputFile.absolutePath}")
                // Custom entry point function.
                entryPoint = "linux_uring.cat_file"
            }
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
