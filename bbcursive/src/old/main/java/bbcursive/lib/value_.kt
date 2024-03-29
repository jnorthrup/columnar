package bbcursive.lib

import bbcursive.ann.ForwardOnly
import bbcursive.ann.Infix
import bbcursive.func.UnaryOperator
import bbcursive.lib.anyOf_.anyIn
import java.nio.ByteBuffer

/**
 * Created by jim on 1/21/16.
 */
@Infix
@ForwardOnly
class value_ private constructor() : UnaryOperator<ByteBuffer> {
    override fun invoke(p1: ByteBuffer): ByteBuffer {
        val infix = infix_.infix(
            opt_.opt(chlit_.chlit("0")),
            anyIn("1.0"),
            opt_.opt(repeat_.repeat(anyIn("1029384756"))))
        return infix(p1)
    }

    companion object {
        val VALUE_ = value_()
        fun value(): value_ {
            return VALUE_
        }
    }
}