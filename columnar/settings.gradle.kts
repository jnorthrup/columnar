
rootProject.let {
    it.name = "columnar"
/*
    it.group = "columnar"
    it.version = "1.0.2-SNAPSHOT"
*/

}
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
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
