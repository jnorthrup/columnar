rootProject.name = "columnar"
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
//        includeBuild("buildsrc")
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
//    includeBuild("../somethingelse")
}

include(*(rootDir.listFiles()
.filter(File::isDirectory)
.filter { !it.isHidden }
.map(File::getName)-"buildSrc"-"gradle").toTypedArray())