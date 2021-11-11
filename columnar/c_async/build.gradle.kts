plugins {
    kotlin("multiplatform") version "1.6.0-RC"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable("uring_read", listOf(DEBUG, RELEASE)) {
                // Base name for the output file.
                baseName = "ufoo"
                // Accessing the output file.
                println("Executable path: ${outputFile.absolutePath}")

                // Custom entry point function.
                entryPoint = "uring.main"
            }
            executable("simple_read", listOf(DEBUG, RELEASE)) {
                // Base name for the output file.
                baseName = "foo"
                // Accessing the output file.
                println("Executable path: ${outputFile.absolutePath}")

                // Custom entry point function.
                entryPoint = "simple.main"
            }
        }
        val main by compilations.getting

    }
}
