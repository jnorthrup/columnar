package bbcursive.lib

import bbcursive.func.UnaryOperator
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
internal class lim_ private constructor() {
    class lim(val position: Int) : UnaryOperator<ByteBuffer?> {
        override operator fun invoke(p1: ByteBuffer?): ByteBuffer? {
            return p1!!.limit(position)
        }
    }
}