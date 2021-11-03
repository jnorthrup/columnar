package ports

import kotlin.math.min

class ByteBuffer(
    val array: ByteArray,
    /**offset*/
    private val start: Int = 0,

    /**capacity*/
    private val cap: Int = array.size - start,
    /**limit*/
    private var lim: Int = cap,
) {
    fun slice() = ByteBuffer(array, pos + start, lim = rem).also { it.byteOrder = byteOrder }
    fun limit() = lim
    fun limit(x: Int) = apply {
        require(x < capacity() && x > position())
        lim = x
    }

    fun capacity() = cap

    /**remaining*/
    val rem get() = lim - pos

    fun remaining() = rem
    fun hasRemaining() = rem > 0

    /**mark*/
    private var mk = -1
    fun mark() = apply { mk = pos }
    fun reset() = apply {
        pos = mk
        mk = -1
    }

    fun rewind() = apply { pos = 0 }


    /**position*/
    var pos = 0
    fun position() = pos
    fun position(x: Int) = apply {
        require(x < limit())
        pos = x
    }

    fun clear(): ByteBuffer = apply {
        pos = 0
        mk = -1
        lim = cap
    }

    private var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    fun order() = byteOrder
    fun order(byteOrder: ByteOrder): ByteBuffer = apply { this.byteOrder = byteOrder }

    //// advancing position
    private fun <T> advance(block: (Int) -> T) = block(pos + start).also { pos += 1 }

    val next get() = advance { array[it] }
    fun get(): Byte {
        require(rem > 0);return next
    }

    val peek get() = array[pos + start]


    fun put(toByte: Byte) = apply {
        require(rem > 0)
        advance { array[it] = toByte }
    }

    /////---     indexed

    operator fun get(x: Int): Byte {
        require(x < lim)
        return array[x + start]
    }

    fun put(x: Int, d: Byte) = apply { this[x] = d }
    operator fun set(x: Int, d: Byte) {
        require(x < lim)
        array[x + start] = d
    }

    ///////////////conversion

    fun getShort() = let {
        require(remaining() >= Short.SIZE_BYTES)
        shortIndices.fold(0) { acc, x -> acc or (next.toInt() shl (x shl 3)) }.toShort()
    }

    fun getInt() = let {
        require(remaining() >= Int.SIZE_BYTES)
        intIndices.fold(0) { acc, x -> acc or (next.toInt() shl (x shl 3)) }.toInt()
    }

    fun getLong() = let {
        require(remaining() >= Long.SIZE_BYTES)
        longIndices.fold(0L) { acc, x -> acc or next.toLong() shl (x shl 3) }.toLong()
    }

    fun putShort(data: Short) = apply {
        val sz = Short.SIZE_BYTES
        val indices1 = shortIndices
        require(remaining() >= sz)//shortIndices
        val d1 = data.toUInt()
        val iit = indices1.iterator()
        val aa = ByteArray(sz) {
            d1.shr(iit.nextInt() shl 3).toByte()
        }
        put(aa)
    }

    fun putInt(data: Int) = apply {
        val sz = Int.SIZE_BYTES
        val indices1 = intIndices
        require(remaining() >= sz)//intIndices
        val d1 = data.toUInt()
        val iit = indices1.iterator()
        val aa = ByteArray(sz) {
            d1.shr(iit.nextInt() shl 3).toByte()
        }
        put(aa)
    }

    fun putLong(data: Long) = apply {
        val sz = Long.SIZE_BYTES
        val indices1 = longIndices
        require(remaining() >= sz)//longIndices
        val d1 = data.toULong()
        val iit = indices1.iterator()
        val aa = ByteArray(sz) {
            d1.shr(iit.nextInt() shl 3).toByte()
        }
        put(aa)
    }

    fun getFloat() = Float.fromBits(getInt())
    fun getDouble() = Double.fromBits(getLong())
    fun putFloat(b: Float) = apply { putInt(b.toXBits()) }
    fun putDouble(b: Double) = apply { putLong(b.toXBits()) }

    ///////////// bulk
    operator fun get(bytesInto: ByteArray) = arCp(bytesInto)

    fun get(other: ByteBuffer) = apply { bufCp(other) }
    fun put(other: ByteBuffer) = other.apply { bufCp(this) }

    private fun bufCp(bytesFrom: ByteBuffer) {
        val startIndex = bytesFrom.start + bytesFrom.pos
        val destinationOffset = start + pos
        val cap0 = bytesFrom.rem
        val cap1 = rem
        val span = min(cap0, cap1)
        bytesFrom.array.copyInto(this.array, destinationOffset, startIndex, span)
        bytesFrom.pos += span
        pos += span
    }

    private fun arCp(bytesInto: ByteArray) {
        val span = min(rem, bytesInto.size)
        array.copyInto(bytesInto, 0, pos + start, span)
        pos += span
    }

    fun put(bytesFrom: ByteArray) = apply {
        val span = min(rem, bytesFrom.size)
        bytesFrom.copyInto(array, pos + start, 0, span)
        pos += span
    }

    fun flip() = apply {
        lim = pos
        pos = 0
        mk = -1
    }


    /** these indices are 0..{2,4,8} and can be data.shl(x.shl(3)) which iiuc is no consequence for modern cpu.*/
    private val shortIndices get() = if (byteOrder == ByteOrder.BIG_ENDIAN) bIndShort else sIndShort
    private val intIndices get() = if (byteOrder == ByteOrder.BIG_ENDIAN) bIndInt else sIndInt
    private val longIndices get() = if (byteOrder == ByteOrder.BIG_ENDIAN) bIndLong else sIndLong

    companion object {
        val sIndShort   = 0 until Short.SIZE_BYTES
        val sIndInt   = 0 until Int.SIZE_BYTES
        val sIndLong   = 0 until Long.SIZE_BYTES
        val bIndShort   = sIndShort.reversed()
        val bIndInt   = sIndInt.reversed()
        val bIndLong   = sIndLong.reversed()
        fun wrap(bytes: ByteArray): ByteBuffer = ByteBuffer(bytes)
        fun allocate(sz: Int) = wrap(ByteArray(sz))
        fun allocateDirect(i: Int) = allocate(i)
    }
}
