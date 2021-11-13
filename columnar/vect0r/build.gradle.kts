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
//    js { nodejs() }

    jvm()
    linuxX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val linuxX64Main by getting { dependsOn(commonMain)}
        val jvmMain by getting { dependsOn(commonMain)}
        val jvmTest by getting { dependsOn(commonMain)
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}
//
//val dokkaOutputDir = "$buildDir/dokka"
//
//tasks.dokkaHtml {
//    outputDirectory.set(file(dokkaOutputDir))
//}
//
//val deleteDokkaOutputDir by tasks.registering(Delete::class) {
//    delete(dokkaOutputDir)
//}
//
//val javadocJar by tasks.registering(Jar::class) {
//    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
//    archiveClassifier.set("javadoc")
//    from(dokkaOutputDir)
//}
//
//publishing {
//    publications.withType<MavenPublication> {
//        artifact(javadocJar)
//
//        pom {
//            name.set("file-io")
//            description.set("Kotlin/Native file IO library with standard java-io interface")
//            url.set("https://github.com/Archinamon/native-file-io")
//            licenses {
//                license {
//                    name.set("MIT License")
//                    url.set("https://github.com/Archinamon/native-file-io")
//                    distribution.set("repo")
//                }
//                license {
//                    name.set("The Apache License, Version 2.0")
//                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                }
//            }
//            developers {
//                developer {
//                    id.set("Archinamon")
//                    name.set("Eduard Obolenskiy")
//                    email.set("archinamon@gmail.com")
//                }
//            }
//            scm {
//                url.set("https://github.com/Archinamon/native-file-io")
//                connection.set("scm:git:https://github.com/Archinamon/native-file-io.git")
//                developerConnection.set("scm:git:git@github.com:Archinamon/native-file-io.git")
//            }
//        }
//    }
//
//    repositories {
//        if (isRunningInIde)
//            return@repositories
//
//        val isSnapshotPublishing: String? by extra
//        val repositoryUrl = if (isSnapshotPublishing?.toBoolean() == true)
//            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
//        else "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
//
//        maven(repositoryUrl) {
//            credentials {
//                val apiKey: String? by extra
//
//                username = "Archinamon"
//                password = apiKey ?: "[empty token]"
//            }
//        }
//    }
//}
//
//signing {
//    sign(publishing.publications)
//}
