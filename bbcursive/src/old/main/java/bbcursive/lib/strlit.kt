package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.MessageFormat

/**
 * Created by jim on 1/17/16.
 */
object strlit {
    fun strlit(s: CharSequence): UnaryOperator<ByteBuffer> {
        return ByteBufferUnaryOperator(s)
    }

    private class ByteBufferUnaryOperator(private val s: CharSequence) :
        UnaryOperator<ByteBuffer> {
        override fun toString(): String {
            return MessageFormat.format("u8\"{0}\"", s)
        }

        override operator fun invoke(buffer: ByteBuffer): ByteBuffer {
            val encode = StandardCharsets.UTF_8.encode(s.toString())
            while (encode.hasRemaining() && buffer!!.hasRemaining() && encode.get() == buffer.get());
            return if (encode.hasRemaining()) std.NULL_BUFF else buffer
        }
    }
}