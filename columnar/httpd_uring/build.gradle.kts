plugins {
    kotlin("multiplatform") version "1.6.0"
}
kotlin {
    linuxX64 {
        binaries {
            executable("uring_httpd"//, listOf(DEBUG, RELEASE)
            ) {
                // Base name for the output file.
                baseName = "uring_httpd"
                // Accessing the output file.
                println("Executable path: ${outputFile.absolutePath}")
                // Custom entry point function.
                entryPoint = "lib_uring.httpd"
            }
        }
        val main by compilations.getting {
            compilations["main"].cinterops {
                create("native") {
                    defFile = project.file("src/nativeInterop/cinterop/uring_httpd.def")
                }
            }
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":cat_uring"))
            }
        }
        val linuxX64Main by getting {
            dependsOn(commonMain)
        }
    }
}
