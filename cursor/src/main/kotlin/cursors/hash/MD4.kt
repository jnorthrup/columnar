package cursors.hash

import org.bouncycastle.crypto.digests.MD4Digest


val Any?.md4a :ByteArray get() {
        val s = toString()
        val ba = s.toByteArray()
        val d = MD4Digest()
        d.update(ba, 0, ba.size)
        val o = ByteArray(d.getDigestSize())
        d.doFinal(o, 0)
        return o
    }

val Any?.md4 get() =   this.md4a.hex
fun xlate(v: Int) = if (v < 0xa) '0' + v
else 'a' + (v - 0xa)

val ByteArray.hex: String
    get() {
        val res = CharArray(size shl 1)
        for (ix in 0 until size) get(ix).toInt().also {
            var os = ix shl 1
            res[os++] = xlate(it.toInt() shr 4 and 0xf)
            res[os] = xlate(it.toInt() and 0xf)
        }
        return String(res)
    }


/*
object RosettaMD4 {
    @Throws(Exception::class)
    @JvmStatic
    fun main(argv: Array<String>) {
        val r = "test".toByteArray(charset("US-ASCII"))
        val d = MD4Digest()
        d.update(r, 0, r.size)
        val o = ByteArray(d.digestSize)
        d.doFinal(o, 0)

        Hex.encode(o, System.out)
        println()
        println(o.hex)
    }
}*/
