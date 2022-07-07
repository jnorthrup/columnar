package bbcursive.lib

import bbcursive.lib.allOf_.Companion.allOf
import bbcursive.lib.chlit_.chlit
import bbcursive.lib.opt_.opt
import bbcursive.std.bb
import junit.framework.Assert
import junit.framework.TestCase
import org.junit.Test

/**
 * Created by jim on 1/24/16.
 */
class opt_Test {
    @Test
    fun testChlit() {
        var aa = bb("aba", chlit('a'), opt(chlit('a')), opt(chlit('c')), opt(chlit('b')), chlit('a'))
        Assert.assertNotNull(aa)
        aa = bb("aba", allOf(chlit('a'), opt(chlit('a')), opt(chlit('c')), opt(chlit('b')), chlit('z')))
        TestCase.assertNull(aa)
        aa =
            bb("aba", allOf(chlit('a'), opt(chlit('a')), opt(chlit('b')), opt(chlit('z')), chlit('a'), opt(chlit('a'))))
        Assert.assertNotNull(aa)
    }
}