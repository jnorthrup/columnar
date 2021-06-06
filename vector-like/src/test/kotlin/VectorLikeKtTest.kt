package cursors

import org.junit.*
import org.junit.Assert.*
import vec.macros.*
import vec.util._a
import vec.util._l

class VectorLikeKtTest {
    @Test
    fun testCombineVec() {
        val c: Vect0r<Int> = combine(
            (0..2).toVect0r(),
            (3..5).toVect0r(),
            (6..9).toVect0r()
        )
        val toList = c.toList()
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]", toList.toString())
        System.err.println(toList)
    }

    @Test
    fun testDiv() {
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
        assertEquals(toList1, toList)
    }

    infix fun Any?.shouldBe(that: Any?) = assertEquals(this, that)

    @Test
    fun `WhatCanVect0rDo`() {

        // we can create an array from bikeshed util
        val ar: Array<String> = _a["0", "1", "2"]

        //we can make a lazy vector from the array of strings
        val strVec = ar.toVect0r()

        // ## "convertables"
        // we have conversion code for sequences, flows, arrays, and List in both directions.
        val convertedToSequence = strVec.toSequence()
        val convertedToFlow = strVec.toFlow()
        val convertedToArray = strVec.toArray()
        val convertedToList = strVec.toList()

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
        val reordered1 = doubleLength[2, 1, 3, 4, 4, 4, 4, 4]
        System.err.println(reordered1.toList())
        System.err.println()

        //#### zip
        // same as the stdlib zip except the Vect0r result uses an interface instead of a data class
        // the pairwise construction uses the simplest possible `pair` interface named `Pai2`
        val zip: Vect02<Int, Int> = reordered1.zip(reordered1)

        // these are pure functional interfaces without toString, but have a pair conversion
        val z1 = zip α Pai2<Int, Int>::pair
        System.err.println("z1: zip" + z1.toList())

        //#### Vect0r.zipWithNext()
        //also similar to stdlib with a modified
        val zwn = combine(strVec, strVec).zipWithNext()
        val z2 = zwn α Pai2<String, String>::pair
        System.err.println("z2: zwn " + z2.toList())

        System.err.println()

        //## isomorphic pair interfaces
        //        we use a simple inheritable Pair interface called Pai2
        val theInts = 0 t2 1
        val theIntPai = theInts.pair
        System.err.println("theIntPair:" + theIntPai)

        //### union type casting
        // we get a free retype operation by having typalias of pair work across multiple sets of
        // extension functions... for free
        val theTw1n = theInts
        System.err.println(" theTw1n:" + theTw1n.pair)

        // we can still specialize based on type semantics in typealiases.  powerful language to say the least.
        val theTw1n2: Tw1nt = Tw1n<Int>(0, 1)
        System.err.println(" theTw1n:" +/* */_l[theTw1n2.toString()])


//        ### twins
        //as suggested there is a Twin in gscollections now called Eclipse-Collecitons
        //we name this Tw1n

        val theTwinAny: Tw1n<*> = Tw1n("0", 1)

        //specialized overloads exist for primitive types

        val theTwinInt: Tw1nt = Tw1n<Int>(0, 1)
        val theTwInt: Tw1nt = Tw1nt(_a[0, 1])


//        #### isomorphisms:   Vect0r<Pai2<T,T>>,Vect0r<Tw1n<T>>, Vect02<T>

//        these are all interchangable casts of the same Pai2:

        val pai2: Pai2<Int, (Int) -> Pai2<Int, Int>> = reordered1.zip(reordered1)
        val vect02: Vect02<Int, Int> = reordered1.zip(reordered1)
        val vectTwin: Vect0r<Tw1n<Int>> = reordered1.zip(reordered1)
        val vectTwint: Vect0r<Tw1nt> = reordered1.zip(reordered1) as Vect0r<Tw1nt>

    }

}
