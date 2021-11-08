plugins {
    kotlin("multiplatform") version "1.6.0-RC"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64("native") { // on Linux
        // macosX64("native") { // on x86_64 macOS
        // macosArm64("native") { // on Apple Silicon macOS
        // mingwX64("native") { // on Windows
        val main by compilations.getting
        val interop by main.cinterops.creating

        binaries {
            executable()
        }
    }
}
