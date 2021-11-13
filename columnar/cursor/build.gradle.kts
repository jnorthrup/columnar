 plugins {
    kotlin("multiplatform") version "1.6.0-RC"
//    id("org.jetbrains.dokka") version "1.4.32"
//    id("maven-publish")
//    id("signing")
}

group = "columnar"
version = "1.0.2-SNAPSHOT"

val isRunningInIde: Boolean = System.getProperty("idea.active")
    ?.toBoolean() == true

val testApp: String? by extra

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":vect0r"))
                api(project(":trie"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val posixMain by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting{
            dependencies{
                dependsOn(commonMain)
                api("org.bouncycastle:bcprov-jdk15on:1.69")
            }
        }
        val jvmTest by getting {
            dependencies {
                dependsOn(commonTest)
            }
        }
    }
}