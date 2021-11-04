 plugins {
    kotlin ("multiplatform") version "1.6.0-RC"
//    id("org.jetbrains.dokka") version "1.4.32"
//    id("maven-publish")
//    id("signing")
}

group = "columnar"
version = "1.0.2-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {  jvm()
    linuxX64 { //sudo apt install liburing*
        binaries {
            staticLib()
            compilations["main"].cinterops {
                create("native") {
                    defFile = project.file("${buildFile.parent}/src/linuxX64Main/c_async.def")
                    packageName = "columnar.c_async"
//                    includeDirs.headerFilterOnly("${buildFile.parent}/src/linuxX64Main/cinterop")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
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
        val linuxX64Main by getting{dependsOn(posixMain)}

        val jvmMain by getting{
            dependencies{
                dependsOn(commonMain)
            }
        }
        val jvmTest by getting {
            dependencies {
                dependsOn(commonTest)
            }
        }
    }
}