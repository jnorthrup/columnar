package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object abort {
    @JvmStatic
    fun abort(rollbackPosition: Int): UnaryOperator<ByteBuffer> = object :
        UnaryOperator<ByteBuffer> {
        override fun invoke(it: ByteBuffer): ByteBuffer {
            return if (std.NULL_BUFF != it) std.bb(it, pos(rollbackPosition), std.ABORT_ONLY) else std.NULL_BUFF
        }
    }

}
