package cursors

import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vec.macros.*
import vec.util._a

class VectorLikeKtTest {
    @Test
    fun combinevec() {
        val c: Vect0r<Int> = combine(
                (0..2).toVect0r(),
                (3..5).toVect0r(),
                (6..9).toVect0r()
        )
        val toList = c.toList()
        Assertions.assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]", toList.toString())
        System.err.println(toList)
    }

    @Test
    fun div() {
        val intRange = 0..11
        fun IntRange.split(nSubRanges: Int) = run {
            val subSize = (last - first + (1 - first)) / nSubRanges
            sequence {
                for (i in this@split step subSize) yield(i..minOf(last, i + subSize - 1))
            }
        }
        System.err.println(intRange.toList())
        System.err.println(intRange.last)
        val toList = intRange.split(3).toList()
        val toList1 = (intRange / 3).toList()
        System.err.println(toList to toList1)
        Assertions.assertEquals(toList1, toList)
    }

    infix fun Any?.shouldBe(that: Any?) = Assert.assertEquals(this, that)

    @Test
    fun `WhatCanVect0rDo`() {

        // we can create an array from bikeshed util
        val ar: Array<String> = _a["0", "1", "2"]

        //we can make a lazy vector from the array of strings
        val strVec  = ar.toVect0r()

        // ## "convertables"
        // we have conversion code for sequences, flows, arrays, and List in both directions.
        val convertedToSequence =strVec.toSequence()
        val convertedToFlow =strVec.toFlow()
        val convertedToArray =strVec.toArray()
        val convertedToList =strVec.toList()

        //we can make a lazy vector of ints from the array of strings.
        val intVec = strVec α String::toInt

        intVec.last() shouldBe 2

        //we can combine them to create new indexed vet0r's
        val doubleLength = combine(intVec, intVec)
        doubleLength.size shouldBe 6


        // we  can print them.  there is no toString so we get an identity from an inline class
        System.err.println("double length vector is cold: " + doubleLength)
        System.err.println()

        // we can reify them and then print that.
        System.err.println("double length vector is reified: " + doubleLength.toList())
        System.err.println()

        //we can destructure them to reach under the hood
        val (a: Int, b: (Int) -> Int) = doubleLength
        (a === doubleLength.size) shouldBe (doubleLength.size === doubleLength.first)

        //we can reorder them as a new Vect0r,  lazily.  the bikeshed  util extends this to Any convertables above
        val reordered1 = doubleLength[2, 1, 3, 4,4,4,4,4]
        System.err.println(reordered1.toList())


    }

}
