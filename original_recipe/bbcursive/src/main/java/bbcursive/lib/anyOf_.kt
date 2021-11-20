package bbcursive.lib

import bbcursive.Cursive.pre
import bbcursive.ann.Backtracking
import bbcursive.func.UnaryOperator
import bbcursive.std
import bbcursive.std.traits
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by jim on 1/17/16.
 */
object anyOf_ {
    val NONE_OF = EnumSet.noneOf(traits::class.java)
    @SafeVarargs
    fun anyOf(vararg anyOf: UnaryOperator<ByteBuffer?>): UnaryOperator<ByteBuffer?> {
        return object : UnaryOperator<ByteBuffer?> {
            override fun toString(): String {
                return "any${Arrays.deepToString(anyOf)}"
            }

            override fun invoke(p1: ByteBuffer?): ByteBuffer? {
                var buffer = p1
                var mark = buffer!!.position()
                if (std.flags.get().contains(traits.skipper)) {
                    val apply = pre.skipWs.invoke(buffer)
                    buffer = apply ?: buffer.position(mark)
                    if (!buffer.hasRemaining()) {
                        return null
                    }
                }
                mark = buffer!!.position()
                for (function in anyOf) {
                    val function1 = function.invoke(buffer)
                    if(function1 !=null)return buffer

                }
                buffer.position(mark)
                return null
            }
        }
    }

    @JvmStatic
    @Backtracking
    fun anyIn(s: String): UnaryOperator<ByteBuffer?> {
        val ints = s.chars().sorted().toArray()
        return object : UnaryOperator<ByteBuffer?> {
            override fun toString(): String {
                val b = StringBuilder()
                for (i in ints)
                    b.append(i.toChar())
                return "in ${Arrays.deepToString(arrayOf(b.toString()))}"
            }

            override fun invoke(b: ByteBuffer?): ByteBuffer? {
                var r: ByteBuffer? = null
                when {
                    null != b && b.hasRemaining() -> {
                        val b1 = b.get()
                        if (-1 < Arrays.binarySearch(ints, b1.toUByte().toInt())) r = b
                    }
                }
                return r
            }
        }
    }
}