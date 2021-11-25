plugins { 
    kotlin("multiplatform") version "1.6.0"
}
kotlin {
    linuxX64  {
        binaries {
          "uring_cat".let {
              executable(it,listOf(DEBUG, RELEASE)
              ) {
                  // Base name for the output file.
                  baseName = it
                  // Accessing the output file.
                  println("Executable path: ${outputFile.absolutePath}")
                  // Custom entry point function.
                  entryPoint = "linux_uring.cat_file"
              }
          }
            "fixedlink" .let {
                executable(it
                    ,listOf(DEBUG, RELEASE)
                ) {
                    // Base name for the output file.
                    baseName = it
                    // Accessing the output file.
                    println("Executable path: ${outputFile.absolutePath}")
                    // Custom entry point function.
                    entryPoint = "test.$it.$it"
                }
            }
        }
        val main by compilations.getting {
            compilations["main"].cinterops {
                create("native") {
                    defFile = project.file("src/nativeInterop/cinterop/linux_uring.def")
                }
            }
        }
    }
}
