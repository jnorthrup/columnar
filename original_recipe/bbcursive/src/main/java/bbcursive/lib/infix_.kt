package bbcursive.lib

import bbcursive.ann.Infix
import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer
import java.util.*

interface infix_ {
    @Infix

    class infix(
        vararg val allOf: UnaryOperator<ByteBuffer>
    ) : UnaryOperator<ByteBuffer>
 {

        override operator fun invoke(p1: ByteBuffer): ByteBuffer {
            return std.bb(if (p1 != std.NULL_BUFF) p1 else std.NULL_BUFF, *allOf) as ByteBuffer
        }
        override fun toString(): String {
            return "infix" + Arrays.deepToString(allOf)
        }
    }

    companion object {
        @Infix
        fun infix(
            vararg allOf: UnaryOperator<ByteBuffer>
        ): UnaryOperator<ByteBuffer> {
            return infix(*allOf)
        }
    }
}