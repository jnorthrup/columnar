package bbcursive.lib

import bbcursive.func.UnaryOperator
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object advance {
    /**
     * consumes a token from the current ByteBuffer position.  null signals fail and should reset.
     *
     * @param exemplar ussually name().getBytes(), but might be other value also.
     * @return null if no match -- rollback not done here use Narsive.$ for whitespace and rollback
     */
    fun genericAdvance(vararg exemplar: Byte): UnaryOperator<ByteBuffer?> {
        return object : UnaryOperator<ByteBuffer?> {
            var bytes: ByteArray? = exemplar
            override fun toString(): String {
                return asString()!!
            }

            fun asString(): String? {
                bytes = exemplar
                return "advance->" + String(bytes!!)
            }

            override fun invoke(target: ByteBuffer?): ByteBuffer? {
                var c = 0
                while (null != exemplar && null != target && target.hasRemaining() && c < exemplar.size && exemplar[c] == target.get()) c++
                return if (!(null != target && c == exemplar.size)) null else target
            }
        }
    }
}