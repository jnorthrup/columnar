package cursors.tbd

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

typealias IntIntPair = Pair<Int, Int>

/** TODO more exhasutive tests  */
internal class GilbertCurveTest {
    @Test
    fun testSizes() {
        for (w in 1..8) for (h in 1..8) testGilbert(w, h)
    }

    companion object {
        private fun testGilbert(w: Int, h: Int) {

            val pointsOrdered = mutableListOf<IntIntPair>()
            gilbertCurve(w, h) { x, y -> pointsOrdered += (x to y) }

            assertEquals(w * h, pointsOrdered.distinct().size/*, "expected # of unique points"*/)

            //test that each sequential point has manhattan distance <= 2 from previous
            for (i in 0 until pointsOrdered.size - 1) {
                val a: IntIntPair = pointsOrdered[i]
                val b: IntIntPair = pointsOrdered[i + 1]
                assertTrue(Math.abs(a.first - b.first) + Math.abs(a.second - b.second) <= 2)
            }
        }
    }
}
