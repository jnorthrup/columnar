package simple.simple

object CZero {
// @formatter:off
/** nonzero test */ val Byte.nz get() = 0 != this.toInt()
/** nonzero test */ val Short.nz get() = 0 != this.toInt()
/** nonzero test */ val Char.nz get() = 0  != this.code
/** nonzero test */ val Int.nz get() = 0 != this
/** nonzero test */ val Long.nz get() = 0L != this
/** nonzero test */ val UByte.nz get() = 0 != this.toInt()
/** nonzero test */ val UShort.nz get() = 0 != this.toInt()
/** nonzero test */ val UInt.nz get() = 0U != this
/** nonzero test */ val ULong.nz get() = 0UL != this
/**zerotest*/val Byte.z get() =0==this.toInt()
/**zerotest*/val Short.z get() =0==this.toInt()
/**zerotest*/val Char.z get() =0== this.code
/**zerotest*/val Int.z get() =0==this
/**zerotest*/val Long.z get() =0L==this
/**zerotest*/val UByte.z get() =0==this.toInt()
/**zerotest*/val UShort.z get() =0==this.toInt()
/**zerotest*/val UInt.z get() =0U==this
/**zerotest*/val ULong.z get() =0UL==this
}