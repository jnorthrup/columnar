package cursors.io

import com.sun.nio.file.ExtendedOpenOption
import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.*
import vec.macros.*
import vec.util.logDebug
import vec.util.span
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

