package bbcursive.lib

import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object push {
    /**
     * @param src
     * @param dest
     * @return
     */
    fun push(src: ByteBuffer, dest: ByteBuffer): ByteBuffer {
        val need = src  .remaining()
        val have = dest.remaining()
        return if (have <= need) {
            dest.put(src.slice().limit(have))
            src.position(src.position() + have)
            dest
        } else {
            dest.put(src)
        }
    }
}