package cursors.hash

import org.bouncycastle.util.encoders.Hex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import vec.util._a

class MD4Test {
    @Test
    fun testxlate() {
        assertEquals( _a[0xa.toByte()].hex,"0a")
        assertEquals( _a[0x1.toByte()].hex,"01")
    }

    @Test
    fun testest() {

        val md4 = "test".md4
        assertEquals("db346d691d7acc4dc2625db19f9e3f52", md4)


    }
    @Test
    fun test1() {

        val md4 = "1".md4
        assertEquals("8be1ec697b14ad3a53b371436120641d", md4)

    }
}