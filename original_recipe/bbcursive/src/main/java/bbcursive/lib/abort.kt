package bbcursive.lib

import bbcursive.func.UnaryOperator
import bbcursive.std
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object abort {
    @JvmStatic
    fun abort(rollbackPosition: Int): UnaryOperator<ByteBuffer?> = object :
        UnaryOperator<ByteBuffer?> {
        override fun invoke(it: ByteBuffer?): ByteBuffer?{
            return if (null == it) null else std.bb(it,pos (rollbackPosition),ABORT_ONLY)
        }
    }

    val  EMPTY_BUFF :ByteBuffer get()= TODO()
    object  ABORT_ONLY:UnaryOperator<ByteBuffer?> { override fun invoke(p1: ByteBuffer?): ByteBuffer? = TODO("handle ABORT_ONLY") }
}
