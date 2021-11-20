package bbcursive.lib

import bbcursive.func.UnaryOperator
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
open class pos(private val position: Int) : UnaryOperator<ByteBuffer?> {


    override fun invoke(t: ByteBuffer?): ByteBuffer? {
        return if (null == t) t!! else t.position(position)
    }

    override fun toString(): String {
        return "pos($position)"
    }
}