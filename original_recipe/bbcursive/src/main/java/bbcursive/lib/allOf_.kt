package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std.bb
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jim on 1/17/16.
 */
interface allOf_ {
    companion object {
        /**
         * bbcursive.lib.allOf_ of, in sequence, without failures
         *
         * @param allOf
         * @return null if not bbcursive.lib.allOf_ match in sequence
         */
        @JvmStatic
        fun allOf(vararg allOf: UnaryOperator<ByteBuffer?>): UnaryOperator<ByteBuffer?> {
            return object : UnaryOperator<ByteBuffer?> {
                override fun toString(): String {
                    return "all" + Arrays.deepToString(allOf)
                }

                override operator fun invoke(p1: ByteBuffer?): ByteBuffer? {
                    return bb(p1!!, *allOf)!!
                }
            }
        }
    }
}