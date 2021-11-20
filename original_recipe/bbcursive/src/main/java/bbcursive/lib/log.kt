package bbcursive.lib

import bbcursive.Cursive.pre
import bbcursive.WantsZeroCopy
import bbcursive.func.UnaryOperator
import bbcursive.std.bb
import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object log {
    /**
     * conditional debug output assert log(Object,[prefix[,suffix]])
     *
     * @param ob
     * @param prefixSuffix
     * @return
     */
    fun log(ob: Any, vararg prefixSuffix: String) {
        assert(`log$`(ob, *prefixSuffix))
    }

    /**
     * conditional debug output assert log(Object,[prefix[,suffix]])
     *
     * @param ob
     * @param prefixSuffix
     * @return
     */
    fun `log$`(ob: Any, vararg prefixSuffix: String): Boolean {
        val hasSuffix = 1 < prefixSuffix.size
        if (0 < prefixSuffix.size) System.err.print(prefixSuffix[0] + "\t")
        if (ob !is ByteBuffer) {
            if (ob is WantsZeroCopy) {
                bb(ob.asByteBuffer(), pre.debug as UnaryOperator<ByteBuffer?>)
            } else {
                bb(ob.toString(), pre.debug)
            }
        } else bb(ob, pre.debug)
        if (hasSuffix) {
            System.err.println(prefixSuffix[1] + "\t")
        }
        return true
    }
}