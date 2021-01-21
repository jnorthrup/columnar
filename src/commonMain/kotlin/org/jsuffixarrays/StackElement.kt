package org.jsuffixarrays
/*
    *
    */
data class StackElement(val a: Int, val b: Int, val c: Int, var d: Int, val e: Int = 0) {
    companion object {
        val SENIL = StackElement(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
    }
}/*
    *
    */