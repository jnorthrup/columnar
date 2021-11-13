plugins {
    kotlin("multiplatform") version "1.6.0-RC"
}
kotlin {
    jvm()
    linuxX64{}
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val posixMain by creating {
            dependsOn(commonMain)
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}