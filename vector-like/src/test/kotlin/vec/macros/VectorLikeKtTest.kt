package vec.macros

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import vec.util._l
import vec.util._v

class VectorLikeKtTest {

    @Test
    fun reverse() {
        assertArrayEquals(_v[0,1,2,3,4,5,6].reverse.also { System.err.println(it.toList()) }.toList().toIntArray(),_l[0,1,2,3,4,5,6].reversed().toIntArray())


    }
}