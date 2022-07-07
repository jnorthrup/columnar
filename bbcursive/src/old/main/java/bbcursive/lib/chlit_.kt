package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer

/**
 * char literal
 */
object chlit_ {
      @JvmStatic
  fun chlit(c: Char): UnaryOperator<ByteBuffer> {
        return ByteBufferUnaryOperator1(c)
    }

    @JvmStatic
    fun chlit(s: String): UnaryOperator<ByteBuffer> {
        return chlit(s[0])
    }

    private class ByteBufferUnaryOperator1(private val c: Char) : UnaryOperator<ByteBuffer> {
        override fun toString(): String {
            return "c8'" +
                    c + "'"
        }

        override fun invoke(p1: ByteBuffer): ByteBuffer {
            if (null == p1) {
                return std.NULL_BUFF
            }
            if (p1.hasRemaining()) {
                val b = p1.get()
                return if (c.code .toUByte() == b .toUByte()) p1 else std.NULL_BUFF
            }
            return std.NULL_BUFF
        }
    }
}