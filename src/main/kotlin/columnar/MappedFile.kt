package columnar

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * mapmodes  READ_ONLY, READ_WRITE, or PRIVATE
"r"	Open for reading only. Invoking any of the write methods of the resulting object will cause an IOException to be thrown.
"rw"	Open for reading and writing. If the file does not already exist then an attempt will be made to create it.
"rws"	Open for reading and writing, as with "rw", and also require that every update to the file's content or metadata be written synchronously to the underlying storage device.
"rwd"  	Open for reading and writing, as with "rw", and also require that every update to the file's content be written synchronously to the underlying storage device.
 */
open class MappedFile(
    filename: String,
    mode: String = "r",
    mapMode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY,
    val randomAccessFile: RandomAccessFile = RandomAccessFile(
        filename,
        mode
    ),
    channel: FileChannel = randomAccessFile.channel,
    length: Long = randomAccessFile.length(),
    val mappedByteBuffer: MappedByteBuffer = channel.map(mapMode, 0, length),
    override val size: Int = mappedByteBuffer.limit(),
    /**default returns a line seeked EOL buffer.*/
    override var values: suspend (Int) -> Flow<ByteBuffer> = { row ->
        flowOf(mappedByteBuffer.apply { position(row) }.slice().also {
            while (it.hasRemaining() && it.get() != '\n'.toByte());
            (it as ByteBuffer).flip()
        })
    }
) : FileAccess(filename),
    RowStore<Flow<ByteBuffer>>, Closeable by randomAccessFile