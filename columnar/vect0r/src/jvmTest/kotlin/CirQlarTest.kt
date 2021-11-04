package cursors


import kotlinx.coroutines.runBlocking
import kotlin.test.*
import vec.CirQlar
import vec.macros.f1rst
import vec.macros.last
import vec.macros.toList

class CirQlarTest {
    @Test
    fun testCirQtoList() = runBlocking {

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
    fun testtoVect0r() = runBlocking {
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