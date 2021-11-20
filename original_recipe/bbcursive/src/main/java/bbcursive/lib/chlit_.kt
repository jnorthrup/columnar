package bbcursive.lib

import bbcursive.func.UnaryOperator
import java.nio.ByteBuffer

/**
 * char literal
 */
object chlit_ {
      @JvmStatic
  fun chlit(c: Char): UnaryOperator<ByteBuffer?> {
        return ByteBufferUnaryOperator1(c)
    }

    @JvmStatic
    fun chlit(s: String): UnaryOperator<ByteBuffer?> {
        return chlit(s[0])
    }

    private class ByteBufferUnaryOperator1(private val c: Char) : UnaryOperator<ByteBuffer?> {
        override fun toString(): String {
            return "c8'" +
                    c + "'"
        }

        override fun invoke(buf: ByteBuffer?): ByteBuffer? {
            if (null == buf) {
                return null
            }
            if (buf.hasRemaining()) {
                val b = buf.get()
                return if (c.code .toUByte() == b .toUByte()) buf else null
            }
            return null
        }
    }
}