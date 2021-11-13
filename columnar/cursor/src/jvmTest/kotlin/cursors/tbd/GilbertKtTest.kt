package cursors.tbd

import kotlin.math.abs
import kotlin.test.*

typealias IntIntPair = Pair<Int, Int>

/** TODO more exhaustive tests  */
internal class GilbertCurveTest {
    @Test
    fun testSizes() {
        for (w in 1..8) for (h in 1..8) testGilbert(w, h)
    }

    companion object {
        private fun testGilbert(w: Int, h: Int) {

            val pointsOrdered = mutableListOf<IntIntPair>()
            gilbertCurve(w, h, fun(x: Int, y: Int) {
                pointsOrdered += (x to y)
            })

            assertEquals(w * h, pointsOrdered.distinct().size/*, "expected # of unique points"*/)

            //test that each sequential point has manhattan distance <= 2 from previous
            for (i in 0 until pointsOrdered.size - 1) {
                val a: IntIntPair = pointsOrdered[i]
                val b: IntIntPair = pointsOrdered[i + 1]
                assertTrue(abs(a.first - b.first) + abs(a.second - b.second) <= 2)
            }
        }
    }
}
