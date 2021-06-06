package cursors.ml

import junit.framework.Assert.assertEquals
import org.junit.Test
import vec.macros.Tw1n

class FeatureRangeKtTest {

    @Test
    fun normalize() {

        (Tw1n(10.0, 100.0).normalize(40.0))
        assertEquals(0.0, (Tw1n(10.0, 100.0).normalize(10.0)))
        run {
            val normalize = Tw1n(10.0, 100.0).normalize(100.0)
            assertEquals(1.0, normalize)
        }
        run {
            val deNormalize = Tw1n(10.0, 100.0).deNormalize(1.0)
            assertEquals(100.0, deNormalize)
        }
        run {
            val deNormalize = Tw1n(10.0, 100.0).deNormalize(0.0)
            assertEquals(10.0, deNormalize)
        }


    }
}