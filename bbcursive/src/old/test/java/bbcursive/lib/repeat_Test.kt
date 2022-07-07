package bbcursive.lib

import bbcursive.lib.chlit_.chlit
import bbcursive.lib.repeat_.repeat
import bbcursive.std
import bbcursive.std.bb
import org.junit.Assert
import org.junit.Test

/**
 * Created by jim on 1/23/16.
 */
class repeat_Test {
    @Test
    fun testRepeat() {
        var aaa = bb("aaa", repeat(chlit('a')))
        Assert.assertNotNull(aaa)
        aaa = bb("aba", repeat(chlit('a')))
        Assert.assertNotNull(aaa)
        aaa = bb("baa", repeat(chlit('a')))
        Assert.assertNull(aaa)
        std.flags.apply {  (get()+(std.traits.skipper)).also (::set)}
        aaa = bb("a a a", repeat(chlit('a')))
        Assert.assertNotNull(aaa)
        std.flags .apply{  (get()-(std.traits.skipper)).also(::set) }
        aaa = bb(" a a a", repeat(chlit('a')))
        Assert.assertNull(aaa)
    }
}