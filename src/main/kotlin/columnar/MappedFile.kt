package columnar

import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * mapmodes  READ_ONLY, READ_WRITE, or PRIVATE
"r"	Open for reading only. Invoking any of the write methods of the resulting object will cause an IOException to be thrown.
"rw"	Open for reading and writing. If the file does not already exist then an attempt will be made to create it.
"rws"	Open for reading and writing, as with "rw", and also require that every update to the file's content or metadata be written synchronously to the underlying storage device.
"rwd"  	Open for reading and writing, as with "rw", and also require that every update to the file's content be written synchronously to the underlying storage device.
 */
open class MappedFile(
    filename: String,
    val mode: String = "r",
    val mapMode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY,
    val randomAccessFile: RandomAccessFile = RandomAccessFile(
        filename,
        mode
    ),
    val channel: FileChannel = randomAccessFile.channel,
    val length: Long = randomAccessFile.length(),
    val mappedByteBuffer: ThreadLocal<MappedByteBuffer> = ThreadLocal.withInitial {
        channel.map(
            mapMode,
            0,
            min(Int.MAX_VALUE.toLong(), length)
        )
    }
) : FileAccess(filename), Closeable by randomAccessFile