package vec.util

import java.nio.file.Paths

val String.path get() = Paths.get(this)