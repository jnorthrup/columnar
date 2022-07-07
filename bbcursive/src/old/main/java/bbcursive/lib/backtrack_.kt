package bbcursive.lib

import bbcursive.ann.Backtracking
import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer
import java.util.*

@Backtracking
internal object backtrack_ {
    @Backtracking
    fun backtracker(vararg allOf: UnaryOperator<ByteBuffer>): UnaryOperator<ByteBuffer> {

        @Backtracking
        class backTracker(vararg val allOf: UnaryOperator<ByteBuffer>) :
            UnaryOperator<ByteBuffer> {
            override fun toString(): String {
                return "backtracker" + Arrays.deepToString(allOf)
            }

            override operator fun invoke(p1: ByteBuffer): ByteBuffer {
                std.flags.apply { set(get() + std.traits.skipper) }
                return std.bb(p1!!, *allOf)
            }
        }
        return backtracker(*allOf)
    }
}