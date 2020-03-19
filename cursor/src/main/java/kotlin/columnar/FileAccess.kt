package columnar

import java.io.Closeable

abstract class FileAccess(open val filename: String) : Closeable