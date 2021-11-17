 plugins {
    kotlin("multiplatform") version "1.6.0"
}
kotlin {

    jvm {  }
    linuxX64{}
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