package bbcursive

import bbcursive.lib.log.log
import java.nio.ByteBuffer
import java.rmi.server.LogStream

/**
 * User: jim
 * Date: Oct 6, 2007
 * Time: 3:10:32 AM
 */
class Allocator(vararg bytes: Int) {
    var DIRECT_HEAP: ByteBuffer? = null
    private var initialCapacity = Runtime.getRuntime().availableProcessors() * 20 * 2
    val EMPTY_SET = ByteBuffer.allocate(0).asReadOnlyBuffer()
    var size = initialCapacity
    private fun init() {
        var buffer: ByteBuffer? = null
        while (buffer == null) try {
            buffer = if (isDirect) ByteBuffer.allocateDirect(size).limit(0) else ByteBuffer.allocate(size).limit(0)
            DIRECT_HEAP = buffer
            LogStream.log("Heap allocated at " + size / MEG + " megs")
            size *= 2
        } catch (e: IllegalArgumentException) {
            size = Math.max(16 * MEG, size / 2)
            System.gc()
        } catch (e: OutOfMemoryError) {
            size = Math.max(16 * MEG, size / 2)
            System.gc()
        }
    }

    fun allocate(size: Int): ByteBuffer {
        if (size == 0) return EMPTY_SET
        try {
            DIRECT_HEAP!!.limit(DIRECT_HEAP!!.limit() + size)
        } catch (e: IllegalArgumentException) {
            init()
            return allocate(size)
        }
        val ret = DIRECT_HEAP!!.slice().limit(size).mark()
        DIRECT_HEAP!!.position(DIRECT_HEAP!!.limit())
        return ret
    }

    val isDirect: Boolean
        get() = false

    companion object {
        var MEG = 1 shl 10 shl 10
        var BLOCKSIZE = MEG * 2
    }

    init {
        if (bytes.size > 0) initialCapacity = bytes[0]
        var buffer: ByteBuffer? = null
        while (buffer == null) try {
            buffer = if (isDirect) ByteBuffer.allocateDirect(size).limit(0) else ByteBuffer.allocate(size).limit(0)
            DIRECT_HEAP = buffer
            log("Heap allocated at ${size / MEG} megs")
            size *= 2
        } catch (e: IllegalArgumentException) {
            size = Math.max(16 * MEG, size / 2)
            System.gc()
        } catch (e: OutOfMemoryError) {
            size = Math.max(16 * MEG, size / 2)
            System.gc()
        }
    }
}