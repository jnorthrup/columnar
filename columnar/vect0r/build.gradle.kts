plugins {
    kotlin("multiplatform") version "1.6.0-RC"
}

kotlin {
    jvm {}
    linuxX64 {}
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val linuxX64Main by getting { dependsOn(commonMain) }
        val jvmMain by getting { dependsOn(commonMain) }
        val jvmTest by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}