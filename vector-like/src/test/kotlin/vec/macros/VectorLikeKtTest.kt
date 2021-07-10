package vec.macros

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import vec.util._l
import vec.util._v

class VectorLikeKtTest {

    @Test
    fun reverse() {
        assertArrayEquals(_v[0, 1, 2, 3, 4, 5, 6].reverse.also { System.err.println(it.toList()) }.toList()
            .toIntArray(), _l[0, 1, 2, 3, 4, 5, 6].reversed().toIntArray())


    }

    @Test
    fun testTranspose() {
        val tme = _v[
                _v[1, 2, 3, 4],   //4, 5,100],
                _v[6, 7, 8, 9],   //9, 10,101],
                _v[11, 12, 13, 14],  //14, 15,102],
        ]
        val goal = _v[
                _v[1, 6, 11],
                _v[2, 7, 12],
                _v[3, 8, 13],
                _v[4, 9, 14],
        ]
        val tist = tme.T
        (tme α Vect0r<Int>::toList).forEach { list: List<Int> ->
            System.err.println(list.toString())
        }
        System.err.println("----")
        (goal α Vect0r<Int>::toList).forEach { list: List<Int> ->
            System.err.println(list.toString())
        }
        System.err.println("----")
        (tist α Vect0r<Int>::toList).forEach { list: List<Int> ->
            System.err.println(list.toString())
        }
        assertEquals(goal[1][0], tist[0][1])
        assertEquals(goal[2][1], tist[1][2])
        assertEquals(goal[3][2], tist[2][3])
        assertEquals(goal[0][2], tist[2][0])

    }
}