plugins {
    kotlin("multiplatform") version "1.6.0-RC"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable("my_executable", listOf(DEBUG,RELEASE,)) {
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
