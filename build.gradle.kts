import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70-eap-42"
}
group = "columnar"
version = "1.0-SNAPSHOT"
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}
repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "13"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "13"
}
tasks.withType(KotlinCompile::class)
    .forEach {
        it.kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xnew-inference",
                "-Xinline-classes",
                "-Xuse-experimental=kotlin.Experimental")
        }
    }

tasks.named<Test>("test") {
    useJUnitPlatform()
}
