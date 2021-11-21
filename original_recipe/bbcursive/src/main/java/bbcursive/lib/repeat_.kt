package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jim on 1/17/16.
 */
internal object repeat_ {
    @JvmStatic
    fun repeat(vararg op: UnaryOperator<ByteBuffer> ): UnaryOperator<ByteBuffer> {
        return object : UnaryOperator<ByteBuffer> {
            override fun toString(): String {
                return "rep:" + Arrays.deepToString(op)
            }

            override operator fun invoke( byteBuffer: ByteBuffer): ByteBuffer {
                var mark = byteBuffer!!.position()
                var matches = 0
                var handle = byteBuffer
                var last: ByteBuffer = std.NULL_BUFF
                while (handle!!.hasRemaining()) {
                    last = handle
                    //                if (null != (handle=op.apply(handle))) {
                    if (std.NULL_BUFF != std.bb(last, *op ).also { handle = it!! }) {
                        matches++
                        mark = handle!!.position()
                    } else break
                }
                if (matches > 0 && last!!.hasRemaining()) last.position(mark)
                return if (matches > 0) last else std.NULL_BUFF
            }
        }
    }
}