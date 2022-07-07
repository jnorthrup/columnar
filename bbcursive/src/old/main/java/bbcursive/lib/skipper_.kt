package bbcursive.lib

import bbcursive.ann.Skipper
import bbcursive.func.UnaryOperator
import bbcursive.std
import bbcursive.std.traits.skipper
import java.nio.ByteBuffer
import java.util.*

@Skipper
interface skipper_ {
    @Skipper
    class ByteBufferUnaryOperator(
        vararg val allOf: UnaryOperator<ByteBuffer>
    ) : UnaryOperator<ByteBuffer> {
        override fun toString(): String {
            return "skipper" + Arrays.deepToString(allOf)
        }

        override operator fun invoke(p1: ByteBuffer): ByteBuffer {
            std.flags.apply{
                set(get() + skipper as Set<std.traits>)
            }
            return std.bb(p1!!,  *allOf)!!
        }
    }

    companion object {
        @Skipper
        fun skipper(vararg allOf: UnaryOperator<ByteBuffer> ): UnaryOperator<ByteBuffer> {
            return ByteBufferUnaryOperator(    *allOf)
        }
    }
}