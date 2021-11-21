package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.lib.allOf_.Companion.allOf
import bbcursive.lib.chlit_.chlit
import bbcursive.std.bb
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jim on 1/17/16.
 */
object confix_ {
    fun confix(operator: UnaryOperator<ByteBuffer>, vararg chars: Char): UnaryOperator<ByteBuffer> {
        return object : UnaryOperator<ByteBuffer> {
            override fun toString(): String {
                return "confix_:" + Arrays.toString(chars) + " : " + operator
            }
            override operator fun invoke(p1: ByteBuffer): ByteBuffer {
                val chlit = chlit(chars[0])
                val aChar = chars[if (2 > chars.size) 0 else 1]
                val chlit1 = chlit(aChar)
                return bb(p1, confix(chlit, chlit1, operator))
            }
        }
    }

    fun confix(
        before: UnaryOperator<ByteBuffer>,
        after: UnaryOperator<ByteBuffer>,
        operator: UnaryOperator<ByteBuffer>
    ): UnaryOperator<ByteBuffer> {
        return object : UnaryOperator<ByteBuffer> {
            override fun toString(): String {
                return "confix" + Arrays.deepToString(arrayOf<UnaryOperator<*>>(before, operator, after))
            }

            override operator fun invoke(p1: ByteBuffer): ByteBuffer {
                return bb(p1, allOf(before, operator, after))
            }
        }
    }

    fun confix(open: Char, unaryOperator: UnaryOperator<ByteBuffer>, close: Char): UnaryOperator<ByteBuffer> {
        return confix(unaryOperator, open, close)
    }

    fun confix(s: String, unaryOperator: UnaryOperator<ByteBuffer>): UnaryOperator<ByteBuffer> {
        return confix(unaryOperator, *s.toCharArray())
    }
}