package bbcursive.lib

import java.nio.ByteBuffer

/**
 * Created by jim on 1/17/16.
 */
object Int_ {
    fun parseInt(r: ByteBuffer): Int? {
        var x: Long = 0
        var neg = false
        var res: Int? = null
        if (r.hasRemaining()) {
            var i = r.get().toInt().toChar()
            when (i) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> x = x * 10.toInt() + i.code - '0'.code.toLong()
                '-' -> neg = true
                '+' -> {}
            }
            while (r.hasRemaining()) {
                i = r.get().toInt().toChar()
                when (i) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> x = x * 10.toInt() + i.code - '0'.code.toLong()
//                    '-' -> neg = true
//                    '+' -> {}
                }
            }
            res = (if (neg) -x  else x ).toInt()
        }
        return res
    }

    fun parseInt(r: String): Int? {
        var x: Long = 0
        var neg = false
        var res: Int? = null
        val length = r.length
        if (0 < length) {
            var i = r[0].code.toChar()
            when (i) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> x = x * 10.toInt() + i.code - '0'.code.toLong()
                '-' -> neg = true
                '+' -> {}
            }
            for (j in 1 until length) {
                i = r[j].code.toChar()
                when (i) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> x = x * 10.toInt() + i.code - '0'.code.toLong()
//                    '-' -> neg = true
//                    '+' -> {}
                }
            }
            res = (if (neg) -x  else x).toInt()
        }
        return res
    }
}