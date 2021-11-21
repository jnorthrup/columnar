package vec.ml

import kotlin.test.*
import vec.util._l


infix fun Any?.shouldBe(that: Any?) {
 assertEquals(that, this)
}

class FeatureRangeKtTest {
    @Test
    fun test0() {
        val seq = _l[0] as Iterable<Int>
        val featureRange = featureRange(seq)
        System.err.println(featureRange)
    }
}