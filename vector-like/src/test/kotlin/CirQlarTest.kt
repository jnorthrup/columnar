package cursors

import junit.framework.TestCase
import org.junit.Test
import vec.macros.f1rst
import vec.macros.last
import vec.macros.toList
import vec.CirQlar

class CirQlarTest : TestCase() {

    @Test
    fun testCirQtoList() {

        val cirQ = CirQlar<Int>(3)
        (-1 until 7).forEach {
            System.err.println(cirQ.toList())
            cirQ.offer(it)
        }
        val list = cirQ.toList()
        System.err.println(list)
        assertEquals(6, list.last())
        assertEquals(4, list.first())

    }

    @Test
    fun testtoVect0r() {
        val cirQ = CirQlar<Int>(3)
        (-1 until 7).forEach {
            System.err.println(cirQ.toVect0r().toList())
            cirQ.offer(it)
        }
        val vec = cirQ.toVect0r()
        System.err.println(vec.toList())
        assertEquals(6, vec.last)
        assertEquals(4, vec.f1rst)
    }


}