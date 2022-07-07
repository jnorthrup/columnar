package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jim on 1/17/16.
 */
internal object opt_ {
    @JvmStatic
    fun opt(
        vararg unaryOperators:UnaryOperator<ByteBuffer>
    ): UnaryOperator<ByteBuffer> {
        return ByteBufferUnaryOperator(*unaryOperators)
    }

    class ByteBufferUnaryOperator(vararg val allOrPrevious: UnaryOperator<ByteBuffer>) :
        UnaryOperator<ByteBuffer> {
        override fun toString(): String = "opt:" + Arrays.deepToString(allOrPrevious)
        override operator fun invoke(buffer: ByteBuffer): ByteBuffer {
            val position = buffer!!.position()
            val r = std.bb(buffer, *allOrPrevious  )
            if (null == r) buffer.position(position)
            return buffer
        }
    }
}