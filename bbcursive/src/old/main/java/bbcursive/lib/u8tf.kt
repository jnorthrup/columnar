package bbcursive.lib

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * unique code completion for utf8
 */
object u8tf {
    /**
     * utf8 encoder macro
     * @param charseq
     * @return
     */
    fun c2b(charseq: String): ByteBuffer {
        return StandardCharsets.UTF_8.encode(charseq)
    }

    /**
     * UTF8 decoder macro
     *
     * @param buffer
     * @return defered  string translation decision
     */
    fun b2c(buffer: ByteBuffer): CharSequence {
        return StandardCharsets.UTF_8.decode(buffer)
    }
}